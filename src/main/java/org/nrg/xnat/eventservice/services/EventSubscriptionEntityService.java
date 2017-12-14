package org.nrg.xnat.eventservice.services;


import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.exceptions.SubscriptionValidationException;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.model.Subscription;
import reactor.bus.Event;

import javax.persistence.EntityNotFoundException;
import java.util.List;

public interface EventSubscriptionEntityService extends BaseHibernateService<SubscriptionEntity>{

    Subscription createSubscription(Subscription subscription) throws SubscriptionValidationException;
    Subscription validate(Subscription eventSubscription) throws SubscriptionValidationException;
    Subscription activate(Subscription eventSubscription);
    Subscription deactivate(Subscription eventSubscription) throws NotFoundException, EntityNotFoundException;
    Subscription save(Subscription subscription);

    Subscription update(Subscription subscription) throws SubscriptionValidationException, NotFoundException;
    void delete(Long subscriptionId) throws Exception;

    List<Subscription> getAllSubscriptions();
    List<Subscription> getSubscriptionsByKey(String key) throws NotFoundException;
    Subscription getSubscription(Long id) throws NotFoundException;
    void reregisterAllActive();

    void processEvent(EventServiceListener listener, Event event) throws NotFoundException;
}
