package org.nrg.xnat.eventservice.services.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.model.Action;
import org.nrg.xnat.eventservice.services.ActionManager;
import org.nrg.xnat.eventservice.services.EventService;
import org.nrg.xnat.eventservice.services.EventServiceActionProvider;
import org.nrg.xnat.eventservice.services.EventServiceComponentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ActionManagerImpl implements ActionManager {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventServiceComponentManager componentManager;

    @Autowired
    public ActionManagerImpl(final EventServiceComponentManager componentManager) {
        this.componentManager = componentManager;
    }


    // ** actionKey should be formatted as "actionProvider:actionLabel" ** //
    @Override
    public Action getActionByKey(String actionKey) {
        String actionProvider = null;
        String actionId = null;
        Iterable<String> key = Splitter.on(':')
                                         .trimResults()
                                         .omitEmptyStrings()
                                         .split(actionKey);
        ImmutableList<String> keyList = ImmutableList.copyOf(key);
        if(!keyList.isEmpty()) {
            actionProvider = actionId = keyList.get(0);
            if(keyList.size()>1){
                actionId = keyList.get(1);
            }
        }
        List<Action> actions = getActionsByProvider(actionProvider);
        for(Action action : actions) {
            if(action.actionKey().contentEquals(actionKey)) {
                return action;
            }
        }
        return null;
    }

    @Override
    public List<Action> getActions(UserI user) {
        List<Action> actions = new ArrayList<>();
        for(EventServiceActionProvider provider:getActionProviders()) {
            actions.addAll(provider.getActions(user));
        }
        return actions;
    }

    @Override
    public List<Action> getActions(String xnatType, UserI user) {
        List<Action> actions = new ArrayList<>();
        for(EventServiceActionProvider provider:getActionProviders()) {
            actions.addAll(provider.getActions(xnatType, user));
        }
        return actions;
    }

    @Override
    public List<Action> getActions(String projectId, String xnatType, UserI user) {
        List<Action> actions = new ArrayList<>();
        for(EventServiceActionProvider provider:getActionProviders()) {
            actions.addAll(provider.getActions(projectId, xnatType, user));
        }
        return actions;
    }

    @Override
    public List<Action> getActionsByProvider(String providerName) {
        for(EventServiceActionProvider provider : componentManager.getActionProviders()){
            if(provider.getName().contentEquals(providerName)) {
                return provider.getActions(null);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<Action> getActionsByProvider(EventServiceActionProvider provider) {
        return provider.getActions(null);
    }

    @Override
    public List<Action> getActionsByObject(String operation) {
//        List<EventServiceAction> actions = null;
//        for(EventServiceAction action : getActionsByProvider()){
//            if(action.)
//        }
        return null;
    }

    @Override
    public List<EventServiceActionProvider> getActionProviders() {
        return componentManager.getActionProviders();
    }

    @Override
    public EventServiceActionProvider getActionProvider(String providerName) {
        List<EventServiceActionProvider> providers = getActionProviders();
        for(EventServiceActionProvider provider : providers){
            if(provider.getName().contentEquals(providerName)){
                return provider;
            }
        }
        return null;
    }

    @Override
    public boolean validateAction(Action action) {
        List<Action> actions = getActionsByProvider(action.provider());
        if(actions != null && actions.contains(action)) {
            return true;
        }
        return false;
    }

}
