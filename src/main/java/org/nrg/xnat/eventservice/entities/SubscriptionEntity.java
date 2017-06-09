package org.nrg.xnat.eventservice.entities;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.xnat.eventservice.model.Subscription;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Entity
public class SubscriptionEntity extends AbstractHibernateEntity {

    public SubscriptionEntity() {}


    private String name;
    private java.lang.Boolean active;
    private Integer listenerRegistrationKey;
    private String eventType;
    private String actionKey;
    private Map<String,String> attributes;
    private EventServiceFilterEntity eventServiceFilterEntity;
    private Boolean actAsEventUser;



    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("id", getId())
                          .add("name", name)
                          .add("active", active)
                          .add("listenerRegistrationKey", listenerRegistrationKey)
                          .add("eventType", eventType)
                          .add("actionKey", actionKey)
                          .add("attributes", attributes)
                          .add("eventServiceFilterEntity", eventServiceFilterEntity)
                          .add("actAsEventUser", actAsEventUser).toString();

    }

    public SubscriptionEntity(String name, java.lang.Boolean active, Integer listenerRegistrationKey, String eventType, String actionKey, Map<String, String> attributes, EventServiceFilterEntity eventServiceFilterEntity, Boolean actAsEventUser) {
        this.name = name;
        this.active = active;
        this.listenerRegistrationKey = listenerRegistrationKey;
        this.eventType = eventType;
        this.actionKey = actionKey;
        this.attributes = attributes;
        this.eventServiceFilterEntity = eventServiceFilterEntity;
        this.actAsEventUser = actAsEventUser;
    }

    @Transactional
    @Nonnull
    public SubscriptionEntity update(@Nonnull final Subscription subscription) {
        this.name = Strings.isNullOrEmpty(subscription.name()) ? this.name : subscription.name();
        this.active = subscription.active() == null ? this.active : subscription.active();
        this.listenerRegistrationKey = subscription.listenerRegistrationKey() == null ? this.listenerRegistrationKey : subscription.listenerRegistrationKey();
        this.eventType = Strings.isNullOrEmpty(subscription.event()) ? this.eventType : subscription.event();
        this.actionKey = Strings.isNullOrEmpty(subscription.actionKey()) ? this.actionKey : subscription.actionKey();
        this.attributes = subscription.attributes() == null ? this.attributes : subscription.attributes();
        this.eventServiceFilterEntity = subscription.eventFilter() == null ? this.eventServiceFilterEntity : EventServiceFilterEntity.fromPojo(subscription.eventFilter());
        this.actAsEventUser = subscription.actAsEventUser() == null ? this.actAsEventUser : subscription.actAsEventUser();
        return this;
    }


    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getEventType() { return eventType; }

    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getActionProvider() { return actionKey; }

    public void setActionProvider(String actionKey) { this.actionKey = actionKey; }

    @ElementCollection
    public Map<String, String> getAttributes() { return attributes; }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes == null ?
            Maps.<String, String>newHashMap() :
                attributes; }

    @ManyToOne(cascade=CascadeType.ALL)
    public EventServiceFilterEntity getEventServiceFilterEntity() { return eventServiceFilterEntity; }

    public void setEventServiceFilterEntity(EventServiceFilterEntity eventServiceFilterEntity) { this.eventServiceFilterEntity = eventServiceFilterEntity; }

    public Boolean getActAsEventUser() { return actAsEventUser; }

    public void setActAsEventUser(Boolean actAsEventUser) { this.actAsEventUser = actAsEventUser; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SubscriptionEntity that = (SubscriptionEntity) o;
        return Objects.equal(name, that.name) &&
                Objects.equal(active, that.active) &&
                Objects.equal(listenerRegistrationKey, that.listenerRegistrationKey) &&
                Objects.equal(eventType, that.eventType) &&
                Objects.equal(actionKey, that.actionKey) &&
                Objects.equal(attributes, that.attributes) &&
                Objects.equal(eventServiceFilterEntity, that.eventServiceFilterEntity) &&
                Objects.equal(actAsEventUser, that.actAsEventUser);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), name, active, listenerRegistrationKey, eventType, actionKey, attributes, eventServiceFilterEntity, actAsEventUser);
    }

    public static SubscriptionEntity fromPojo(final Subscription subscription) {
        return fromPojoWithTemplate(subscription, new SubscriptionEntity());
    }

    public static SubscriptionEntity fromPojoWithTemplate(final Subscription subscription, final SubscriptionEntity template) {
        if(template==null) {
            return fromPojo(subscription);
        }
        template.name = subscription.name();
        template.active = subscription.active();
        template.listenerRegistrationKey = subscription.listenerRegistrationKey();
        template.eventType = subscription.event();
        template.actionKey = subscription.actionKey();
        template.attributes = subscription.attributes();
        template.eventServiceFilterEntity = EventServiceFilterEntity.fromPojo(subscription.eventFilter());
        template.actAsEventUser = subscription.actAsEventUser();
        return template;
    }

    public Subscription toPojo() {
        return Subscription.builder()
                .id(this.getId())
                .name(this.name)
                .active(this.active)
                .listenerRegistrationKey(this.listenerRegistrationKey)
                .event(this.eventType)
                .actionKey(this.actionKey)
                .attributes(this.attributes)
                .eventFilter(this.eventServiceFilterEntity != null ? this.eventServiceFilterEntity.toPojo() : null)
                .actAsEventUser(this.actAsEventUser)
                .build();
    }

    @Nonnull
    static public List<Subscription> toPojo(@Nonnull final List<SubscriptionEntity> subscriptionEntities) {
        List<Subscription> subscriptions = new ArrayList<>();
        for (SubscriptionEntity subscriptionEntity : subscriptionEntities) {
            subscriptions.add(subscriptionEntity.toPojo());
        }
        return subscriptions;
    }

    public java.lang.Boolean getActive() {
        return active;
    }

    public void setActive(java.lang.Boolean active) {
        this.active = active;
    }

    public Integer getListenerRegistrationKey() {
        return listenerRegistrationKey;
    }

    public void setListenerRegistrationKey(Integer listenerRegistrationKey) {
        this.listenerRegistrationKey = listenerRegistrationKey;
    }
}
