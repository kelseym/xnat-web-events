package org.nrg.xnat.eventservice.services;


import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xnat.eventservice.entities.SubscriptionDeliveryEntity;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.entities.TimedEventStatus;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;

public interface SubscriptionDeliveryEntityService extends BaseHibernateService<SubscriptionDeliveryEntity> {

    Long create(SubscriptionEntity subscription, EventServiceEvent event, EventServiceListener listener,
                String actionUserLogin, String projectId, String actionInputs);
    void addStatus(Long deliveryId, TimedEventStatus status);
}
