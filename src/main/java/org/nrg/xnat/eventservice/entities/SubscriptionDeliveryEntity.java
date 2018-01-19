package org.nrg.xnat.eventservice.entities;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
//@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"eventUUID", "subscription"})})
public class SubscriptionDeliveryEntity extends AbstractHibernateEntity {

    public SubscriptionDeliveryEntity() {}

    private UUID eventUUID;
    private SubscriptionEntity subscription;
    private String actionUserLogin;
    private String projectId;
    private String actionInputs;
    private List<TimedEventStatus> timedEventStatuses = new ArrayList<>();

    public SubscriptionDeliveryEntity(SubscriptionEntity subscription, UUID eventUUID, String actionUserLogin,
                                      String projectId, String actionInputs) {
        this.subscription = subscription;
        this.eventUUID = eventUUID;
        this.actionUserLogin = actionUserLogin;
        this.projectId = projectId;
        this.actionInputs = actionInputs;
    }

    public UUID getEventUUID() {
        return eventUUID;
    }

    public void setEventUUID(UUID eventUUID) {
        this.eventUUID = eventUUID;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    public SubscriptionEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionEntity subscription) {
        this.subscription = subscription;
    }

    public String getActionUserLogin() {
        return actionUserLogin;
    }

    public void setActionUserLogin(String actionUserLogin) {
        this.actionUserLogin = actionUserLogin;
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

    @OneToMany(mappedBy = "subscriptionDeliveryEntity",cascade = CascadeType.ALL, orphanRemoval = true)
    public List<TimedEventStatus> getTimedEventStatuses() {
        return timedEventStatuses;
    }

    public void setTimedEventStatuses(List<TimedEventStatus> timedEventStatuses){
        this.timedEventStatuses = timedEventStatuses;
    }

    @Transient
    public void addTimedEventStatus(TimedEventStatus timedEventStatus){
        timedEventStatuses.add(timedEventStatus);
        timedEventStatus.setSubscriptionDeliveryEntity(this);
    }
}
