package org.nrg.xnat.eventservice.services;


import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.exceptions.SubscriptionValidationException;
import org.nrg.xnat.eventservice.model.Subscription;

import javax.persistence.EntityNotFoundException;
import java.util.List;

public interface EventSubscriptionEntityService extends BaseHibernateService<SubscriptionEntity>{


     Subscription validate(Subscription eventSubscription) throws SubscriptionValidationException;
     Subscription activate(Subscription eventSubscription);
     Subscription deactivate(Subscription eventSubscription) throws NotFoundException, EntityNotFoundException;

    Subscription save(Subscription subscription);
    void delete(Subscription subscription) throws NotFoundException, Exception;

    Subscription activateAndSave(Subscription subscription) throws SubscriptionValidationException;
    Subscription update(Subscription subscription) throws SubscriptionValidationException, NotFoundException;

     List<Subscription> getAllSubscriptions();


    Subscription getSubscription(Long id) throws NotFoundException;
}
