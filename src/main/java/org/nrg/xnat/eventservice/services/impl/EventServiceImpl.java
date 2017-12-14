package org.nrg.xnat.eventservice.services.impl;

import com.google.common.collect.Lists;
import javassist.Modifier;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.services.ContextService;
import org.nrg.framework.utilities.BasicXnatResourceLocator;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.exceptions.SubscriptionValidationException;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.model.*;
import org.nrg.xnat.eventservice.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class EventServiceImpl implements EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private ContextService contextService;
    private EventSubscriptionEntityService subscriptionService;
    private EventBus eventBus;
    private EventServiceComponentManager componentManager;
    private ActionManager actionManager;

    @Autowired
    public EventServiceImpl(final EventSubscriptionEntityService subscriptionService,
                            final EventBus eventBus,
                            final ContextService contextService,
                            final EventServiceComponentManager componentManager,
                            final ActionManager actionManager) {
        this.subscriptionService = subscriptionService;
        this.eventBus = eventBus;
        this.contextService = contextService;
        this.componentManager = componentManager;
        this.actionManager = actionManager;

    }

    @Override
    public Subscription createSubscription(Subscription subscription) throws SubscriptionValidationException {

        return subscriptionService.createSubscription(subscription);
    }

    @Override
    public Subscription updateSubscription(Subscription subscription) throws SubscriptionValidationException, NotFoundException {
        return subscriptionService.update(subscription);
    }

    @Override
    public void deleteSubscription(Long id) throws Exception {
        subscriptionService.delete(id);
    }

    @Override
    public List<Subscription> getSubscriptions() {
        return subscriptionService.getAllSubscriptions();
    }

    @Override
    public Subscription getSubscription(Long id) throws NotFoundException {
        return subscriptionService.getSubscription(id);
    }

    @Override
    public Subscription validateSubscription(Subscription subscription) throws SubscriptionValidationException {
        return null;
    }

    @Deprecated
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
    public List<ActionProvider> getActionProviders() {
        List<ActionProvider> providers = new ArrayList<>();
        for(EventServiceActionProvider ap : componentManager.getActionProviders()) {
            providers.add(toPojo(ap));
        }
        return providers;
    }

    @Override
    public List<ActionProvider> getActionProviders(String xsiType, String projectId) {
        List<ActionProvider> providers = new ArrayList<>();
        for(EventServiceActionProvider ap : componentManager.getActionProviders()) {
            providers.add(toPojo(ap));
        }
        return providers;
    }


    @Override
    public List<Action> getAllActions(UserI user) {
        return actionManager.getActions(user);
    }

    @Override
    public List<Action> getAllActions(String xnatType, UserI user) {
        return actionManager.getActions(xnatType, user);
    }

    @Override
    public List<Action> getAllActions(String projectId, String xnatType, UserI user) {
        return actionManager.getActions(projectId, xnatType, user);
    }

    @Override
    public List<Action> getActionsByProvider(String actionProvider, UserI user) {
        return actionManager.getActionsByProvider(actionProvider, user);
    }

    @Override
    public Action getActionByKey(String actionKey, UserI user) {
        return actionManager.getActionByKey(actionKey, user);
    }

    @Override
    public List<SimpleEvent> getEvents() throws Exception {
        List<SimpleEvent> events = new ArrayList();
        for(EventServiceEvent e : componentManager.getInstalledEvents()){
            events.add(toPojo(e));
        }
        return events;
    }

    @Override
    public List<Listener> getInstalledListeners() {

        return null;
    }

    @Override
    public void reactivateAllSubscriptions() {
        subscriptionService.reregisterAllActive();
    }


    @Override
    public void processEvent(EventServiceListener listener, Event event) {
        try {
            log.debug("Event noticed by EventService: " + event.getClass().getSimpleName());
            subscriptionService.processEvent(listener, event);
        } catch (NotFoundException e) {
            log.error("Failed to processEvent with subscription service.\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private SimpleEvent toPojo(@Nonnull EventServiceEvent event) {
        return SimpleEvent.builder()
                .id(event.getId() == null ? "" : event.getId())
                .listenerService(
                        event instanceof EventServiceListener
                        ? ((EventServiceListener) event).getClass().getName()
                        : "")
                .displayName(event.getDisplayName() == null ? "" : event.getDisplayName())
                .description(event.getDescription() == null ? "" : event.getDescription())
                .payloadClass(event.getObjectClass() == null ? "" : event.getObjectClass())
                .xnatType(event.getPayloadXnatType() == null ? "" : event.getPayloadXnatType())
                .isXsiType(event.isPayloadXsiType() == null ? false : event.isPayloadXsiType())
                .build();
    }

    private ActionProvider toPojo(@Nonnull EventServiceActionProvider actionProvider) {
        return ActionProvider.builder()
                .className(actionProvider.getName())
                .displayName((actionProvider.getDisplayName()))
                .description(actionProvider.getDescription())
                .actions(actionProvider.getActions(null))
                .events(actionProvider.getEvents())
                .build();
    }


}
