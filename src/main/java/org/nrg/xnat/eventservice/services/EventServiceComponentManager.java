package org.nrg.xnat.eventservice.services;

import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;

import java.util.List;

public interface EventServiceComponentManager {

    List<EventServiceEvent> getInstalledEvents();
    EventServiceEvent getEvent(String id);

    List<EventServiceListener> getInstalledListeners();
    EventServiceListener getListener(String listenerClassName);

    List<EventServiceActionProvider> getActionProviders();

}
