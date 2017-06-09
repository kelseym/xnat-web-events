package org.nrg.xnat.eventservice.entities;

import com.google.common.base.MoreObjects;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.xnat.eventservice.model.EventSubscription;

import javax.persistence.Entity;
import java.util.Objects;


@Entity
public class EventSubscriptionEntity extends AbstractHibernateEntity {

    public EventSubscriptionEntity() {}

    public EventSubscriptionEntity(final String name,
                                   final boolean active,
                                   final Integer subscriptionKey,
                                   final String eventType,
                                   final String consumerType,
                                   final String projectId) {
        this.name = name;
        this.active = active;
        this.subscriptionKey = subscriptionKey;
        this.eventType = eventType;
        this.consumerType = consumerType;
        this.projectId = projectId;
    }

    private String name;
    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }

    private Integer subscriptionKey;
    public Integer getSubscriptionKey() { return subscriptionKey; }
    public void setSubscriptionKey(Integer subscriptionKey) { this.subscriptionKey = subscriptionKey; }

    private String eventType;
    public String getEventType() { return eventType; }
    public void setEventType(String event) { this.eventType = event; }

    private String consumerType;
    public String getConsumerType() { return consumerType; }
    public void setConsumerType(String consumer) { this.consumerType = consumer; }

    private String projectId;
    public String getProjectId() { return projectId;}
    public void setProjectId(String filter) {this.projectId = filter;}

    private boolean active;
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("subscriptionKey",subscriptionKey.toString())
                .add("eventType",eventType)
                .add("consumerType",consumerType)
                .toString();
    }


    public static EventSubscriptionEntity fromPojo(EventSubscription subscription) {
        return new EventSubscriptionEntity(
                subscription.name(),
                subscription.isEnabled(),
                subscription.subscriptionKey(),
                subscription.eventType(),
                subscription.consumerType(),
                subscription.projectId());
    }

    public EventSubscription toPojo() {
        return EventSubscription.create(this.getId(),getName(), this.isActive(), this.getSubscriptionKey(),this.getEventType(),this.getConsumerType(), this.getProjectId());
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Registration getListenerRegistrationKey() {
        return listenerRegistrationKey;
    }

    public void setListenerRegistrationKey(Registration listenerRegistrationKey) {
        this.listenerRegistrationKey = listenerRegistrationKey;
    }
}
