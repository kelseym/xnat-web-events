package org.nrg.xnat.eventservice.services.impl;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnat.eventservice.daos.SubscriptionDeliveryEntityDao;
import org.nrg.xnat.eventservice.entities.SubscriptionDeliveryEntity;
import org.nrg.xnat.eventservice.services.SubscriptionDeliveryEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionDeliveryEntityServiceImpl extends AbstractHibernateEntityService<SubscriptionDeliveryEntity, SubscriptionDeliveryEntityDao>
        implements SubscriptionDeliveryEntityService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionDeliveryEntityService.class);

}
