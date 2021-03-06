/*
 * web: org.nrg.xnat.security.XnatAuthenticationFilter
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.nrg.xdat.exceptions.UsernameAuthMappingNotFoundException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.turbine.utils.AccessLogger;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.nrg.xnat.utils.XnatHttpUtils.getCredentials;

@Slf4j
public class XnatAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    @Autowired
    public void setAuthenticationManager(final AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

    @Autowired
    public void setXnatProviderManager(final XnatProviderManager providerManager) {
        _providerManager = providerManager;
    }

    @Autowired
    @Override
    public void setAuthenticationSuccessHandler(final AuthenticationSuccessHandler handler) {
        super.setAuthenticationSuccessHandler(handler);
    }

    @Autowired
    @Override
    public void setAuthenticationFailureHandler(final AuthenticationFailureHandler handler) {
        super.setAuthenticationFailureHandler(handler);
    }

    @Autowired
    @Override
    public void setSessionAuthenticationStrategy(final SessionAuthenticationStrategy strategy) {
        super.setSessionAuthenticationStrategy(strategy);
    }

    @Override
    public Authentication attemptAuthentication(final HttpServletRequest request, final HttpServletResponse response) throws AuthenticationException {
        final Pair<String, String> credentials;
        try {
            credentials = getCredentials(request);
        } catch (ParseException e) {
            // This means that the basic authentication header wasn't properly formatted, so we can't find credentials.
            throw new AuthenticationCredentialsNotFoundException(e.getMessage());
        }

        final String username     = credentials.getLeft();
        final String password     = credentials.getRight();
        final String providerName = request.getParameter("login_method");

        final UsernamePasswordAuthenticationToken authRequest;

        if (StringUtils.isBlank(providerName) && StringUtils.isNotBlank(username)) {
            // Try to guess the auth method
            final String authMethod = _providerManager.retrieveAuthMethod(username);
            if (StringUtils.isEmpty(authMethod)) {
                throw new BadCredentialsException("Missing login_method parameter.");
            } else {
                authRequest = _providerManager.buildUPTokenForAuthMethod(authMethod, username, password);
            }
        } else {
            authRequest = _providerManager.buildUPTokenForProviderName(providerName, username, password);
        }

        setDetails(request, authRequest);

        try {
            AccessLogger.LogServiceAccess(username, AccessLogger.GetRequestIp(request), "Authentication", "SUCCESS");
            final Authentication authentication;
            try {
                authentication = getAuthenticationManager().authenticate(authRequest);
            } catch (UsernameAuthMappingNotFoundException e) {
                log.info("User {} attempted to log using {} provider {}, creating new user auth and diverting to account merge page.");
                request.getSession().setAttribute(UsernameAuthMappingNotFoundException.class.getSimpleName(), e);
                response.sendRedirect(TurbineUtils.GetFullServerPath() + "/app/template/RegisterExternalLogin.vm");
                return null;
            }

            //Fixed XNAT-4409 by adding a check for a par parameter on login. If a PAR is present and valid, then grant the user that just logged in the appropriate project permissions.
            if(StringUtils.isNotBlank(request.getParameter("par"))){
                final String parId = request.getParameter("par");
                request.getSession().setAttribute("par", parId);

                final ProjectAccessRequest par = ProjectAccessRequest.RequestPARByGUID(parId, null);
                if (par.getApproved() != null || par.getApprovalDate() != null) {
                    log.warn("User {} tried to access a PAR that is not approved or has already been accepted: {}", username, par.getGuid());
                } else {
                    final XDATUser user = new XDATUser(username);
                    par.process(user, true, EventUtils.TYPE.WEB_FORM, "", "");
                }
            }

            return authentication;
        } catch (AuthenticationException e) {
            logFailedAttempt(username, request);
            throw e;
        } catch (UserNotFoundException e) {
            log.error("Couldn't find a user with the name '" + username + "'", e);
        } catch (UserInitException e) {
            log.error("An error occurred trying to initialize the user with the name '" + username + "'", e);
        } catch (Exception e) {
            log.error("An unknown error occurred while trying to authenticate the user with the name '" + username + "'", e);
        }
        return null;
    }

    public static void logFailedAttempt(final String username, final HttpServletRequest request) {
        if (!StringUtils.isBlank(username)) {
            final Integer uid = retrieveUserId(username);
            if (uid != null) {
                try {
                    final XFTItem item = XFTItem.NewItem("xdat:user_login", null);
                    item.setProperty("xdat:user_login.user_xdat_user_id", uid);
                    item.setProperty("xdat:user_login.ip_address", AccessLogger.GetRequestIp(request));
                    item.setProperty("xdat:user_login.login_date", Calendar.getInstance(java.util.TimeZone.getDefault()).getTime());
                    SaveItemHelper.authorizedSave(item, null, true, false, (EventMetaI) null);
                } catch (Exception exception) {
                    log.error("An exception occurred trying to log a failed login attempt for the user " + username, exception);
                }
            }
            AccessLogger.LogServiceAccess(username, AccessLogger.GetRequestIp(request), "Authentication", "FAILED");
        }
    }

    public static Integer retrieveUserId(final String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }

        if (checked.containsKey(username)) {
            return checked.get(username);
        }

        synchronized (checked) {
            final Integer userId = Users.getUserId(username);
            if (userId == null) {
                return null;
            }

            checked.put(username, userId);
            return userId;
        }
    }

    private static final Map<String, Integer> checked = new ConcurrentHashMap<>();

    private XnatProviderManager _providerManager;
}
