package org.nrg.xnat.eventservice.services;

import org.nrg.xft.security.UserI;
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

    // Return a list of org.nrg.xnat.eventservice.model.Action objects to describe actions available to the system //
    List<Action> getActions();
    List<Action> getActions(String xsiType, UserI user);
    List<Action> getActions(String projectId, String xsiType, UserI user);

    List<String> getEvents();
    void processEvent(final SimpleEventServiceEvent event, SubscriptionEntity subscription);
}
