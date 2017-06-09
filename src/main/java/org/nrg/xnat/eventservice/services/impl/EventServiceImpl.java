package org.nrg.xnat.eventservice.services.impl;

import com.google.common.collect.Lists;
import javassist.Modifier;
import org.nrg.framework.event.XnatEventServiceAction;
import org.nrg.framework.event.XnatEventServiceListener;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.services.ContextService;
import org.nrg.framework.utilities.BasicXnatResourceLocator;
import org.nrg.xnat.eventservice.actions.EventServiceActionI;
import org.nrg.xnat.eventservice.events.AbstractLifecycleEvent;
import org.nrg.xnat.eventservice.listeners.EventServiceListenerI;
import org.nrg.xnat.eventservice.model.EventServiceAction;
import org.nrg.xnat.eventservice.model.EventServiceEvent;
import org.nrg.xnat.eventservice.model.EventServiceListener;
import org.nrg.xnat.eventservice.model.EventSubscription;
import org.nrg.xnat.eventservice.services.EventService;
import org.nrg.xnat.eventservice.services.EventSubscriptionEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.registry.Registration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static reactor.bus.selector.Selectors.regex;

@Service
public class EventServiceImpl implements EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private static final String EVENT_RESOURCE_PATH ="classpath*:META-INF/xnat/event/*-xnateventserviceevent.properties";
    private static final String LISTENER_RESOURCE_PATH ="classpath*:META-INF/xnat/event/*-xnateventservicelistener.properties";
    private static final String ACTION_RESOURCE_PATH ="classpath*:META-INF/xnat/event/*-xnateventserviceaction.properties";

    private ContextService contextService;
    private EventSubscriptionEntityService subscriptionService;
    private EventBus eventBus;
    private List<EventServiceEvent> installedEvents;

    @Autowired
    public EventServiceImpl(final EventSubscriptionEntityService subscriptionService,
                            final EventBus eventBus, final ContextService contextService) {
        this.subscriptionService = subscriptionService;
        this.eventBus = eventBus;
        this.contextService = contextService;
        this.installedEvents = new ArrayList<>();
    }

    @Override
    @Deprecated
    //TODO: Use new subscription model
    public EventSubscription createSubscription(EventSubscription subscription) {
        log.info("Creating new subscription: " + subscription.name());

        try {
            Registration registrationKey = eventBus.on(regex("ProjectId:" + subscription.projectId()), null);
            log.info("Created registrationKey: " + registrationKey.hashCode());
            return subscription.withSubscriptionKey(registrationKey.hashCode());
        }
        catch (Exception e) {
            log.debug("Event subscription failed for " + subscription.consumerType());
        }
        return null;
    }

    @Override
    public void updateSubscription(EventSubscription subscription) throws SubscriptionValidationException, NotFoundException {
        subscriptionService.update(subscription);
    }

    @Override
    public List<EventSubscription> getSubscriptions() {
        return subscriptionService.getAllSubscriptions();
    }

    @Override
    public EventSubscription getSubscription(Long id) throws NotFoundException {
        return subscriptionService.getSubscription(id);
    }

    @Override
    public List<Class<?>> getClassList(Class<?> parentClass, String resourcePath, String propertyKey) {
        final List<Class<?>> classList = Lists.newArrayList();
        try {
            for (final Resource resource : BasicXnatResourceLocator.getResources(resourcePath)) {
                final Properties properties = PropertiesLoaderUtils.loadProperties(resource);
                if (!properties.containsKey(propertyKey)) {
                    continue;
                }
                final String classStr = properties.get(propertyKey).toString();
                try {
                    final Class<?> clazz = Class.forName(classStr);
                    if (parentClass.isAssignableFrom(clazz) && !clazz.isInterface() &&
                            !Modifier.isAbstract(clazz.getModifiers())) {
                        if (!classList.contains(clazz)) {
                            classList.add(clazz);
                        }
                    }
                } catch (ClassNotFoundException cex) {
                    log.debug("Could not load class for class name (" + classStr + ")");
                }
            }
        } catch (IOException e) {
            log.debug("Could not load event class properties resources (" + resourcePath + ")");
        }

        return classList;
    }


    @Override
    public List<EventServiceAction> getInstalledActions() {
        List<EventServiceAction> actions = new ArrayList<>();
        try {
            for (Class<?> clazz : this.getClassList(EventServiceActionI.class, ACTION_RESOURCE_PATH, XnatEventServiceAction.ACTION_CLASS)) {
                if (EventServiceActionI.class.isAssignableFrom(clazz)) {
                    try {
                        EventServiceActionI action = (EventServiceActionI)clazz.getConstructor().newInstance();
                        //EventServiceActionI action = (EventServiceActionI) applicationContext.getBean(clazz);
                        actions.add(toPojo(action));
                    } catch (Exception e) {
                        log.debug("Error reading installed actions into displayable Actions.\n" + clazz.getCanonicalName());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error reading installed actions into displayable Actions.");
        }
        return actions;
    }

    @Override
    public List<EventServiceEvent> getInstalledEvents() {
        List<EventServiceEvent> events = new ArrayList<>();
        try {
            for (final Resource resource : BasicXnatResourceLocator.getResources(EVENT_RESOURCE_PATH)) {
                events.add(toPojo(AbstractLifecycleEvent.createFromResource(resource)));
            }
        } catch (Exception e) {
            log.debug("Error reading installed events into displayable Events.");
            log.debug(e.getMessage());
        }
        return events;
    }

    @Override
    public List<EventServiceListener> getInstalledListeners() {

        List<EventServiceListener> listeners = new ArrayList<>();
        try {
            for (Class<?> clazz : this.getClassList(EventServiceListenerI.class, LISTENER_RESOURCE_PATH, XnatEventServiceListener.LISTENER_CLASS)) {
                if (EventServiceListenerI.class.isAssignableFrom(clazz)) {
                    try {
                        EventServiceListenerI listener = (EventServiceListenerI) contextService.getBean(clazz);
                        listeners.add(toPojo(listener));
                    } catch (Exception e) {
                        log.debug("Error reading installed listeners into displayable Listeners.\n" + clazz.getCanonicalName());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error reading installed listeners into displayable Listeners.");
        }
        return listeners;
    }

    private EventServiceEvent toPojo(AbstractLifecycleEvent event) {
            return EventServiceEvent.create(event.getId() == null ? 0 : event.getId(), event.getClassName(), event.getDisplayName(), event.getDescription(),
                    event.getEventObject(), event.getEventOperation());
    }
    private EventServiceAction toPojo(EventServiceActionI action) {
        return EventServiceAction.builder()
                .displayName(action.getDisplayName())
                .description(action.getDescription())
                .isEnabled(action.getEnabled())
                .className(action.getClass().getName()).build();
    }
    private EventServiceListener toPojo(EventServiceListenerI listener) {
        return null;
    }

    @Override
    public void processEvent(Event<?> event) {
        log.debug("SessionArchiveEvent noticed by EventService: " + event.toString());

        // Lookup subscription, filter, and notify action(s)
    }

}
