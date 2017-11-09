package org.nrg.xnat.eventservice.services.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
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
        String actionName = null;
        Iterable<String> key = Splitter.on(':')
                                         .trimResults()
                                         .omitEmptyStrings()
                                         .split(actionKey);
        ImmutableList<String> keyList = ImmutableList.copyOf(key);
        if(!keyList.isEmpty()) {
            actionProvider = actionName = keyList.get(0);
            if(keyList.size()>1){
                actionName = keyList.get(1);
            }
        }
        List<Action> actions = getActionsByProvider(actionProvider);
        for(Action action : actions) {
            if(action.id().contentEquals(actionName)) {
                return action;
            }
        }
        return null;
    }

    @Override
    public List<Action> getActions() {
        List<Action> actions = new ArrayList<>();
        for(EventServiceActionProvider provider:getActionProviders()) {
            actions.addAll(provider.getActions());
        }
        return actions;
    }

    @Override
    public List<Action> getActions(String xnatType) {
        List<Action> actions = new ArrayList<>();
        for(EventServiceActionProvider provider:getActionProviders()) {
            actions.addAll(provider.getActions(xnatType, null));
        }
        return actions;
    }

    @Override
    public List<Action> getActions(String xnatType, String projectId) {
        List<Action> actions = new ArrayList<>();
        for(EventServiceActionProvider provider:getActionProviders()) {
            actions.addAll(provider.getActions(xnatType, projectId,null));
        }
        return actions;
    }

    @Override
    public List<Action> getActionsByProvider(String providerName) {
        for(EventServiceActionProvider provider : componentManager.getActionProviders()){
            if(provider.getName().contentEquals(providerName)) {
                return provider.getActions();
            }
        }
        return null;
    }

    @Override
    public List<Action> getActionsByProvider(EventServiceActionProvider provider) {
        return provider.getActions();
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
