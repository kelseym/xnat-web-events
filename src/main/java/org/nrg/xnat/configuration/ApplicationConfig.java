/*
 * web: org.nrg.xnat.configuration.ApplicationConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.configuration;

import lombok.extern.slf4j.Slf4j;
import org.nrg.config.services.ConfigService;
import org.nrg.framework.configuration.ConfigPaths;
import org.nrg.framework.services.NrgEventService;
import org.nrg.framework.services.SerializerService;
import org.nrg.framework.utilities.OrderedProperties;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.xdat.preferences.NotificationsPreferences;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.XDATUserMgmtServiceImpl;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.ThemeService;
import org.nrg.xdat.services.impl.ThemeServiceImpl;
import org.nrg.xnat.initialization.InitializingTask;
import org.nrg.xnat.initialization.InitializingTasksExecutor;
import org.nrg.xnat.preferences.AutomationPreferences;
import org.nrg.xnat.preferences.PluginOpenUrlsPreference;
import org.nrg.xnat.restlet.XnatRestletExtensions;
import org.nrg.xnat.restlet.XnatRestletExtensionsBean;
import org.nrg.xnat.restlet.actions.importer.ImporterHandlerPackages;
import org.nrg.xnat.processor.importer.ProcessorImporterHandlerA;
import org.nrg.xnat.processor.importer.ProcessorImporterMap;
import org.nrg.xnat.services.PETTracerUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.apache.commons.configuration.ConfigurationException;

import java.io.IOException;

import javax.servlet.ServletContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Configuration
@ComponentScan({"org.nrg.automation.daos", "org.nrg.automation.repositories", "org.nrg.config.daos", "org.nrg.dcm.xnat",
                "org.nrg.dicomtools.filters", "org.nrg.framework.datacache.impl.hibernate",
                "org.nrg.framework.services.impl", "org.nrg.notify.daos", "org.nrg.prefs.repositories",
                "org.nrg.xdat.daos", "org.nrg.xdat.security.validators", "org.nrg.xdat.services.impl.hibernate",
                "org.nrg.xft.daos", "org.nrg.xft.event.listeners", "org.nrg.xft.services",
                "org.nrg.xnat.configuration", "org.nrg.xnat.daos", "org.nrg.xnat.event.listeners",
                "org.nrg.xnat.helpers.merge", "org.nrg.xnat.initialization.tasks",
                "org.nrg.xnat.node", "org.nrg.xnat.task", "org.nrg.xnat.processors",
                "org.nrg.xnat.processor.services.impl", "org.nrg.xnat.processor.dao", "org.nrg.xnat.processor.importer"})
@Import({FeaturesConfig.class, ReactorConfig.class})
@ImportResource("WEB-INF/conf/mq-context.xml")
@EnableCaching
@Slf4j
public class ApplicationConfig {
    @Bean
    public ThemeService themeService(final SerializerService serializer, final ServletContext context) {
        return new ThemeServiceImpl(serializer, context);
    }

    @Bean
    public CacheManager cacheManager() {
        return new EhCacheCacheManager(ehCacheManagerFactory().getObject());
    }

    @Bean
    public EhCacheManagerFactoryBean ehCacheManagerFactory() {
        return new EhCacheManagerFactoryBean() {{
            setConfigLocation(new ClassPathResource("xnat-cache.xml"));
        }};
    }

    @Bean
    public InitializingTasksExecutor initializingTasksExecutor(final TaskScheduler scheduler, final List<InitializingTask> tasks) {
        log.debug("Creating InitializingTasksExecutor bean with a scheduler of type {} and {} tasks", scheduler.getClass().getName(), tasks.size());
        return new InitializingTasksExecutor(scheduler, tasks);
    }

    @Bean(name = {"siteConfigPreferences", "siteConfig"})
    public SiteConfigPreferences siteConfigPreferences(final NrgPreferenceService preferenceService, final NrgEventService eventService, final ConfigPaths configFolderPaths, final OrderedProperties initPrefs) {
        return new SiteConfigPreferences(preferenceService, eventService, configFolderPaths, initPrefs);
    }

    @Bean
    public NotificationsPreferences notificationsPreferences(final NrgPreferenceService preferenceService, final NrgEventService eventService, final ConfigPaths configFolderPaths, final OrderedProperties initPrefs) {
        return new NotificationsPreferences(preferenceService, eventService, configFolderPaths, initPrefs);
    }

    @Bean
    public AutomationPreferences automationPreferences(final NrgPreferenceService preferenceService, final NrgEventService service, final ConfigPaths configFolderPaths, final OrderedProperties initPrefs) {
        return new AutomationPreferences(preferenceService, service, configFolderPaths, initPrefs);
    }

    @Bean
    public PluginOpenUrlsPreference pluginOpenUrlsPreference(final NrgPreferenceService preferenceService) {
        return new PluginOpenUrlsPreference(preferenceService);
    }

    @Bean
    public PETTracerUtils petTracerUtils(final ConfigService configService) {
        return new PETTracerUtils(configService);
    }

    @Bean
    public UserManagementServiceI userManagementService(final NamedParameterJdbcTemplate template) {
        // TODO: This should be made to use a preference setting.
        return new XDATUserMgmtServiceImpl(template);
    }

    // MIGRATION: I'm not even sure this is used, but we need to do away with it in favor of prefs.
    @Bean
    public List<String> propertiesRepositories() {
        return Collections.singletonList("WEB-INF/conf/properties");
    }

    @Bean
    public XnatUserProvider primaryAdminUserProvider(final SiteConfigPreferences preferences) {
        return new XnatUserProvider(preferences, "primaryAdminUsername");
    }

    @Bean
    public XnatUserProvider receivedFileUserProvider(final SiteConfigPreferences preferences) {
        return new XnatUserProvider(preferences, "receivedFileUser");
    }

    @Bean
    public XnatRestletExtensionsBean xnatRestletExtensionsBean(final List<XnatRestletExtensions> extensions) {
        return new XnatRestletExtensionsBean(extensions);
    }

    @Bean
    public XnatRestletExtensions defaultXnatRestletExtensions() {
        return new XnatRestletExtensions(new HashSet<>(Collections.singletonList("org.nrg.xnat.restlet.extensions")));
    }

    @Bean
    public XnatRestletExtensions extraXnatRestletExtensions() {
        return new XnatRestletExtensions(new HashSet<>(Collections.singletonList("org.nrg.xnat.restlet.actions")));
    }

    @Bean
    public ImporterHandlerPackages importerHandlerPackages() {
        return new ImporterHandlerPackages(new HashSet<>(Arrays.asList("org.nrg.xnat.restlet.actions", "org.nrg.xnat.archive")));
    }

    @Bean
    public ProcessorImporterMap processorImporterMap(final List<ProcessorImporterHandlerA> handlers) throws ConfigurationException, IOException, ClassNotFoundException {
        return new ProcessorImporterMap(new HashSet<>(Arrays.asList("org.nrg.xnat.processor.importer")), handlers);
    }
}
