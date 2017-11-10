package org.nrg.xnat.eventservice.services;

import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.model.Action;

import java.util.List;

public interface ActionManager {

    Action getActionByKey(String actionKey);

    List<Action> getActions(UserI user);
    List<Action> getActions(String xnatType, UserI user);
    List<Action> getActions(String projectId, String xnatType, UserI user);

    List<Action> getActionsByProvider(String providerName);
    List<Action> getActionsByProvider(EventServiceActionProvider provider);
    List<Action> getActionsByObject(String operation);

    List<EventServiceActionProvider> getActionProviders();
    EventServiceActionProvider getActionProvider(String providerName);

    boolean validateAction(Action action);
}
