package org.nrg.xnat.eventservice.services;

import org.nrg.xnat.eventservice.model.Action;

import java.util.List;

public interface ActionManager {

    Action getActionByKey(String actionKey);

    List<Action> getActions();
    List<Action> getActions(String xnatType);
    List<Action> getActions(String xnatType, String projectId);

    List<Action> getActionsByProvider(String providerName);
    List<Action> getActionsByProvider(EventServiceActionProvider provider);
    List<Action> getActionsByObject(String operation);

    List<EventServiceActionProvider> getActionProviders();
    EventServiceActionProvider getActionProvider(String providerName);

    boolean validateAction(Action action);
}
