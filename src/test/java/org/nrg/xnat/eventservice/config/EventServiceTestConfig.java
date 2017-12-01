package org.nrg.xnat.eventservice.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.nrg.framework.services.ContextService;
import org.nrg.xnat.eventservice.actions.EventServiceLoggingAction;
import org.nrg.xnat.eventservice.actions.TestAction;
import org.nrg.xnat.eventservice.daos.EventSubscriptionEntityDao;
import org.nrg.xnat.eventservice.entities.EventServiceFilterEntity;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.events.TestCombinedEvent;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.listeners.TestListener;
import org.nrg.xnat.eventservice.services.*;
import org.nrg.xnat.eventservice.services.impl.ActionManagerImpl;
import org.nrg.xnat.eventservice.services.impl.EventServiceComponentManagerImpl;
import org.nrg.xnat.eventservice.services.impl.EventServiceImpl;
import org.nrg.xnat.eventservice.services.impl.EventSubscriptionEntityServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.support.ResourceTransactionManager;
import reactor.bus.EventBus;

import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;

@Configuration
@Import({HibernateConfig.class, ObjectMapperConfig.class})
public class EventServiceTestConfig {


    @Bean
    public EventService eventService(final EventSubscriptionEntityService subscriptionService,
                                     final EventBus eventBus,
                                     final ContextService contextService,
                                     final EventServiceComponentManager componentManager,
                                     final ActionManager actionManager){
        return new EventServiceImpl(subscriptionService, eventBus, contextService, componentManager, actionManager);
    }

    @Bean
    public EventService mockEventService(final EventSubscriptionEntityService subscriptionService,
                                     final EventBus eventBus,
                                     final ContextService contextService,
                                     final EventServiceComponentManager mockComponentManager,
                                     final ActionManager mockActionManager){
        return new EventServiceImpl(subscriptionService, eventBus, contextService, mockComponentManager, mockActionManager);
    }

    @Bean
    public EventSubscriptionEntityService eventSubscriptionService(final @Lazy EventService eventService,
                                                                   final ObjectMapper objectMapper,
                                                                   final EventBus eventBus,
                                                                   final ContextService contextService,
                                                                   final ActionManager actionManager,
                                                                   final EventServiceComponentManager componentManager) {
        return new EventSubscriptionEntityServiceImpl(eventBus, contextService, actionManager, componentManager, eventService, objectMapper);
    }

    @Bean
    public TestListener testListener() {return new TestListener(); }

    @Bean
    public TestCombinedEvent testCombinedEvent() {return new TestCombinedEvent(); }

    @Bean
    public ContextService contextService(final ApplicationContext applicationContext) {
        final ContextService contextService = new ContextService();
        contextService.setApplicationContext(applicationContext);
        return contextService;
    }

    @Bean
    public EventServiceActionProvider testAction() {return new TestAction(); }

    @Bean
    public EventServiceActionProvider eventServiceLoggingAction() {return new EventServiceLoggingAction(); }

    @Bean
    public EventServiceLoggingAction mockEventServiceLoggingAction() { return Mockito.mock(EventServiceLoggingAction.class); }

    @Bean
    public ActionManager actionManager(EventServiceComponentManager componentManager) {
        return new ActionManagerImpl(componentManager);
    }

    @Bean
    public ActionManager mockActionManager(EventServiceComponentManager mockComponentManager) {
        return new ActionManagerImpl(mockComponentManager);
    }

    @Bean
    public List<EventServiceActionProvider> actionProviders () {return null;}

    @Bean
    public EventBus eventBus(ContextService contextService) {return EventBus.create(); }

    @Bean
    public EventSubscriptionEntityDao eventSubscriptionDao() {
        return new EventSubscriptionEntityDao();
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(final DataSource dataSource, @Qualifier("hibernateProperties") final Properties properties) {
        final LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setHibernateProperties(properties);
        bean.setAnnotatedClasses(
                SubscriptionEntity.class,
                Boolean.class,
                EventServiceFilterEntity.class);
        return bean;
    }

    @Bean
    public ResourceTransactionManager transactionManager(final SessionFactory sessionFactory) throws Exception {
        return new HibernateTransactionManager(sessionFactory);
    }

    @Bean
    public EventServiceComponentManager mockComponentManager() { return Mockito.mock(EventServiceComponentManager.class); }

    @Bean
    public EventServiceComponentManager componentManager(final List<EventServiceListener> eventListeners,
                                                         final List<EventServiceActionProvider> actionProviders) {
        return new EventServiceComponentManagerImpl(eventListeners, actionProviders);
    }


}
