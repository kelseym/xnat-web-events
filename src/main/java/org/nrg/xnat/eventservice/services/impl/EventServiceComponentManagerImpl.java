package org.nrg.xnat.eventservice.services.impl;

import org.nrg.framework.utilities.BasicXnatResourceLocator;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.events.CombinedEventServiceEvent;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.services.EventService;
import org.nrg.xnat.eventservice.services.EventServiceActionProvider;
import org.nrg.xnat.eventservice.services.EventServiceComponentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventServiceComponentManagerImpl implements EventServiceComponentManager {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private static final String EVENT_RESOURCE_PATH ="classpath*:META-INF/xnat/event/*-xnateventserviceevent.properties";

    private List<EventServiceListener> installedListeners;
    private List<EventServiceActionProvider> actionProviders;
    private List<EventServiceEvent> installedEvents;

    @Autowired
    public EventServiceComponentManagerImpl(@Lazy final List<EventServiceListener> installedListeners,
                                            @Lazy final List<EventServiceActionProvider> actionProviders) {
        this.installedListeners = installedListeners;
        this.actionProviders = actionProviders;

        try {
            this.installedEvents = loadInstalledEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<EventServiceEvent> getInstalledEvents() throws Exception {
        if (installedEvents == null || installedEvents.isEmpty()){
            try {
                installedEvents = loadInstalledEvents();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return installedEvents;
    }

    @Override
    public org.nrg.xnat.eventservice.events.EventServiceEvent getEvent(String id) {
        return null;
    }

    @Override
    public List<EventServiceListener> getInstalledListeners() { return installedListeners; }

    @Override
    public EventServiceListener getListener(String name) {
        for(EventServiceListener el: installedListeners){
            if(el.toString().contains(name)) {
                return el;
            }
        }
        return null;
    }

    @Override
    public List<EventServiceActionProvider> getActionProviders() {
        return actionProviders;
    }

    public List<EventServiceEvent> loadInstalledEvents() throws IOException, NoSuchMethodException {
        List<EventServiceEvent> events = new ArrayList<>();
        for (final Resource resource : BasicXnatResourceLocator.getResources(EVENT_RESOURCE_PATH)) {
            try {
                events.add(CombinedEventServiceEvent.createFromResource(resource));
            } catch (IOException |ClassNotFoundException|IllegalAccessException|InvocationTargetException |InstantiationException e) {
                log.debug("Exception loading EventClass from " + resource.toString());
                log.debug("Possible missing Class Definition");
                log.debug(e.getMessage());
            }
        }
        return events;
    }

}
