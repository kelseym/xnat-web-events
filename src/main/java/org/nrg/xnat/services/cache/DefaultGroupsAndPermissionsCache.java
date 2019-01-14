/*
 * web: org.nrg.xnat.services.cache.DefaultUserProjectCache
 * XNAT http://www.xnat.org
 * Copyright (c) 2017, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.services.cache;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.framework.orm.DatabaseHelper;
import org.nrg.framework.utilities.LapStopWatch;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.display.ElementDisplay;
import org.nrg.xdat.om.*;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xdat.security.*;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.services.Initializing;
import org.nrg.xdat.services.cache.GroupsAndPermissionsCache;
import org.nrg.xdat.servlet.XDATServlet;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.event.XftItemEventI;
import org.nrg.xft.event.methods.XftItemEventCriteria;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.ItemNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.XFTManager;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.services.cache.jms.InitializeGroupRequest;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.nrg.framework.exceptions.NrgServiceError.ConfigurationError;
import static org.nrg.xapi.rest.users.DataAccessApi.*;
import static org.nrg.xdat.security.PermissionCriteria.dumpCriteriaList;
import static org.nrg.xdat.security.helpers.Groups.*;
import static org.nrg.xft.event.XftItemEventI.*;

@SuppressWarnings("Duplicates")
@Service
@Slf4j
public class DefaultGroupsAndPermissionsCache extends AbstractXftItemAndCacheEventHandlerMethod implements GroupsAndPermissionsCache, Initializing, GroupsAndPermissionsCache.Provider {

    @Autowired
    public DefaultGroupsAndPermissionsCache(final CacheManager cacheManager, final NamedParameterJdbcTemplate template, final JmsTemplate jmsTemplate) throws SQLException {
        super(cacheManager,
              XftItemEventCriteria.builder().xsiType(XnatProjectdata.SCHEMA_ELEMENT_NAME).actions(CREATE, UPDATE, DELETE).build(),
              XftItemEventCriteria.builder().xsiType(XnatSubjectdata.SCHEMA_ELEMENT_NAME).xsiType(XnatExperimentdata.SCHEMA_ELEMENT_NAME).actions(CREATE, XftItemEventI.DELETE, SHARE).build(),
              XftItemEventCriteria.getXsiTypeCriteria(XdatUsergroup.SCHEMA_ELEMENT_NAME),
              XftItemEventCriteria.getXsiTypeCriteria(XdatElementSecurity.SCHEMA_ELEMENT_NAME));

        _template = template;
        _jmsTemplate = jmsTemplate;
        _helper = new DatabaseHelper((JdbcTemplate) _template.getJdbcOperations());
        _totalCounts = new HashMap<>();
        _missingElements = new HashMap<>();
        _userChecks = new ConcurrentHashMap<>();
        _initialized = new AtomicBoolean(false);
        if (_helper.tableExists("xnat_projectdata")) {
            resetTotalCounts();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyElementRemoved(final Ehcache cache, final Element element) throws CacheException {
        handleCacheRemoveEvent(cache, element, "removed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyElementExpired(final Ehcache cache, final Element element) {
        handleCacheRemoveEvent(cache, element, "expired");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyElementEvicted(final Ehcache cache, final Element element) {
        handleCacheRemoveEvent(cache, element, "evicted");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyRemoveAll(final Ehcache cache) {
        handleCacheRemoveEvent(cache, null, "removed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    /**
     * Gets the specified project if the user has any access to it. Returns null otherwise.
     *
     * @param groupId The ID or alias of the project to retrieve.
     *
     * @return The project object if the user can access it, null otherwise.
     */
    @Override
    public UserGroupI get(final String groupId) {
        if (StringUtils.isBlank(groupId)) {
            throw new IllegalArgumentException("Can not request a group with a blank group ID");
        }

        // Check that the group is cached and, if so, return it.
        log.trace("Retrieving group through cache ID {}", groupId);
        final UserGroupI cachedGroup = getCachedGroup(groupId);
        if (cachedGroup != null) {
            log.debug("Found cached group entry for cache ID '{}'", groupId);
            return cachedGroup;
        }

        try {
            log.trace("Initializing group entry for cache ID '{}'", groupId);
            return initGroup(groupId);
        } catch (ItemNotFoundException e) {
            log.info("Can't find a group with the group ID '{}', returning null", groupId);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Long> getReadableCounts(final UserI user) {
        if (user == null) {
            return Collections.emptyMap();
        }

        final String username = user.getUsername();
        final String cacheId  = getCacheIdForUserElements(username, READABLE);

        // Check whether the element types are cached and, if so, return that.
        log.trace("Retrieving readable counts for user {} through cache ID {}", username, cacheId);
        final Map<String, Long> cachedReadableCounts = getCachedMap(cacheId);
        if (cachedReadableCounts != null) {
            log.debug("Found cached readable counts entry for user '{}' with cache ID '{}' containing {} entries", username, cacheId, cachedReadableCounts.size());
            return cachedReadableCounts;
        }

        return initReadableCountsForUser(cacheId, user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ElementDisplay> getBrowseableElementDisplays(final UserI user) {
        if (user == null) {
            return Collections.emptyMap();
        }

        final Map<String, ElementDisplay> guestBrowseableElementDisplays = getGuestBrowseableElementDisplays();
        if (user.isGuest()) {
            log.debug("Got a request for browseable element displays for the guest user, returning {} entries", guestBrowseableElementDisplays.size());
            return guestBrowseableElementDisplays;
        }

        final String username = user.getUsername();
        final String cacheId  = getCacheIdForUserElements(username, BROWSEABLE);

        // Check whether the element types are cached and, if so, return that.
        log.trace("Retrieving browseable element displays for user {} through cache ID {}", username, cacheId);
        final Map<String, ElementDisplay> cachedUserEntry = getCachedMap(cacheId);
        if (cachedUserEntry != null) {
            @SuppressWarnings("unchecked") final Map<String, ElementDisplay> browseables = buildImmutableMap(cachedUserEntry, guestBrowseableElementDisplays);
            log.debug("Found a cached entry for user '{}' browseable element displays under cache ID '{}' with {} entries", username, cacheId, browseables.size());
            return browseables;
        }

        log.trace("Initializing browseable element displays for user '{}' with cache ID '{}'", username, cacheId);
        @SuppressWarnings("unchecked") final Map<String, ElementDisplay> browseables = buildImmutableMap(initBrowseableElementDisplaysForUser(cacheId, user), guestBrowseableElementDisplays);
        log.debug("Initialized browseable element displays for user '{}' with cache ID '{}' with {} entries (including guest browseable element displays)", username, cacheId, browseables.size());
        return browseables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ElementDisplay> getSearchableElementDisplays(final UserI user) {
        if (user == null) {
            return Collections.emptyList();
        }

        final String username = user.getUsername();
        final String cacheId  = getCacheIdForUserElements(username, SEARCHABLE);
        log.debug("Retrieving searchable element displays for user {} through cache ID {}", username, cacheId);

        final Map<String, Long> counts = getReadableCounts(user);
        try {
            return Lists.newArrayList(Iterables.filter(getActionElementDisplays(username, SecurityManager.READ), new Predicate<ElementDisplay>() {
                @Override
                public boolean apply(@Nullable final ElementDisplay elementDisplay) {
                    if (elementDisplay == null) {
                        return false;
                    }
                    final String elementName = elementDisplay.getElementName();
                    try {
                        return ElementSecurity.IsSearchable(elementName) && counts.containsKey(elementName) && counts.get(elementName) > 0;
                    } catch (Exception e) {
                        return false;
                    }
                }
            }));
        } catch (Exception e) {
            log.error("An unknown error occurred", e);
        }

        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ElementDisplay> getActionElementDisplays(final UserI user, final String action) {
        return getActionElementDisplays(user.getUsername(), action);
    }

    @Override
    public List<ElementDisplay> getActionElementDisplays(final String username, final String action) {
        if (!ACTIONS.contains(action)) {
            throw new NrgServiceRuntimeException(ConfigurationError, "The action '" + action + "' is invalid, must be one of: " + StringUtils.join(ACTIONS, ", "));
        }
        final List<ElementDisplay> elementDisplays = getActionElementDisplays(username).get(action);
        if (log.isTraceEnabled()) {
            log.trace("Found {} element displays for user {} action {}: {}", elementDisplays.size(), username, action, formatElementDisplays(elementDisplays));
        }
        return elementDisplays;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PermissionCriteriaI> getPermissionCriteria(final UserI user, final String dataType) {
        return getPermissionCriteria(user.getUsername(), dataType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PermissionCriteriaI> getPermissionCriteria(final String username, final String dataType) {
        try {
            PoolDBUtils.CheckSpecialSQLChars(dataType);
        } catch (Exception e) {
            return null;
        }

        try {
            final List<PermissionCriteriaI> criteria = new ArrayList<>();

            final Map<String, ElementAccessManager> managers = getElementAccessManagers(username);
            if (managers.isEmpty()) {
                log.info("Couldn't find element access managers for user {} trying to retrieve permissions for data type {}", username, dataType);
            } else {
                final ElementAccessManager manager = managers.get(dataType);
                if (manager == null) {
                    log.info("Couldn't find element access manager for data type {} for user {} while trying to retrieve permissions ", dataType, username);
                } else {
                    criteria.addAll(manager.getCriteria());
                    if (criteria.isEmpty()) {
                        log.debug("Couldn't find any permission criteria for data type {} for user {} while trying to retrieve permissions ", dataType, username);
                    }
                }
            }

            final Map<String, UserGroupI> userGroups = getMutableGroupsForUser(username);
            if (log.isDebugEnabled()) {
                log.debug("Found {} user groups for the user {}", userGroups.size(), username, userGroups.isEmpty() ? "" : ": " + Joiner.on(", ").join(userGroups.keySet()));
            }
            final Set<String> groups = userGroups.keySet();
            if (CollectionUtils.containsAny(groups, ALL_DATA_GROUPS)) {
                if (groups.contains(ALL_DATA_ADMIN_GROUP)) {
                    _template.query(QUERY_GET_ALL_MEMBER_GROUPS, new RowCallbackHandler() {
                        @Override
                        public void processRow(final ResultSet resultSet) throws SQLException {
                            final String projectId = resultSet.getString("project_id");
                            final String groupId   = resultSet.getString("group_id");

                            // If the user is a collaborator on a project, we're going to upgrade them to member,
                            // so remove that collaborator nonsense, this is the big time.
                            userGroups.remove(projectId + "_collaborator");

                            // If the user is already a member of owner of a project, then don't bother: they already have
                            // sufficient access to the project.
                            if (!userGroups.containsKey(projectId + "_owner") && !userGroups.containsKey(projectId + "_member")) {
                                userGroups.put(groupId, get(groupId));
                            }
                        }
                    });
                } else if (userGroups.containsKey(ALL_DATA_ACCESS_GROUP)) {
                    _template.query(QUERY_GET_ALL_COLLAB_GROUPS, new RowCallbackHandler() {
                        @Override
                        public void processRow(final ResultSet resultSet) throws SQLException {
                            final String projectId = resultSet.getString("project_id");
                            final String groupId   = resultSet.getString("group_id");

                            // If the user has no group membership, then add as a collaborator.
                            if (!CollectionUtils.containsAny(groups, Arrays.asList(groupId, projectId + "_member", projectId + "_owner"))) {
                                userGroups.put(groupId, get(groupId));
                            }
                        }
                    });
                }
            }

            for (final UserGroupI group : userGroups.values()) {
                final List<PermissionCriteriaI> permissions = group.getPermissionsByDataType(dataType);
                if (permissions != null) {
                    if (log.isTraceEnabled()) {
                        log.trace("Searched for permission criteria for user {} on type {} in group {}: {}", username, dataType, group.getId(), dumpCriteriaList(permissions));
                    } else {
                        log.debug("Searched for permission criteria for user {} on type {} in group {}: {} permissions found", username, dataType, group.getId(), permissions.size());
                    }
                    criteria.addAll(permissions);
                } else {
                    log.warn("Tried to retrieve permissions for data type {} for user {} in group {}, but this returned null.", dataType, username, group.getId());
                }
            }

            if (!isGuest(username)) {
                try {
                    final List<PermissionCriteriaI> permissions = getPermissionCriteria(getGuest().getUsername(), dataType);
                    if (permissions != null) {
                        criteria.addAll(permissions);
                    } else {
                        log.warn("Tried to retrieve permissions for data type {} for the guest user, but this returned null.", dataType);
                    }
                } catch (Exception e) {
                    log.error("An error occurred trying to retrieve the guest user", e);
                }
            }

            if (log.isTraceEnabled()) {
                log.trace("Retrieved permission criteria for user {} on the data type {}: {}", username, dataType, dumpCriteriaList(criteria));
            } else {
                log.debug("Retrieved permission criteria for user {} on the data type {}: {} criteria found", username, dataType, criteria.size());
            }

            return ImmutableList.copyOf(criteria);
        } catch (UserNotFoundException e) {
            log.error("Couldn't find the indicated user");
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Long> getTotalCounts() {
        if (_totalCounts.isEmpty()) {
            resetTotalCounts();
        }

        return ImmutableMap.copyOf(_totalCounts);
    }

    @Nonnull
    @Override
    public List<String> getProjectsForUser(final String username, final String access) {
        log.info("Getting projects with {} access for user {}", access, username);
        switch (access) {
            case SecurityManager.READ:
                return getUserReadableProjects(username);

            case SecurityManager.EDIT:
                return getUserEditableProjects(username);

            case SecurityManager.DELETE:
                return getUserOwnedProjects(username);

            default:
                throw new IllegalArgumentException("Invalid access level '" + access + "', valid values are: '" + SecurityManager.READ + "', '" + SecurityManager.EDIT + "', and '" + SecurityManager.DELETE + "'.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public List<UserGroupI> getGroupsForTag(final String tag) {
        // Get the group IDs associated with the tag.
        log.info("Getting groups for tag {}", tag);
        final List<String> groupIds = getTagGroups(tag);
        return getUserGroupList(groupIds);
    }

    @Nonnull
    @Override
    public Map<String, UserGroupI> getGroupsForUser(final String username) throws UserNotFoundException {
        return ImmutableMap.copyOf(getMutableGroupsForUser(username));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshGroupsForUser(final String username) throws UserNotFoundException {
        initUserGroupIds(getCacheIdForUserGroups(username), username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public UserGroupI getGroupForUserAndTag(final String username, final String tag) throws UserNotFoundException {
        final String groupId = _template.query(QUERY_GET_GROUP_FOR_USER_AND_TAG, checkUser(username).addValue("tag", tag), new ResultSetExtractor<String>() {
            @Override
            public String extractData(final ResultSet results) throws DataAccessException, SQLException {
                return results.next() ? results.getString("id") : null;
            }
        });
        return StringUtils.isNotBlank(groupId) ? get(groupId) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getUserIdsForGroup(final String groupId) {
        final UserGroupI userGroup = get(groupId);
        if (userGroup == null) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(userGroup.getUsernames());
    }

    @Override
    public void refreshGroup(final String groupId) throws ItemNotFoundException {
        final UserGroupI group = initGroup(groupId);
        for (final String username : group.getUsernames()) {
            try {
                if (!getGroupIdsForUser(username).contains(groupId)) {
                    refreshGroupsForUser(username);
                }
            } catch (UserNotFoundException ignored) {
                //
            }
        }
    }

    @Override
    public Date getUserLastUpdateTime(final UserI user) {
        return getUserLastUpdateTime(user.getUsername());
    }

    @Override
    public Date getUserLastUpdateTime(final String username) {
        try {
            @SuppressWarnings("unchecked") final List<String> cacheIds = new ArrayList<>(buildImmutableSet(getGroupIdsForUser(username), getCacheIdsForUsername(username)));
            if (cacheIds.isEmpty()) {
                return new Date();
            }
            if (log.isDebugEnabled()) {
                log.debug("Found {} cache entries related to user {}: {}", cacheIds.size(), username, StringUtils.join(cacheIds, ", "));
            }
            final long lastUpdateTime = Collections.max(Lists.transform(cacheIds, new Function<String, Long>() {
                @Override
                public Long apply(@Nullable final String cacheId) {
                    final Date lastUpdateTime = getCacheEntryLastUpdateTime(cacheId);
                    log.trace("User {} cache entry '{}' last updated: {}", username, cacheId, lastUpdateTime == null ? "null" : lastUpdateTime.getTime());
                    return lastUpdateTime == null ? 0L : lastUpdateTime.getTime();
                }
            }));
            log.debug("Found latest cache entry last updated time for user {}: {}", username, lastUpdateTime);
            return new Date(lastUpdateTime);
        } catch (UserNotFoundException ignored) {
            log.warn("Someone requested the cache entry last updated time for user {} but that user wasn't found", username);
            return new Date();
        }
    }

    /**
     * Finds all user element cache IDs for the specified user and evicts them from the cache.
     *
     * @param username The username to be cleared.
     */
    @Override
    public void clearUserCache(final String username) {
        final List<String> cacheIds = getCacheIdsForUserElements(username);
        if (log.isDebugEnabled()) {
            log.debug("Clearing caches for user '{}': {}", username, StringUtils.join(cacheIds, ", "));
        }
        evict(cacheIds);
    }

    @Override
    public boolean canInitialize() {
        try {
            if (_listener == null) {
                return false;
            }
            final boolean doesUserGroupTableExists            = _helper.tableExists("xdat_usergroup");
            final boolean isXftManagerComplete                = XFTManager.isComplete();
            final boolean isDatabasePopulateOrUpdateCompleted = XDATServlet.isDatabasePopulateOrUpdateCompleted();
            log.info("User group table {}, XFTManager initialization completed {}, database populate or updated completed {}", doesUserGroupTableExists, isXftManagerComplete, isDatabasePopulateOrUpdateCompleted);
            return doesUserGroupTableExists && isXftManagerComplete && isDatabasePopulateOrUpdateCompleted;
        } catch (SQLException e) {
            log.info("Got an SQL exception checking for xdat_usergroup table", e);
            return false;
        }
    }

    @Async
    @Override
    public Future<Boolean> initialize() {
        final LapStopWatch stopWatch = LapStopWatch.createStarted(log, Level.INFO);

        // This clears out any group initialization requests that may be left in the database from earlier starts.
        _template.update("DELETE FROM activemq_msgs WHERE container LIKE '%initializeGroupRequest'", EmptySqlParameterSource.INSTANCE);

        final int tags = initializeTags();
        stopWatch.lap("Processed {} tags", tags);

        final List<String> groupIds = _template.queryForList(QUERY_ALL_GROUPS, EmptySqlParameterSource.INSTANCE, String.class);
        _listener.setGroupIds(groupIds);
        stopWatch.lap("Initialized listener of type {} with {} tags", _listener.getClass().getName(), tags);

        try {
            final UserI adminUser = Users.getAdminUser();
            assert adminUser != null;

            stopWatch.lap("Found {} group IDs to run through, initializing cache with these as user {}", groupIds.size(), adminUser.getUsername());
            for (final String groupId : groupIds) {
                stopWatch.lap(Level.DEBUG, "Creating queue entry for group {}", groupId);
                XDAT.sendJmsRequest(_jmsTemplate, new InitializeGroupRequest(groupId));
            }
        } finally {
            if (stopWatch.isStarted()) {
                stopWatch.stop();
            }
            log.info("Total time to queue {} groups was {} ms", groupIds.size(), NUMBER_FORMAT.format(stopWatch.getTime()));
            if (log.isInfoEnabled()) {
                log.info(stopWatch.toTable());
            }
        }

        resetGuestBrowseableElementDisplays();
        _initialized.set(true);
        return new AsyncResult<>(true);
    }

    @Override
    public boolean isInitialized() {
        return _initialized.get();
    }

    @Override
    public Map<String, String> getInitializationStatus() {
        final Map<String, String> status = new HashMap<>();
        if (_listener == null) {
            status.put("message", "No listener registered, so no status to report.");
            return status;
        }

        final Set<String> processed      = _listener.getProcessed();
        final int         processedCount = processed.size();
        final Set<String> unprocessed    = _listener.getUnprocessed();
        final Date        start          = _listener.getStart();

        status.put("start", DATE_FORMAT.format(start));
        status.put("processedCount", Integer.toString(processedCount));
        status.put("processed", StringUtils.join(processed, ", "));

        final Date completed = _listener.getCompleted();

        if (unprocessed.isEmpty() && completed != null) {
            final String duration = DurationFormatUtils.formatPeriodISO(start.getTime(), completed.getTime());
            status.put("completed", DATE_FORMAT.format(completed));
            status.put("duration", duration);
            status.put("message", "Cache initialization is complete. Processed " + processedCount + " groups in " + duration);
            return status;
        }

        final Date   now              = new Date();
        final String duration         = DurationFormatUtils.formatPeriodISO(start.getTime(), now.getTime());
        final int    unprocessedCount = unprocessed.size();

        status.put("unprocessedCount", Integer.toString(unprocessedCount));
        status.put("unprocessed", StringUtils.join(unprocessed, ", "));
        status.put("current", DATE_FORMAT.format(now));
        status.put("duration", duration);
        status.put("message", unprocessed.isEmpty()
                              ? "Cache initialization is on-going, with " + processedCount + " groups processed and no groups remaining, time elapsed so far is " + duration
                              : "Cache initialization is on-going, with " + processedCount + " groups processed and " + unprocessedCount + " groups remaining, time elapsed so far is " + duration);

        return status;
    }

    @Override
    public void registerListener(final Listener listener) {
        _listener = listener;
    }

    @Override
    public Listener getListener() {
        return _listener;
    }

    @Override
    protected boolean handleEventImpl(final XftItemEventI event) {
        switch (event.getXsiType()) {
            case XnatProjectdata.SCHEMA_ELEMENT_NAME:
                return handleProjectEvents(event);

            case XnatSubjectdata.SCHEMA_ELEMENT_NAME:
                return handleSubjectEvents(event);

            case XdatUsergroup.SCHEMA_ELEMENT_NAME:
                return handleGroupRelatedEvents(event);

            case XdatElementSecurity.SCHEMA_ELEMENT_NAME:
                return handleElementSecurityEvents(event);

            default:
                // This is always some type of experiment.
                return handleExperimentEvents(event);
        }
    }

    private boolean handleProjectEvents(final XftItemEventI event) {
        final String         xsiType    = event.getXsiType();
        final String         id         = event.getId();
        final String         action     = event.getAction();
        final Map<String, ?> properties = event.getProperties();

        try {
            switch (action) {
                case CREATE:
                    log.debug("New project created with ID {}, caching new instance", xsiType, id);
                    for (final String owner : getProjectOwners(id)) {
                        if (!Iterables.any(getActionElementDisplays(owner).get(SecurityManager.CREATE), CONTAINS_MR_SESSION)) {
                            initActionElementDisplays(owner, true);
                        }
                    }

                    final boolean created = !initGroups(getGroups(xsiType, id)).isEmpty();
                    final String access = Permissions.getProjectAccess(_template, id);
                    if (StringUtils.isNotBlank(access)) {
                        switch (access) {
                            case "public":
                                if (!Iterables.any(getActionElementDisplays(GUEST_USERNAME).get(SecurityManager.READ), CONTAINS_MR_SESSION)) {
                                    initActionElementDisplays(GUEST_USERNAME, true);
                                }

                            case "protected":
                                updateProjectRelatedCaches(xsiType, id, false);
                                break;
                        }
                    }
                    return created;

                case UPDATE:
                    log.debug("The {} object {} was updated, caching updated instance", xsiType, id);
                    if (properties.containsKey("accessibility")) {
                        final String accessibility = (String) properties.get("accessibility");
                        switch (accessibility) {
                            case "private":
                                return updateProjectRelatedCaches(xsiType, id, true);

                            case "public":
                                if (!Iterables.any(getActionElementDisplays(GUEST_USERNAME).get(SecurityManager.READ), CONTAINS_MR_SESSION)) {
                                    initActionElementDisplays(GUEST_USERNAME, true);
                                }

                            case "protected":
                                return updateProjectRelatedCaches(xsiType, id, true);

                            default:
                                log.warn("The project {}'s accessibility setting was updated to an invalid value: {}. Must be one of private, protected, or public.", id, accessibility);
                        }
                    }
                    break;

                case XftItemEventI.DELETE:
                    log.debug("The {} {} was deleted, removing related instances from cache", xsiType, id);
                    final String cacheId = getCacheIdForProject(id);
                    evict(cacheId);
                    for (final String accessCacheId : Iterables.filter(getCacheIdsForUserElements(), Predicates.contains(REGEX_USER_PROJECT_ACCESS_CACHE_ID))) {
                        final List<String> projectIds = getCachedList(accessCacheId);
                        if (projectIds != null && projectIds.contains(id)) {
                            final List<String> updated = new ArrayList<>(projectIds);
                            updated.remove(id);
                            forceCacheObject(accessCacheId, updated);
                        }
                    }
                    resetGuestBrowseableElementDisplays();
                    initReadableCountsForUsers(this.<String>getCachedSet(cacheId));
                    resetTotalCounts();
                    return true;

                default:
                    log.warn("I was informed that the '{}' action happened to the project with ID '{}'. I don't know what to do with this action.", action, xsiType, id);
                    break;
            }
        } catch (ItemNotFoundException e) {
            log.warn("While handling action {}, I couldn't find a group for type {} ID {}.", action, xsiType, id);
        }

        return false;
    }

    private boolean updateProjectRelatedCaches(final String xsiType, final String id, final boolean affectsOtherDataTypes) throws ItemNotFoundException {
        final boolean cachedRelatedGroups = !initGroups(getGroups(xsiType, id)).isEmpty();

        evict(GUEST_CACHE_ID);
        evict(GUEST_ACTION_READ);
        resetGuestBrowseableElementDisplays();
        initActionElementDisplays(GUEST_USERNAME, true);

        final Set<String> readableCountCacheIds = new HashSet<>(getCacheIdsForUserReadableCounts());
        if (affectsOtherDataTypes) {
            for (final String cacheId : readableCountCacheIds) {
                evict(cacheId);
            }
            resetTotalCounts();
        } else {
            // Update existing user element displays
            final List<String> cacheIds = getCacheIdsForActions();
            cacheIds.addAll(getCacheIdsForUserElements());
            clearAllUserProjectAccess();
            initReadableCountsForUsers(Sets.newHashSet(Iterables.filter(Lists.transform(cacheIds, FUNCTION_CACHE_IDS_TO_USERNAMES), Predicates.notNull())));
            resetProjectCount();
        }

        return cachedRelatedGroups;
    }

    private boolean handleSubjectEvents(final XftItemEventI event) {
        final String action = event.getAction();
        log.debug("Handling subject {} event for {} {}", XftItemEventI.ACTIONS.get(action), event.getXsiType(), event.getId());
        final Set<String> projectIds = new HashSet<>();
        switch (action) {
            case CREATE:
                projectIds.add(_template.queryForObject(QUERY_GET_SUBJECT_PROJECT, new MapSqlParameterSource("subjectId", event.getId()), String.class));
                resetTotalCounts();
                break;

            case SHARE:
                projectIds.add((String) event.getProperties().get("target"));
                break;

            case MOVE:
                projectIds.add((String) event.getProperties().get("origin"));
                projectIds.add((String) event.getProperties().get("target"));
                break;

            case XftItemEventI.DELETE:
                projectIds.add((String) event.getProperties().get("target"));
                handleGroupRelatedEvents(event);
                resetTotalCounts();
                break;

            default:
                log.warn("I was informed that the '{}' action happened to subject '{}'. I don't know what to do with this action.", action, event.getId());
        }
        if (projectIds.isEmpty()) {
            return false;
        }
        final Set<String> users = getProjectUsers(projectIds);
        for (final String username : users) {
            initReadableCountsForUser(username);
        }
        return true;
    }

    private boolean handleGroupRelatedEvents(final XftItemEventI event) {
        final String         xsiType    = event.getXsiType();
        final String         id         = event.getId();
        final String         action     = event.getAction();
        final Map<String, ?> properties = event.getProperties();
        final Set<String>    usernames  = new HashSet<>();

        try {
            final List<UserGroupI> groups = getGroups(xsiType, id);
            switch (action) {
                case CREATE:
                    log.debug("New {} created with ID {}, caching new instance", xsiType, id);
                    for (final UserGroupI group : groups) {
                        usernames.addAll(group.getUsernames());
                    }
                    log.debug("Handling create group event with ID '{}' for users: {}", id);
                    return !initGroups(groups).isEmpty();

                case UPDATE:
                    log.debug("The {} object {} was updated, caching updated instance", xsiType, id);
                    for (final UserGroupI group : groups) {
                        usernames.addAll(group.getUsernames());
                        evict(group.getId());
                    }
                    if (properties.containsKey(OPERATION) && StringUtils.equals((String) properties.get(OPERATION), OPERATION_REMOVE_USERS)) {
                        //noinspection unchecked
                        usernames.addAll((Collection<? extends String>) properties.get(USERS));
                    }
                    log.debug("Handling update group event with ID '{}' for users: {}", id);
                    return !initGroups(groups).isEmpty();

                case XftItemEventI.DELETE:
                    if (StringUtils.equals(XnatProjectdata.SCHEMA_ELEMENT_NAME, xsiType)) {
                        final List<String> groupIds = getTagGroups(id);
                        if (CollectionUtils.isNotEmpty(groupIds)) {
                            log.info("Found {} groups cached for deleted project {}", groupIds.size(), id);
                            for (final String groupId : groupIds) {
                                evictGroup(groupId, usernames);
                            }
                        }
                    } else {
                        evictGroup(id, usernames);
                    }
                    break;

                default:
                    log.warn("I was informed that the '{}' action happened to the {} object with ID '{}'. I don't know what to do with this action.", action, xsiType, id);
            }
        } catch (ItemNotFoundException e) {
            log.warn("While handling action {}, I couldn't find a group for type {} ID {}.", action, xsiType, id);
        } finally {
            for (final String username : usernames) {
                try {
                    final String cacheId = getCacheIdForUserGroups(username);
                    initUserGroupIds(cacheId, username);
                    initReadableCountsForUser(username);
                } catch (UserNotFoundException e) {
                    log.warn("While handling action {} for type {} ID {}, I couldn't find a user with username {}.", action, xsiType, id, username);
                }
            }
        }
        return false;
    }

    private boolean handleElementSecurityEvents(final XftItemEventI event) {
        log.debug("Handling {} event for '{}' IDs {}. Updating guest browseable element displays...", event.getAction(), event.getXsiType(), event.getIds());
        final Map<String, ElementDisplay> displays = resetGuestBrowseableElementDisplays();

        if (log.isTraceEnabled()) {
            log.trace("Got back {} browseable element displays for guest user after refresh: {}", displays.size(), StringUtils.join(displays.keySet(), ", "));
        }

        log.debug("Evicting all action and user element cache IDs");
        for (final String cacheId : Iterables.concat(getCacheIdsForActions(), getCacheIdsForUserElements())) {
            log.trace("Evicting cache entry with ID '{}'", cacheId);
            evict(cacheId);
        }

        for (final String dataType : event.getIds()) {
            final List<String> groupIds = getGroupIdsForDataType(dataType);
            log.debug("Found {} groups that reference the '{}' data type, updating cache entries for: {}", groupIds.size(), dataType, StringUtils.join(groupIds, ", "));
            for (final String groupId : groupIds) {
                log.trace("Evicting group '{}' due to change in element securities for data type {}", groupId, dataType);
                evict(groupId);
            }
        }

        return true;
    }

    private boolean handleExperimentEvents(final XftItemEventI event) {
        final String action  = event.getAction();
        final String xsiType = event.getXsiType();
        log.debug("Handling experiment {} event for {} {}", XftItemEventI.ACTIONS.get(action), xsiType, event.getId());
        final String target, origin;
        switch (action) {
            case CREATE:
                target = _template.queryForObject(QUERY_GET_EXPERIMENT_PROJECT, new MapSqlParameterSource("experimentId", event.getId()), String.class);
                origin = null;
                break;

            case SHARE:
                target = (String) event.getProperties().get("target");
                origin = null;
                break;

            case MOVE:
                origin = (String) event.getProperties().get("origin");
                target = (String) event.getProperties().get("target");
                break;

            case XftItemEventI.DELETE:
                target = (String) event.getProperties().get("target");
                origin = null;
                break;

            default:
                log.warn("I was informed that the '{}' action happened to experiment '{}' with ID '{}'. I don't know what to do with this action.", action, xsiType, event.getId());
                return false;
        }

        final Map<String, ElementDisplay> displays = getGuestBrowseableElementDisplays();
        log.debug("Found {} elements for guest user: {}", displays.size(), StringUtils.join(displays.keySet(), ", "));

        // If the data type of the experiment isn't in the guest list AND the target project is public,
        // OR if the origin project is both specified and public (meaning the data type might be REMOVED
        // from the guest browseable element displays), then we update the guest browseable element displays.
        final boolean hasEventXsiType        = displays.containsKey(xsiType);
        final boolean isTargetProjectPublic  = Permissions.isProjectPublic(_template, target);
        final boolean hasOriginProject       = StringUtils.isNotBlank(origin);
        final boolean isMovedFromPublicToNon = !isTargetProjectPublic && hasOriginProject && Permissions.isProjectPublic(_template, origin);

        // We need to add the XSI type if guest doesn't already have it and the target project is public.
        final boolean needsPublicXsiTypeAdded = !hasEventXsiType && isTargetProjectPublic;

        // We need to check if the XSI type should be removed if guest has XSI type and item was moved from public to non-public.
        final boolean needsXsiTypeChecked = hasEventXsiType && isMovedFromPublicToNon;

        if (needsPublicXsiTypeAdded || needsXsiTypeChecked) {
            if (needsPublicXsiTypeAdded) {
                log.debug("Updating guest browseable element displays: guest doesn't have the event XSI type '{}' and the target project {} is public.", xsiType, target);
            } else {
                log.debug("Updating guest browseable element displays: guest has the event XSI type '{}' and item was moved from public project {} to non-public project {}.", xsiType, origin, target);
            }
            resetGuestBrowseableElementDisplays();
        } else {
            log.debug("Not updating guest browseable element displays: guest {} '{}' and {}",
                      hasEventXsiType ? "already has the event XSI type " : "doesn't have the event XSI type",
                      xsiType,
                      isTargetProjectPublic ? "target project is public" : "target project is not public");
        }
        initReadableCountsForUsers(hasOriginProject ? getProjectUsers(target) : getProjectUsers(target, origin));
        if (StringUtils.equalsAny(action, CREATE, XftItemEventI.DELETE)) {
            resetTotalCounts();
        }
        return true;
    }

    private void handleCacheRemoveEvent(final Ehcache cache, final Element element, final String event) {
        if (isGroupsAndPermissionsCacheEvent(cache)) {
            if (element == null) {
                log.debug("Got a {} event for cache {}, no specific element affected", event, cache.getName());
                return;
            }
            final Object objectValue = element.getObjectValue();
            log.debug("Got a {} event for cache {} on ID {} with value of type {}", event, cache.getName(), element.getObjectKey(), objectValue != null ? objectValue.getClass().getName() : "<null>");
        }
    }

    private List<String> getProjectOwners(final String projectId) {
        return _template.queryForList(QUERY_PROJECT_OWNERS, new MapSqlParameterSource("projectId", projectId), String.class);
    }

    private ListMultimap<String, ElementDisplay> getActionElementDisplays(final String username) {
        final String cacheId = getCacheIdForActionElements(username);

        // Check whether the action elements are cached and, if so, return that.
        final ListMultimap<String, ElementDisplay> cachedActions = getCachedListMultimap(cacheId);
        if (cachedActions != null) {
            log.debug("Found a cache entry for user '{}' action elements by ID '{}'", username, cacheId);
            return cachedActions;
        }

        return initActionElementDisplays(username);
    }

    private Long getUserReadableWorkflowCount(final UserI user) {
        return _template.queryForObject(QUERY_USER_READABLE_WORKFLOW_COUNT, new MapSqlParameterSource("username", user.getUsername()), Long.class);
    }

    @Nonnull
    private Map<String, UserGroupI> getMutableGroupsForUser(final String username) throws UserNotFoundException {
        final List<String>            groupIds = getGroupIdsForUser(username);
        final Map<String, UserGroupI> groups   = new HashMap<>();
        for (final String groupId : groupIds) {
            final UserGroupI group = get(groupId);
            if (group != null) {
                log.trace("Adding group {} to groups for user {}", groupId, username);
                groups.put(groupId, group);
            } else {
                log.info("User '{}' is associated with the group ID '{}', but I couldn't find that actual group", username, groupId);
            }
        }
        return groups;
    }

    @Nonnull
    private Map<String, ElementAccessManager> getElementAccessManagers(final String username) {
        if (StringUtils.isBlank(username)) {
            return Collections.emptyMap();
        }
        final String                            cacheId                     = getCacheIdForUserElementAccessManagers(username);
        final Map<String, ElementAccessManager> cachedElementAccessManagers = getCachedMap(cacheId);
        if (cachedElementAccessManagers != null) {
            log.debug("Found a cache entry for user '{}' element access managers by ID '{}' with {} entries", username, cacheId, cachedElementAccessManagers.size());
            return cachedElementAccessManagers;
        }
        return initElementAccessManagersForUser(cacheId, username);
    }

    private Map<String, ElementDisplay> getGuestBrowseableElementDisplays() {
        final Map<String, ElementDisplay> guestBrowseableElementDisplays = getCachedMap(GUEST_CACHE_ID);
        if (guestBrowseableElementDisplays != null) {
            return guestBrowseableElementDisplays;
        }
        return resetBrowseableElementDisplays(_guest);
    }

    private void clearAllUserProjectAccess() {
        final List<String> cacheIds = getCacheIdsForUserElements();
        for (final String cacheId : cacheIds) {
            evict(cacheId);
        }
    }

    /**
     * This method retrieves the group with the specified group ID and puts it in the cache. This method
     * does <i>not</i> check to see if the group is already in the cache! This is primarily for use during
     * cache initialization and shouldn't be used for routine access.
     *
     * @param groupId The ID or alias of the group to retrieve.
     *
     * @return The group object for the specified ID if it exists, null otherwise.
     */
    @CacheLock(true)
    private UserGroupI initGroup(@CacheId final String groupId) throws ItemNotFoundException {
        return initGroupImpl(groupId, new UserGroup(groupId, _template));
    }

    @CacheLock(true)
    private UserGroupI initGroup(@CacheId final String groupId, final UserGroupI group) {
        return initGroupImpl(groupId, group);
    }

    private UserGroupI initGroupImpl(final String groupId, final UserGroupI group) {
        log.info("Initializing cache entry for group '{}'", groupId);
        cacheObject(groupId, group);
        log.debug("Retrieved and cached the group for the ID {}", groupId);
        return group;
    }

    @CacheLock(true)
    private Map<String, Long> initReadableCountsForUser(final String cacheId, final UserI user) {
        log.info("Initializing readable counts for user '{}' with cache entry '{}'", user.getUsername(), cacheId);
        final String username = user.getUsername();
        try {
            if (!user.isGuest()) {
                initProjectMember(cacheId, username);
            }

            final Map<String, Long> readableCounts   = new HashMap<>();
            final List<String>      readableProjects = getUserReadableProjects(username);
            readableCounts.put(XnatProjectdata.SCHEMA_ELEMENT_NAME, (long) readableProjects.size());
            readableCounts.put(WrkWorkflowdata.SCHEMA_ELEMENT_NAME, getUserReadableWorkflowCount(user));
            readableCounts.putAll(getUserReadableSubjectsAndExperiments(readableProjects));

            if (log.isDebugEnabled()) {
                log.debug("Caching the following readable element counts for user '{}' with cache ID '{}': '{}'", username, cacheId, getDisplayForReadableCounts(readableCounts));
            }

            cacheObject(cacheId, readableCounts);
            return ImmutableMap.copyOf(readableCounts);
        } catch (DataAccessException e) {
            log.error("An error occurred in the SQL for retrieving readable counts for the  user {}", username, e);
            return Collections.emptyMap();
        }
    }

    @CacheLock(true)
    private void initProjectMember(final String cacheId, final String username) {
        log.debug("Initializing cached project entries for user '{}' with cache ID '{}', initializing entry", username, cacheId);
        final List<String> projects = getUserProjects(username);
        for (final String project : projects) {
            final String      projectCacheId = getCacheIdForProject(project);
            final Set<String> cachedSet      = getCachedSet(projectCacheId);
            final Set<String> projectUsers   = new HashSet<>();
            projectUsers.add(username);
            if (cachedSet != null) {
                projectUsers.addAll(cachedSet);
            }
            forceCacheObject(projectCacheId, projectUsers);
        }
    }

    @CacheLock(true)
    private synchronized List<String> initTag(final String cacheId, final String tag) {
        // If there's a blank tag...
        if (StringUtils.isBlank(tag)) {
            throw new IllegalArgumentException("Can not request a blank tag, that's not a thing.");
        }

        log.info("Initializing cache entry for tag '{}' with cache ID '{}'", tag, cacheId);

        // Then retrieve and cache the groups if found or cache DOES_NOT_EXIST if the tag isn't found.
        final List<String> groups = getGroupIdsForProject(tag);

        // If this is empty, then the tag doesn't exist and we'll just put DOES_NOT_EXIST there.
        if (groups.isEmpty()) {
            log.info("Someone tried to get groups for the tag {}, but there are no groups with that tag.", tag);
            return Collections.emptyList();
        } else {
            log.debug("Cached tag {} for {} groups: {}", tag, groups.size(), StringUtils.join(groups, ", "));
            cacheObject(cacheId, groups);
            return groups;
        }
    }

    @CacheLock(true)
    private Map<String, ElementAccessManager> initElementAccessManagersForUser(final String cacheId, final String username) {
        log.info("Initializing element access managers cache entry for user '{}' with cache ID '{}'", username, cacheId);
        final Map<String, ElementAccessManager> managers = ElementAccessManager.initialize(_template, QUERY_USER_PERMISSIONS, new MapSqlParameterSource("username", username));

        log.trace("Found {} element access managers for user '{}'", managers.size());
        cacheObject(cacheId, managers);

        return managers;
    }

    @CacheLock(true)
    private synchronized Map<String, ElementDisplay> initBrowseableElementDisplaysForUser(final String cacheId, final UserI user) {
        final String username = user.getUsername();

        log.info("Initializing browseable element displays cache entry for user '{}' with cache ID '{}'", username, cacheId);
        final Map<String, Long> counts = getReadableCounts(user);
        log.debug("Found {} readable counts for user {}: {}", counts.size(), username, counts);

        try {
            final Map<String, ElementDisplay> browseableElements = new HashMap<>();
            final List<ElementDisplay>        elementDisplays    = getActionElementDisplays(user.getUsername(), SecurityManager.READ);
            if (log.isTraceEnabled()) {
                log.trace("Found {} readable action element displays for user {}: {}", elementDisplays.size(), username, formatElementDisplays(elementDisplays));
            }

            for (final ElementDisplay elementDisplay : elementDisplays) {
                final String  elementName         = elementDisplay.getElementName();
                final boolean isBrowseableElement = ElementSecurity.IsBrowseableElement(elementName);
                final boolean countsContainsKey   = counts.containsKey(elementName);
                final boolean hasOneOrMoreElement = countsContainsKey && counts.get(elementName) > 0;
                if (isBrowseableElement && countsContainsKey && hasOneOrMoreElement) {
                    log.debug("Adding element display {} to cache entry {} for user {}", elementName, cacheId, username);
                    browseableElements.put(elementName, elementDisplay);
                } else if (log.isTraceEnabled()) {
                    log.trace("Did not add element display {} for user {}: {}, {}", elementName, username, isBrowseableElement ? "browseable" : "not browseable", countsContainsKey ? "counts contains key" : "counts does not contain key", hasOneOrMoreElement ? "counts has one or more elements of this type" : "counts does not have any elements of this type");
                }
            }

            log.info("Adding {} element displays to cache entry {}", browseableElements.size(), cacheId);
            cacheObject(cacheId, browseableElements);
            return browseableElements;
        } catch (ElementNotFoundException e) {
            if (!_missingElements.containsKey(e.ELEMENT)) {
                log.warn("Element '{}' not found. This may be a data type that was installed previously but can't be located now. This warning will only be displayed once. Set logging level to DEBUG to see a message each time this occurs for each element, along with a count of the number of times the element was referenced.", e.ELEMENT);
                _missingElements.put(e.ELEMENT, 1L);
            } else {
                final long count = _missingElements.get(e.ELEMENT) + 1;
                _missingElements.put(e.ELEMENT, count);
                log.debug("Element '{}' not found. This element has been referenced {} times.", e.ELEMENT, count);
            }
        } catch (XFTInitException e) {
            log.error("There was an error initializing or accessing XFT", e);
        } catch (Exception e) {
            log.error("An unknown error occurred", e);
        }

        log.info("No browseable element displays found for user {}", username);
        return Collections.emptyMap();
    }

    @Nonnull
    @CacheLock(true)
    private List<String> initUserGroupIds(final String cacheId, final String username) throws UserNotFoundException {
        log.info("Initializing user group IDs cache entry for user '{}' with cache ID '{}'", username, cacheId);
        final List<String> groupIds = _template.queryForList(QUERY_GET_GROUPS_FOR_USER, checkUser(username), String.class);
        log.debug("Found {} user group IDs cache entry for user '{}'", groupIds.size(), username);
        cacheObject(cacheId, groupIds);
        return ImmutableList.copyOf(groupIds);
    }

    private ListMultimap<String, ElementDisplay> initActionElementDisplays(final String username) {
        return initActionElementDisplays(username, false);
    }

    private synchronized ListMultimap<String, ElementDisplay> initActionElementDisplays(final String username, final boolean evict) {
        final String cacheId = getCacheIdForActionElements(username);
        log.info("Initializing action element displays cache entry {} for user '{}', evict is {}", cacheId, username, evict);

        // If they want to evict the cache entry, then do that and proceed. They explicitly don't want any cached entry to be returned.
        if (evict) {
            evict(cacheId);
        } else {
            final ListMultimap<String, ElementDisplay> cachedObject = getCachedListMultimap(cacheId);
            if (cachedObject != null) {
                log.debug("Found an existing cached entry for cache ID {}, which probably means the cache was initialized while this call was locked out by method sync", cacheId);
                return cachedObject;
            }
        }

        final ListMultimap<String, ElementDisplay> elementDisplays = ArrayListMultimap.create();
        try {
            final List<ElementSecurity> securities = ElementSecurity.GetSecureElements();
            if (log.isDebugEnabled()) {
                log.debug("Evaluating {} element security objects: {}", securities.size(), StringUtils.join(Lists.transform(securities, FUNCTION_ELEMENT_SECURITY_TO_STRING), ", "));
            }
            for (final ElementSecurity elementSecurity : securities) {
                try {
                    final SchemaElement schemaElement = elementSecurity.getSchemaElement();
                    if (schemaElement != null) {
                        final String fullXMLName = schemaElement.getFullXMLName();
                        log.debug("Evaluating schema element {}", fullXMLName);
                        if (schemaElement.hasDisplay()) {
                            log.debug("Schema element {} has a display", fullXMLName);
                            for (final String action : ACTIONS) {
                                log.debug("Check user {} permission for action {} on schema element {}", username, action, fullXMLName);
                                if (Permissions.canAny(username, elementSecurity.getElementName(), action)) {
                                    log.debug("User {} can {} schema element {}", username, action, fullXMLName);
                                    final ElementDisplay elementDisplay = schemaElement.getDisplay();
                                    if (elementDisplay != null) {
                                        log.debug("Adding element display {} to action {} for user {}", elementDisplay.getElementName(), action, username);
                                        elementDisplays.put(action, elementDisplay);
                                    }
                                } else {
                                    log.debug("User {} can not {} schema element {}", username, action, fullXMLName);
                                }
                            }
                        } else {
                            log.debug("Schema element {} does not have a display, rejecting", fullXMLName);
                        }
                    } else {
                        log.warn("Element '{}' not found. This may be a data type that was installed previously but can't be located now.", elementSecurity.getElementName());
                    }
                } catch (ElementNotFoundException e) {
                    log.warn("Element '{}' not found. This may be a data type that was installed previously but can't be located now.", e.ELEMENT);
                } catch (Exception e) {
                    log.error("An exception occurred trying to retrieve a secure element schema", e);
                }
            }
        } catch (Exception e) {
            log.error("An error occurred trying to retrieve the list of secure elements. Proceeding but things probably won't go well from here on out.", e);
        }

        try {
            for (final ElementSecurity elementSecurity : ElementSecurity.GetInSecureElements()) {
                try {
                    final SchemaElement schemaElement = elementSecurity.getSchemaElement();
                    if (schemaElement.hasDisplay()) {
                        final ElementDisplay elementDisplay = schemaElement.getDisplay();
                        log.debug("Adding all actions for insecure schema element {} to user {} permissions", elementDisplay.getElementName(), username);
                        for (final String action : ACTIONS) {
                            elementDisplays.put(action, elementDisplay);
                        }
                    }
                } catch (ElementNotFoundException e) {
                    log.warn("Element '{}' not found. This may be a data type that was installed previously but can't be located now.", e.ELEMENT);
                } catch (Exception e) {
                    log.error("An exception occurred trying to retrieve an insecure element schema", e);
                }
            }
        } catch (Exception e) {
            log.error("An error occurred trying to retrieve the list of insecure elements. Proceeding but things probably won't go well from here on out.", e);
        }

        if (log.isTraceEnabled()) {
            final List<ElementDisplay> readElements   = elementDisplays.get(SecurityManager.READ);
            final List<ElementDisplay> editElements   = elementDisplays.get(SecurityManager.EDIT);
            final List<ElementDisplay> createElements = elementDisplays.get(SecurityManager.CREATE);
            log.trace("Cached {} elements with READ access, {} elements with EDIT access, and {} elements with CREATE access for user {} with cache ID {}:\n * READ: {}\n * EDIT: {}\n * CREATE: {}", readElements.size(), editElements.size(), createElements.size(), username, cacheId, formatElementDisplays(readElements), formatElementDisplays(editElements), formatElementDisplays(createElements));
        } else {
            log.info("Cached {} elements with READ access, {} elements with EDIT access, and {} elements with CREATE access for user {} with cache ID {}", elementDisplays.get(SecurityManager.READ).size(), elementDisplays.get(SecurityManager.EDIT).size(), elementDisplays.get(SecurityManager.CREATE).size(), username, cacheId);
        }

        cacheObject(cacheId, elementDisplays);

        return elementDisplays;
    }

    private void initReadableCountsForUsers(@Nullable final Collection<String> usernames) {
        if (CollectionUtils.isNotEmpty(usernames)) {
            log.info("Initializing readable counts cache entry for {} users: {}", usernames.size(), usernames);
            for (final String username : usernames) {
                initReadableCountsForUser(username);
            }
        } else {
            log.info("Request to initialize readable counts cache entry for users but the list of users is null or empty");
        }
    }

    private void initReadableCountsForUser(final String username) {
        final String cacheId = getCacheIdForUserElements(username, READABLE);
        log.debug("Retrieving readable counts for user {} through cache ID {}", username, cacheId);

        // Check whether the user has readable counts and browseable elements cached. We only need to refresh
        // for those who have them cached.
        final Map<String, Long> cachedReadableCounts = getCachedMap(cacheId);
        if (cachedReadableCounts != null) {
            try {
                log.debug("Found a cache entry for user '{}' readable counts by ID '{}', updating cache entry", username, cacheId);
                final XDATUser user = new XDATUser(username);
                initReadableCountsForUser(cacheId, user);
                initBrowseableElementDisplaysForUser(getCacheIdForUserElements(username, BROWSEABLE), user);
            } catch (UserNotFoundException e) {
                log.warn("Got a user not found exception for username '{}', which is weird because this user has a cache entry.", username, e);
            } catch (UserInitException e) {
                log.error("An error occurred trying to retrieve the user '{}'", username, e);
            } catch (Exception e) {
                log.error("An unexpected error occurred trying to retrieve the readable counts for user '{}'", username, e);
            }
        }
    }

    private synchronized List<UserGroupI> initGroups(final List<UserGroupI> groups) {
        log.debug("Caching {} groups", groups.size());
        return Lists.transform(groups, new InitGroupFunction(this));
    }

    private void resetTotalCounts() {
        resetProjectCount();
        final Long subjectCount = _template.queryForObject("SELECT COUNT(*) FROM xnat_subjectData", EmptySqlParameterSource.INSTANCE, Long.class);
        _totalCounts.put(XnatSubjectdata.SCHEMA_ELEMENT_NAME, subjectCount);
        final List<Map<String, Object>> elementCounts = _template.queryForList("SELECT element_name, COUNT(ID) AS count FROM xnat_experimentData expt LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id GROUP BY element_name", EmptySqlParameterSource.INSTANCE);
        for (final Map<String, Object> elementCount : elementCounts) {
            _totalCounts.put((String) elementCount.get("element_name"), (Long) elementCount.get("count"));
        }
    }

    private void resetProjectCount() {
        _totalCounts.clear();
        final Long projectCount = _template.queryForObject("SELECT COUNT(*) FROM xnat_projectData", EmptySqlParameterSource.INSTANCE, Long.class);
        _totalCounts.put(XnatProjectdata.SCHEMA_ELEMENT_NAME, projectCount);
    }

    private Map<String, ElementDisplay> resetGuestBrowseableElementDisplays() {
        return resetBrowseableElementDisplays(getGuest());
    }

    private Map<String, ElementDisplay> resetBrowseableElementDisplays(final XDATUser user) {
        log.debug("Updating guest browseable element displays, clearing local cache, updating element access managers, and updating browseable element displays");
        user.clearLocalCache();
        final String username = user.getUsername();
        initElementAccessManagersForUser(getCacheIdForUserElementAccessManagers(username), username);
        return ImmutableMap.copyOf(initBrowseableElementDisplaysForUser(getCacheIdForUserElements(username, BROWSEABLE), user));
    }

    private Map<String, Long> getUserReadableSubjectsAndExperiments(final List<String> readableProjectIds) {
        if (readableProjectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final MapSqlParameterSource parameters               = new MapSqlParameterSource("projectIds", readableProjectIds);
        final Map<String, Long>     readableExperimentCounts = _template.query(QUERY_USER_READABLE_EXPERIMENT_COUNT, parameters, ELEMENT_COUNT_EXTRACTOR);
        final Long                  readableSubjectCount     = _template.queryForObject(QUERY_USER_READABLE_SUBJECT_COUNT, parameters, Long.class);
        readableExperimentCounts.put(XnatSubjectdata.SCHEMA_ELEMENT_NAME, readableSubjectCount);
        return readableExperimentCounts;
    }

    private List<UserGroupI> getGroups(final String type, final String id) throws ItemNotFoundException {
        switch (type) {
            case XnatProjectdata.SCHEMA_ELEMENT_NAME:
                return getGroupsForTag(id);

            case XdatUsergroup.SCHEMA_ELEMENT_NAME:
                return Collections.<UserGroupI>singletonList(new UserGroup(id, _template));

            case XdatElementSecurity.SCHEMA_ELEMENT_NAME:
                final List<String> groupIds = getGroupIdsForDataType(id);
                return initGroups(Lists.transform(groupIds, new Function<String, UserGroupI>() {
                    @Override
                    public UserGroupI apply(final String groupId) {
                        try {
                            return new UserGroup(groupId, _template);
                        } catch (ItemNotFoundException e) {
                            log.warn("Somehow didn't find a usergroup that should exist: {}", groupId, e);
                            return null;
                        }
                    }
                }));
        }
        return Collections.emptyList();
    }

    private List<String> getGroupIdsForDataType(final String dataType) {
        return _template.queryForList(QUERY_GET_GROUPS_FOR_DATATYPE, new MapSqlParameterSource("dataType", dataType), String.class);
    }

    private List<String> getGroupIdsForProject(final String projectId) {
        return _template.queryForList(QUERY_GET_GROUPS_FOR_TAG, new MapSqlParameterSource("tag", projectId), String.class);
    }

    /**
     * Retrieves a list of projects where the specified user is a member. This differs from {@link #getUserReadableProjects(String)} in that it
     * includes neither public projects nor projects to which the user has read access due to all data access privileges.
     *
     * @param username The username to retrieve projects for.
     *
     * @return A list of projects for which the specified user is a member.
     */
    private List<String> getUserProjects(final String username) {
        return _template.queryForList(QUERY_GET_PROJECTS_FOR_USER, new MapSqlParameterSource("username", username), String.class);
    }

    /**
     * Retrieves a list of projects where the specified user has read access. This differs from {@link #getUserProjects(String)} in that it
     * includes protected and public projects and projects to which the user has read access due to all data access privileges.
     *
     * @param username The username to retrieve projects for.
     *
     * @return A list of projects to which the specified user has read access.
     */
    private List<String> getUserReadableProjects(final String username) {
        return getProjectsByAccessQuery(username, QUERY_READABLE_PROJECTS, false);
    }

    private List<String> getUserEditableProjects(final String username) {
        return getProjectsByAccessQuery(username, QUERY_EDITABLE_PROJECTS, true);
    }

    private List<String> getUserOwnedProjects(final String username) {
        return getProjectsByAccessQuery(username, QUERY_OWNED_PROJECTS, true);
    }

    private List<String> getProjectsByAccessQuery(final String username, final String query, final boolean requireAdminAccess) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource("username", username);
        return hasAllDataAdmin(username) || (hasAllDataAccess(username) && !requireAdminAccess)
               ? _template.queryForList(QUERY_ALL_DATA_ACCESS_PROJECTS, parameters, String.class)
               : _template.queryForList(query, parameters, String.class);
    }

    /**
     * Cheap test to see if the user is in a group that has data read access on all projects.
     *
     * @param username The username to be checked.
     *
     * @return Returns true if the user has all-data-access, false otherwise.
     */
    private boolean hasAllDataAccess(final String username) {
        return _template.queryForObject(QUERY_HAS_ALL_DATA_ACCESS, new MapSqlParameterSource("username", username), Boolean.class);
    }

    /**
     * Cheap test to see if the user is in a group that has data admin access on all projects.
     *
     * @param username The username to be checked.
     *
     * @return Returns true if the user has all-data-admin, false otherwise.
     */
    private boolean hasAllDataAdmin(final String username) {
        return _template.queryForObject(QUERY_HAS_ALL_DATA_ADMIN, new MapSqlParameterSource("username", username), Boolean.class);
    }

    private int initializeTags() {
        final List<String> tags = _template.queryForList(QUERY_ALL_TAGS, EmptySqlParameterSource.INSTANCE, String.class);
        for (final String tag : tags) {
            getTagGroups(tag);
        }
        final int size = tags.size();
        log.info("Completed initialization of {} tags", size);
        return size;
    }

    private Set<String> getProjectUsers(final String... projectIds) {
        return getProjectUsers(Arrays.asList(projectIds));
    }

    private Set<String> getProjectUsers(final Collection<String> projectIds) {
        return projectIds.isEmpty() ? Collections.<String>emptySet() : new HashSet<>(_template.queryForList(QUERY_GET_USERS_FOR_PROJECTS, new MapSqlParameterSource("projectIds", projectIds), String.class));
    }

    private List<String> getTagGroups(final String tag) {
        final String       cacheId  = getCacheIdForTag(tag);
        final List<String> groupIds = getCachedList(cacheId);
        if (groupIds != null) {
            // If it's cached, we can just return the list.
            log.info("Found {} groups cached for tag '{}'", groupIds.size(), tag);
            return groupIds;
        }

        return initTag(getCacheIdForTag(tag), tag);
    }

    private List<String> getGroupIdsForUser(final String username) throws UserNotFoundException {
        final String       cacheId    = getCacheIdForUserGroups(username);
        final List<String> cachedList = getCachedList(cacheId);
        if (cachedList != null) {
            if (log.isTraceEnabled()) {
                log.trace("Found cached groups list for user '{}' with cache ID '{}': {}", username, cacheId, StringUtils.join(cachedList, ", "));
            } else {
                log.info("Found cached groups list for user '{}' with cache ID '{}' with {} entries", username, cacheId, cachedList.size());
            }
            return cachedList;
        }
        return initUserGroupIds(cacheId, username);
    }

    private void evictGroup(final String groupId, final Set<String> usernames) {
        final UserGroupI group = get(groupId);
        if (group != null) {
            final List<String> groupUsernames = group.getUsernames();
            log.debug("Found group for ID '{}' with {} associated users", groupId, groupUsernames.size());
            usernames.addAll(groupUsernames);
            evict(groupId);
        } else {
            log.info("Requested to evict group with ID '{}', but I couldn't find that actual group", groupId);
        }
    }

    /**
     * Checks whether the users exists. If not, this throws the {@link UserNotFoundException}. Otherwise it returns
     * a parameter source containing the username that can be used in subsequent queries.
     *
     * @param username The user to test.
     *
     * @return A parameter source containing the username parameter.
     *
     * @throws UserNotFoundException If the user doesn't exist.
     */
    private MapSqlParameterSource checkUser(final String username) throws UserNotFoundException {
        final MapSqlParameterSource parameters = new MapSqlParameterSource("username", username);

        // If the user isn't in the check map OR the user is in the check map but is set as not existing...
        if (!_userChecks.containsKey(username) || !_userChecks.get(username)) {
            // See if the user exists now. The non-existent user existing should be updated with the add user event,
            // but we don't have a clearly defined handler for that yet.
            _userChecks.put(username, _template.queryForObject(QUERY_CHECK_USER_EXISTS, parameters, Boolean.class));
        }
        if (!_userChecks.get(username)) {
            throw new UserNotFoundException(username);
        }
        return parameters;
    }

    private XDATUser getGuest() {
        if (_guest == null) {
            log.debug("No guest user initialized, trying to retrieve now.");
            try {
                final UserI guest = Users.getGuest();
                if (guest instanceof XDATUser) {
                    _guest = (XDATUser) guest;
                } else {
                    _guest = new XDATUser(guest.getUsername());
                }
            } catch (UserNotFoundException e) {
                log.error("Got a user name not found exception for the guest user which is very strange.", e);
            } catch (UserInitException e) {
                log.error("Got a user init exception for the guest user which is very unfortunate.", e);
            }
        }
        return _guest;
    }

    private boolean isGuest(final String username) {
        return _guest != null ? StringUtils.equalsIgnoreCase(_guest.getUsername(), username) : StringUtils.equalsIgnoreCase(GUEST_USERNAME, username);
    }

    private List<UserGroupI> getUserGroupList(final List groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return new ArrayList<>();
        }

        return ImmutableList.copyOf(Iterables.filter(Iterables.transform(Iterables.filter(groupIds, String.class), new Function<String, UserGroupI>() {
            @Override
            public UserGroupI apply(final String groupId) {
                return get(groupId);
            }
        }), Predicates.notNull()));
    }

    private UserGroupI getCachedGroup(final String cacheId) {
        return getCachedObject(cacheId, UserGroupI.class);
    }

    private List<String> getCacheIdsForUserReadableCounts() {
        return Lists.newArrayList(Iterables.filter(Iterables.filter(getEhCache().getKeys(), String.class), Predicates.contains(REGEX_USER_READABLE_COUNTS)));
    }

    private List<String> getCacheIdsForActions() {
        return getCacheIdsForPrefix(ACTIONS_PREFIX);
    }

    private List<String> getCacheIdsForUserElements() {
        return getCacheIdsForPrefix(USER_ELEMENT_PREFIX);
    }

    private List<String> getCacheIdsForUserElements(final String username) {
        return getCacheIdsForPrefix(USER_ELEMENT_PREFIX, username);
    }

    private List<String> getCacheIdsForPrefix(final String... prefixes) {
        return Lists.newArrayList(Iterables.filter(Iterables.filter(getEhCache().getKeys(), String.class), Predicates.containsPattern("^" + StringUtils.join(prefixes, ":") + ":.*$")));
    }

    private List<String> getCacheIdsForUsername(final String username) {
        return Lists.newArrayList(Iterables.filter(Iterables.filter(getEhCache().getKeys(), String.class), new Predicate<String>() {
            @Override
            public boolean apply(@Nullable final String cacheId) {
                return StringUtils.equals(username, getUsernameFromCacheId(cacheId));
            }
        }));
    }

    private static String getUsernameFromCacheId(final @Nullable String cacheId) {
        if (StringUtils.isBlank(cacheId)) {
            return null;
        }
        final Matcher matcher = REGEX_EXTRACT_USER_FROM_CACHE_ID.matcher(cacheId);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group("username");
    }

    private static String getCacheIdForTag(final String tag) {
        return StringUtils.startsWith(tag, TAG_PREFIX) ? tag : createCacheIdFromElements(TAG_PREFIX, tag);
    }

    private static String getCacheIdForProject(final String projectId) {
        return StringUtils.startsWith(projectId, PROJECT_PREFIX) ? projectId : createCacheIdFromElements(PROJECT_PREFIX, projectId);
    }

    private static String getDisplayForReadableCounts(final Map<String, Long> readableCounts) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(readableCounts.get(XnatProjectdata.SCHEMA_ELEMENT_NAME)).append(" projects, ");
        buffer.append(readableCounts.get(WrkWorkflowdata.SCHEMA_ELEMENT_NAME)).append(" workflows, ");
        buffer.append(readableCounts.get(XnatSubjectdata.SCHEMA_ELEMENT_NAME)).append(" subjects");
        if (readableCounts.size() > 3) {
            for (final String type : readableCounts.keySet()) {
                if (!StringUtils.equalsAny(type, XnatProjectdata.SCHEMA_ELEMENT_NAME, WrkWorkflowdata.SCHEMA_ELEMENT_NAME, XnatSubjectdata.SCHEMA_ELEMENT_NAME)) {
                    buffer.append(", ").append(readableCounts.get(type)).append(" ").append(type);
                }
            }
        }
        return buffer.toString();
    }

    private static String getCacheIdForUserElementAccessManagers(final String username) {
        return createCacheIdFromElements(USER_ELEMENT_PREFIX, username, ELEMENT_ACCESS_MANAGERS_PREFIX);
    }

    private static String getCacheIdForUserGroups(final String username) {
        return createCacheIdFromElements(USER_ELEMENT_PREFIX, username, GROUPS_ELEMENT_PREFIX);
    }

    private static String getCacheIdForUserElements(final String username, final String elementType) {
        return createCacheIdFromElements(USER_ELEMENT_PREFIX, username, elementType);
    }

    private static String getCacheIdForActionElements(final String username) {
        return createCacheIdFromElements(ACTIONS_PREFIX, username);
    }

    private static boolean isGroupsAndPermissionsCacheEvent(final Ehcache cache) {
        return StringUtils.equals(CACHE_NAME, cache.getName());
    }

    private static String formatElementDisplays(final List<ElementDisplay> elementDisplays) {
        return StringUtils.join(Lists.transform(elementDisplays, FUNCTION_ELEMENT_DISPLAY_TO_STRING), ", ");
    }

    private static class InitGroupFunction implements Function<UserGroupI, UserGroupI> {
        InitGroupFunction(final DefaultGroupsAndPermissionsCache cache) {
            _cache = cache;
        }

        @Override
        public UserGroupI apply(final UserGroupI incoming) {
            log.error("I'm applying the thing for the group {}", incoming.getId());
            return _cache.initGroup(incoming.getId(), incoming);

        }

        private final DefaultGroupsAndPermissionsCache _cache;
    }

    private static final ResultSetExtractor<Map<String, Long>> ELEMENT_COUNT_EXTRACTOR = new ResultSetExtractor<Map<String, Long>>() {
        @Override
        public Map<String, Long> extractData(final ResultSet results) throws SQLException, DataAccessException {
            final Map<String, Long> elementCounts = new HashMap<>();
            while (results.next()) {
                final String elementName  = results.getString("element_name");
                final long   elementCount = results.getLong("element_count");
                log.debug("Found element '{}' with {} instances", elementName, elementCount);
                elementCounts.put(elementName, elementCount);
            }
            return elementCounts;
        }
    };

    private static final DateFormat   DATE_FORMAT   = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.getDefault());

    private static final String QUERY_GET_GROUPS_FOR_USER            = "SELECT groupid " +
                                                                       "FROM xdat_user_groupid xug " +
                                                                       "  LEFT JOIN xdat_user xu ON groups_groupid_xdat_user_xdat_user_id = xdat_user_id " +
                                                                       "WHERE xu.login = :username " +
                                                                       "ORDER BY groupid";
    private static final String QUERY_GET_GROUP_FOR_USER_AND_TAG     = "SELECT id " +
                                                                       "FROM xdat_usergroup xug " +
                                                                       "  LEFT JOIN xdat_user_groupid xugid ON xug.id = xugid.groupid " +
                                                                       "  LEFT JOIN xdat_user xu ON xugid.groups_groupid_xdat_user_xdat_user_id = xu.xdat_user_id " +
                                                                       "WHERE xu.login = :username AND tag = :tag " +
                                                                       "ORDER BY groupid";
    private static final String QUERY_GET_GROUPS_FOR_DATATYPE        = "SELECT DISTINCT usergroup.id AS group_name " +
                                                                       "FROM xdat_usergroup usergroup " +
                                                                       "  LEFT JOIN xdat_element_access xea ON usergroup.xdat_usergroup_id = xea.xdat_usergroup_xdat_usergroup_id " +
                                                                       "WHERE " +
                                                                       "  xea.element_name = :dataType " +
                                                                       "ORDER BY group_name";
    private static final String QUERY_ALL_GROUPS                     = "SELECT id FROM xdat_usergroup";
    private static final String QUERY_ALL_TAGS                       = "SELECT DISTINCT tag FROM xdat_usergroup WHERE tag IS NOT NULL AND tag <> ''";
    private static final String QUERY_GET_GROUPS_FOR_TAG             = "SELECT id FROM xdat_usergroup WHERE tag = :tag";
    private static final String QUERY_GET_ALL_MEMBER_GROUPS          = "SELECT " +
                                                                       "  tag AS project_id, " +
                                                                       "  id AS group_id " +
                                                                       "FROM " +
                                                                       "  xdat_usergroup " +
                                                                       "WHERE " +
                                                                       "  tag IS NOT NULL AND " +
                                                                       "  id LIKE '%_member' " +
                                                                       "ORDER BY project_id, group_id";
    private static final String QUERY_GET_ALL_COLLAB_GROUPS          = "SELECT " +
                                                                       "  tag AS project_id, " +
                                                                       "  id AS group_id " +
                                                                       "FROM " +
                                                                       "  xdat_usergroup " +
                                                                       "WHERE " +
                                                                       "  tag IS NOT NULL AND " +
                                                                       "  id LIKE '%_collaborator' " +
                                                                       "ORDER BY project_id, group_id";
    private static final String QUERY_CHECK_USER_EXISTS              = "SELECT EXISTS(SELECT TRUE FROM xdat_user WHERE login = :username) AS exists";
    private static final String QUERY_GET_EXPERIMENT_PROJECT         = "SELECT project FROM xnat_experimentdata WHERE id = :experimentId";
    private static final String QUERY_GET_SUBJECT_PROJECT            = "SELECT project FROM xnat_subjectdata WHERE id = :subjectId OR label = :subjectId";
    private static final String QUERY_GET_USERS_FOR_PROJECTS         = "SELECT DISTINCT login " +
                                                                       "FROM xdat_user u " +
                                                                       "  LEFT JOIN xdat_user_groupid gid ON u.xdat_user_id = gid.groups_groupid_xdat_user_xdat_user_id " +
                                                                       "  LEFT JOIN xdat_usergroup g ON gid.groupid = g.id " +
                                                                       "  LEFT JOIN xdat_element_access xea ON g.xdat_usergroup_id = xea.xdat_usergroup_xdat_usergroup_id " +
                                                                       "  LEFT JOIN xdat_field_mapping_set xfms ON xea.xdat_element_access_id = xfms.permissions_allow_set_xdat_elem_xdat_element_access_id " +
                                                                       "  LEFT JOIN xdat_field_mapping xfm ON xfms.xdat_field_mapping_set_id = xfm.xdat_field_mapping_set_xdat_field_mapping_set_id " +
                                                                       "WHERE tag IN (:projectIds) OR (tag IS NULL AND field_value = '*') " +
                                                                       "ORDER BY login";
    private static final String QUERY_GET_PROJECTS_FOR_USER          = "SELECT DISTINCT " +
                                                                       "  g.tag AS project " +
                                                                       "FROM xdat_usergroup g " +
                                                                       "  LEFT JOIN xdat_user_groupid gid ON g.id = gid.groupid " +
                                                                       "  LEFT JOIN xdat_user u ON gid.groups_groupid_xdat_user_xdat_user_id = u.xdat_user_id " +
                                                                       "WHERE g.tag IS NOT NULL AND " +
                                                                       "      u.login = :username";
    private static final String QUERY_PROJECT_OWNERS                 = "SELECT DISTINCT u.login AS owner " +
                                                                       "FROM xdat_user                     u " +
                                                                       "  LEFT JOIN xdat_user_groupid      map ON u.xdat_user_id = map.groups_groupid_xdat_user_xdat_user_id " +
                                                                       "  LEFT JOIN xdat_usergroup         g ON map.groupid = g.id " +
                                                                       "  LEFT JOIN xdat_element_access    xea ON (g.xdat_usergroup_id = xea.xdat_usergroup_xdat_usergroup_id OR u.xdat_user_id = xea.xdat_user_xdat_user_id) " +
                                                                       "  LEFT JOIN xdat_field_mapping_set xfms ON xea.xdat_element_access_id = xfms.permissions_allow_set_xdat_elem_xdat_element_access_id " +
                                                                       "  LEFT JOIN xdat_field_mapping     xfm ON xfms.xdat_field_mapping_set_id = xfm.xdat_field_mapping_set_xdat_field_mapping_set_id " +
                                                                       "WHERE " +
                                                                       "  xfm.field_value != '*' AND " +
                                                                       "  xea.element_name = 'xnat:projectData' AND " +
                                                                       "  xfm.delete_element = 1 AND " +
                                                                       "  g.id LIKE '%_owner' AND " +
                                                                       "  g.tag = :projectId " +
                                                                       "ORDER BY owner";
    private static final String QUERY_USER_PERMISSIONS               = "SELECT " +
                                                                       "  xea.element_name    AS element_name, " +
                                                                       "  xeamd.status        AS active_status, " +
                                                                       "  xfms.method         AS method, " +
                                                                       "  xfm.field           AS field, " +
                                                                       "  xfm.field_value     AS field_value, " +
                                                                       "  xfm.comparison_type AS comparison_type, " +
                                                                       "  xfm.read_element    AS read_element, " +
                                                                       "  xfm.edit_element    AS edit_element, " +
                                                                       "  xfm.create_element  AS create_element, " +
                                                                       "  xfm.delete_element  AS delete_element, " +
                                                                       "  xfm.active_element  AS active_element " +
                                                                       "FROM xdat_user u " +
                                                                       "  LEFT JOIN xdat_element_access xea ON u.xdat_user_id = xea.xdat_user_xdat_user_id " +
                                                                       "  LEFT JOIN xdat_element_access_meta_data xeamd ON xea.element_access_info = xeamd.meta_data_id " +
                                                                       "  LEFT JOIN xdat_field_mapping_set xfms ON xea.xdat_element_access_id = xfms.permissions_allow_set_xdat_elem_xdat_element_access_id " +
                                                                       "  LEFT JOIN xdat_field_mapping xfm ON xfms.xdat_field_mapping_set_id = xfm.xdat_field_mapping_set_xdat_field_mapping_set_id " +
                                                                       "WHERE " +
                                                                       "  u.login = :username";
    private static final String QUERY_ACCESSIBLE_DATA_PROJECTS       = "SELECT  " +
                                                                       "  project  " +
                                                                       "FROM  " +
                                                                       "  (SELECT DISTINCT f.field_value AS project  " +
                                                                       "   FROM  " +
                                                                       "     xdat_user u  " +
                                                                       "     LEFT JOIN xdat_user_groupid map ON u.xdat_user_id = map.groups_groupid_xdat_user_xdat_user_id  " +
                                                                       "     LEFT JOIN xdat_usergroup g ON map.groupid = g.id  " +
                                                                       "     LEFT JOIN xdat_element_access a ON (g.xdat_usergroup_id = a.xdat_usergroup_xdat_usergroup_id OR u.xdat_user_id = a.xdat_user_xdat_user_id)  " +
                                                                       "     LEFT JOIN xdat_field_mapping_set s ON a.xdat_element_access_id = s.permissions_allow_set_xdat_elem_xdat_element_access_id  " +
                                                                       "     LEFT JOIN xdat_field_mapping f ON s.xdat_field_mapping_set_id = f.xdat_field_mapping_set_xdat_field_mapping_set_id  " +
                                                                       "   WHERE  " +
                                                                       "     f.field_value != '*' AND  " +
                                                                       "     a.element_name = 'xnat:subjectData' AND  " +
                                                                       "     f.%s = 1 AND  " +
                                                                       "     u.login IN ('guest', :username)) projects";
    private static final String QUERY_OWNED_PROJECTS                 = String.format(QUERY_ACCESSIBLE_DATA_PROJECTS, "delete_element");
    private static final String QUERY_EDITABLE_PROJECTS              = String.format(QUERY_ACCESSIBLE_DATA_PROJECTS, "edit_element");
    private static final String QUERY_READABLE_PROJECTS              = String.format(QUERY_ACCESSIBLE_DATA_PROJECTS, "read_element");
    private static final String QUERY_HAS_ALL_DATA_PRIVILEGES        = "SELECT  " +
                                                                       "  EXISTS(SELECT TRUE  " +
                                                                       "         FROM  " +
                                                                       "           xdat_user u  " +
                                                                       "           LEFT JOIN xdat_user_groupid map ON u.xdat_user_id = map.groups_groupid_xdat_user_xdat_user_id  " +
                                                                       "           LEFT JOIN xdat_usergroup ug ON map.groupid = ug.id  " +
                                                                       "           LEFT JOIN xdat_element_access ea ON (ug.xdat_usergroup_id = ea.xdat_usergroup_xdat_usergroup_id OR u.xdat_user_id = ea.xdat_user_xdat_user_id)  " +
                                                                       "           LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id = fms.permissions_allow_set_xdat_elem_xdat_element_access_id  " +
                                                                       "           LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id = fm.xdat_field_mapping_set_xdat_field_mapping_set_id  " +
                                                                       "         WHERE  " +
                                                                       "           fm.field_value = '*' AND  " +
                                                                       "           ea.element_name = 'xnat:projectData' AND  " +
                                                                       "           fm.%s = 1 AND  " +
                                                                       "           u.login = :username)";
    private static final String QUERY_HAS_ALL_DATA_ACCESS            = String.format(QUERY_HAS_ALL_DATA_PRIVILEGES, "read_element");
    private static final String QUERY_HAS_ALL_DATA_ADMIN             = String.format(QUERY_HAS_ALL_DATA_PRIVILEGES, "edit_element");
    private static final String QUERY_ALL_DATA_ACCESS_PROJECTS       = "SELECT id AS project FROM xnat_projectdata ORDER BY project";
    private static final String QUERY_USER_READABLE_WORKFLOW_COUNT   = "SELECT COUNT(*) FROM wrk_workflowData wrk_workflowData";
    private static final String QUERY_USER_READABLE_SUBJECT_COUNT    = "SELECT  " +
                                                                       "  COUNT(*) " +
                                                                       "FROM  " +
                                                                       "  (SELECT SEARCH.*  " +
                                                                       "   FROM  " +
                                                                       "     (SELECT DISTINCT ON (subjectId) *  " +
                                                                       "      FROM  " +
                                                                       "        (SELECT xnat_subjectData.id AS subjectId, xnat_subjectData.project AS subjectProject, sharedSubjects.project AS sharedProject  " +
                                                                       "         FROM  " +
                                                                       "           xnat_subjectData xnat_subjectData  " +
                                                                       "           LEFT JOIN xnat_projectParticipant sharedSubjects ON xnat_subjectData.id = sharedSubjects.subject_id) SECURITY  " +
                                                                       "      WHERE  " +
                                                                       "        subjectProject IN (:projectIds) OR  " +
                                                                       "        sharedProject IN (:projectIds)) SECURITY  " +
                                                                       "     LEFT JOIN xnat_subjectData SEARCH ON SECURITY.subjectId = SEARCH.id) xnat_subjectData";
    private static final String QUERY_USER_READABLE_EXPERIMENT_COUNT = "SELECT " +
                                                                       "  element_name, " +
                                                                       "  COUNT(*) AS element_count " +
                                                                       "FROM " +
                                                                       "  (SELECT xnat_experimentData.id AS id " +
                                                                       "   FROM " +
                                                                       "     (SELECT SEARCH.* " +
                                                                       "      FROM " +
                                                                       "        (SELECT DISTINCT ON (id) * " +
                                                                       "         FROM " +
                                                                       "           (SELECT xnat_experimentData.id AS id, xnat_experimentData.project AS experimentProject, sharedExperiments.project AS experimentSharedProject " +
                                                                       "            FROM " +
                                                                       "              xnat_experimentData xnat_experimentData " +
                                                                       "              LEFT JOIN xnat_experimentData_share sharedExperiments ON xnat_experimentData.id = sharedExperiments.sharing_share_xnat_experimentda_id) SECURITY " +
                                                                       "         WHERE " +
                                                                       "           experimentProject IN (:projectIds) OR " +
                                                                       "           experimentSharedProject IN (:projectIds)) SECURITY " +
                                                                       "        LEFT JOIN xnat_experimentData SEARCH ON SECURITY.id = SEARCH.id) xnat_experimentData) SEARCH " +
                                                                       "  LEFT JOIN xnat_experimentData expt ON search.id = expt.id " +
                                                                       "  LEFT JOIN xdat_meta_element xme ON expt.extension = xme.xdat_meta_element_id " +
                                                                       "GROUP BY " +
                                                                       "  element_name";
    private static final String GUEST_USERNAME                       = "guest";
    private static final String ACTIONS_PREFIX                       = "actions";
    private static final String TAG_PREFIX                           = "tag";
    private static final String PROJECT_PREFIX                       = "project";
    private static final String USER_ELEMENT_PREFIX                  = "user";
    private static final String ELEMENT_ACCESS_MANAGERS_PREFIX       = "eam";
    private static final String GROUPS_ELEMENT_PREFIX                = "groups";
    private static final String GUEST_ACTION_READ                    = getCacheIdForActionElements(GUEST_USERNAME);

    private static final Pattern REGEX_EXTRACT_USER_FROM_CACHE_ID   = Pattern.compile("^(?<prefix>" + ACTIONS_PREFIX + "|" + USER_ELEMENT_PREFIX + "):(?<username>[^:]+)(?<remainder>:.*)?$");
    private static final Pattern REGEX_USER_READABLE_COUNTS         = Pattern.compile("^(?<prefix>" + USER_ELEMENT_PREFIX + "):(?<username>[^:]+):" + READABLE + "$");
    private static final Pattern REGEX_USER_PROJECT_ACCESS_CACHE_ID = Pattern.compile("^" + USER_ELEMENT_PREFIX + ":(?<username>[A-z0-9_]+):" + XnatProjectdata.SCHEMA_ELEMENT_NAME + ":(?<access>[A-z]+)$");
    private static final String  GUEST_CACHE_ID                     = getCacheIdForUserElements(GUEST_USERNAME, BROWSEABLE);

    private static final Function<ElementDisplay, String>  FUNCTION_ELEMENT_DISPLAY_TO_STRING  = new Function<ElementDisplay, String>() {
        @Override
        public String apply(final ElementDisplay elementDisplay) {
            return elementDisplay.getElementName();
        }
    };
    private static final Function<ElementSecurity, String> FUNCTION_ELEMENT_SECURITY_TO_STRING = new Function<ElementSecurity, String>() {
        @Override
        public String apply(final ElementSecurity security) {
            try {
                return security.getElementName();
            } catch (XFTInitException | ElementNotFoundException | FieldNotFoundException e) {
                log.error("Got an error trying to get an element security object name", e);
                return "";
            }
        }
    };
    private static final Function<String, String>          FUNCTION_CACHE_IDS_TO_USERNAMES     = new Function<String, String>() {
        @Nullable
        @Override
        public String apply(@Nullable final String cacheId) {
            return getUsernameFromCacheId(cacheId);
        }
    };
    private static final Predicate<ElementDisplay>         CONTAINS_MR_SESSION                 = new Predicate<ElementDisplay>() {
        @Override
        public boolean apply(final ElementDisplay display) {
            return StringUtils.equalsIgnoreCase(XnatMrsessiondata.SCHEMA_ELEMENT_NAME, display.getElementName());
        }
    };

    private final NamedParameterJdbcTemplate _template;
    private final JmsTemplate                _jmsTemplate;
    private final DatabaseHelper             _helper;
    private final Map<String, Long>          _totalCounts;
    private final Map<String, Long>          _missingElements;
    private final Map<String, Boolean>       _userChecks;
    private final AtomicBoolean              _initialized;

    private Listener _listener;
    private XDATUser _guest;
}
