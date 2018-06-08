package org.nrg.xnat.eventservice.daos;

import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xnat.eventservice.entities.SubscriptionDeliveryEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class XnatObjectIntrospectionDao extends AbstractHibernateDAO<SubscriptionDeliveryEntity> {

    public List<XnatProjectdata> getProjects(String projectId) throws Throwable{
        return getSession()
                .createQuery("SELECT pro FROM xnat_projectdata WHERE id = :projectId")
                .setString("projectId", projectId)
                .list();
    }

    public Boolean isExperimentModified(String experimentId){
        
    }

}
