package org.nrg.xnat.eventservice.services;

import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.events.SimpleEventServiceEvent;
import org.nrg.xnat.eventservice.model.Action;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public interface EventServiceActionProvider {

    String getName();
    String getDisplayName();
    String getDescription();
    List<Action> getActions();
    List<String> getEvents();
    void processEvent(final SimpleEventServiceEvent event, SubscriptionEntity subscription);
}
