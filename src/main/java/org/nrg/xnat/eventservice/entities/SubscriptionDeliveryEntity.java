package org.nrg.xnat.eventservice.entities;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.util.Date;
import java.util.Map;

@Entity
public class SubscriptionDeliveryEntity extends AbstractHibernateEntity {

    public SubscriptionDeliveryEntity() {}

    private SubscriptionEntity subscription;
    private String eventObject;
    private String actionUser;
    private String projectId;
    private String actionInputs;

    public SubscriptionDeliveryEntity(SubscriptionEntity subscription, String eventObject, String actionUser,
                                      String projectId, String actionInputs,
                                      Map<TimedStatus, Date> timedStatus) {
        this.subscription = subscription;
        this.eventObject = eventObject;
        this.actionUser = actionUser;
        this.projectId = projectId;
        this.actionInputs = actionInputs;
        this.timedStatus = timedStatus;
    }

    @ManyToOne
    public SubscriptionEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionEntity subscription) {
        this.subscription = subscription;
    }

    public String getEventObject() {
        return eventObject;
    }

    public void setEventObject(String eventObject) {
        this.eventObject = eventObject;
    }

    public String getActionUser() {
        return actionUser;
    }

    public void setActionUser(String actionUser) {
        this.actionUser = actionUser;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getActionInputs() {
        return actionInputs;
    }

    public void setActionInputs(String actionInputs) {
        this.actionInputs = actionInputs;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public Map<TimedStatus, Date> getTimedStatus() {
        return timedStatus;
    }

    public void setTimedStatus(
            Map<TimedStatus, Date> timedStatus) {
        this.timedStatus = timedStatus;
    }

    private Map<TimedStatus, Date> timedStatus;

    public enum TimedStatus {
        EVENT_TRIGGERED,
        EVENT_DETECTED,
        SUBSCRIPTION_TRIGGERED,
        OBJECT_SERIALIZED,
        OBJECT_FILTERED,
        ACTION_CALLED,
        COMPLETE,
        FAILED
    }
}
