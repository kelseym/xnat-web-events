package org.nrg.xnat.eventservice.services;


import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.exceptions.SubscriptionValidationException;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.model.*;
import reactor.bus.Event;

import java.util.List;

public interface EventService {
    List<SimpleEvent> getEvents() throws Exception;

    @Deprecated
    List<Listener> getInstalledListeners();


    List<ActionProvider> getActionProviders();
    List<ActionProvider> getActionProviders(String xnatType, String projectId);

    List<Action> getAllActions(UserI user);
    List<Action> getAllActions(String xnatType, UserI user);
    List<Action> getAllActions(String projectId, String xnatType, UserI user);

    List<Action> getActionsByProvider(String actionProvider);

    List<Subscription> getSubscriptions();
    Subscription getSubscription(Long id) throws NotFoundException;
    Subscription validateSubscription(Subscription subscription) throws SubscriptionValidationException;
    Subscription createSubscription(Subscription subscription) throws SubscriptionValidationException;
    Subscription updateSubscription(Subscription subscription) throws SubscriptionValidationException, NotFoundException;
    void deleteSubscription(Long id) throws NotFoundException;

    void processEvent(EventServiceListener listener, Event event);

}
