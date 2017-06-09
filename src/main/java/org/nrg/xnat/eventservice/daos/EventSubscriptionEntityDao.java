package org.nrg.xnat.eventservice.daos;

import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.springframework.stereotype.Repository;

@Repository
public class EventSubscriptionEntityDao extends AbstractHibernateDAO<SubscriptionEntity> {


    public SubscriptionEntity findByName(final String name) throws Exception {
        try {
            return findByUniqueProperty("name", name);
        } catch (RuntimeException e) {
            throw new Exception("More than one result with name " + name + ".");
        }
    }
}
