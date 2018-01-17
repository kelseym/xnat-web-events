package org.nrg.xnat.eventservice.services.impl;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnat.eventservice.daos.SubscriptionDeliveryEntityDao;
import org.nrg.xnat.eventservice.entities.SubscriptionDeliveryEntity;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.entities.TimedEventStatus;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.services.SubscriptionDeliveryEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

import static org.nrg.xnat.eventservice.entities.TimedEventStatus.Status.EVENT_DETECTED;
import static org.nrg.xnat.eventservice.entities.TimedEventStatus.Status.EVENT_TRIGGERED;

@Service
public class SubscriptionDeliveryEntityServiceImpl
        extends AbstractHibernateEntityService<SubscriptionDeliveryEntity, SubscriptionDeliveryEntityDao>
        implements SubscriptionDeliveryEntityService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionDeliveryEntityService.class);

    @Override
    public Long create(@Nonnull SubscriptionEntity subscription, @Nonnull EventServiceEvent event, EventServiceListener listener, String actionUserLogin, String projectId,
                       String actionInputs) {
        SubscriptionDeliveryEntity delivery = super.create(new SubscriptionDeliveryEntity(subscription, event.getEventUUID(), actionUserLogin, projectId, actionInputs));
        if(delivery != null){
            log.debug("Created new SubscriptionDeliveryEntity for subscription: {} and eventUUID {}", subscription.getName(), event.getEventUUID());
            addStatus(delivery.getId(), new TimedEventStatus(EVENT_TRIGGERED, event.getEventTimestamp(), "Event triggered."));
            addStatus(delivery.getId(), new TimedEventStatus(EVENT_DETECTED, listener.getDetectedTimestamp(), "Event detected."));
            return delivery.getId();
        }
        log.error("Could not create new SubscriptionDeliveryEntity for subscription: {} and eventUUID {}", subscription.getName(), event.getEventUUID());
        return null;
    }

    @Override
    public void addStatus(Long deliveryId, TimedEventStatus status) {
        SubscriptionDeliveryEntity subscriptionDeliveryEntity = retrieve(deliveryId);
        if(subscriptionDeliveryEntity != null) {
            subscriptionDeliveryEntity.addTimedEventStatus(status);
            log.debug("Updated SubscriptionDeliveryEntity: {} to update with status: {}", deliveryId, status.getStatus().toString());
        } else{
            log.error("Could not find SubscriptionDeliveryEntity: {} to update with status: {}", deliveryId, status.toString());
        }
    }
}
