package org.nrg.xnat.eventservice.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
public class TimedEventStatus implements Serializable {

    public TimedEventStatus(Status status, Date statusTimestamp, String message, SubscriptionDeliveryEntity subscriptionDeliveryEntity) {
        this.status = status;
        this.statusTimestamp = statusTimestamp;
        this.message = message;
        this.subscriptionDeliveryEntity = subscriptionDeliveryEntity;
    }

    private long id;
    @JsonProperty("status") private Status status;
    @JsonProperty("timestamp")  private Date statusTimestamp;
    @JsonProperty("message") private String message;
    private SubscriptionDeliveryEntity subscriptionDeliveryEntity;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Enumerated(EnumType.STRING)
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Column(name = "status_timestamp")
    public Date getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(Date statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @ManyToOne
    //@JoinColumn(name = "subscription_delivery_entity_id")
    public SubscriptionDeliveryEntity getSubscriptionDeliveryEntity() {
        return subscriptionDeliveryEntity;
    }

    public void setSubscriptionDeliveryEntity(SubscriptionDeliveryEntity subscriptionDeliveryEntity) {
        this.subscriptionDeliveryEntity = subscriptionDeliveryEntity;
    }

    @Override
    public String toString() {
        return "TimedEventStatus{" +
                "status=" + status +
                ", statusTimestamp=" + statusTimestamp +
                ", message='" + message + '\'' +
                ", subscriptionDeliveryEntity=" + subscriptionDeliveryEntity +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TimedEventStatus that = (TimedEventStatus) o;
        return status == that.status &&
                Objects.equal(statusTimestamp, that.statusTimestamp) &&
                Objects.equal(message, that.message) &&
                Objects.equal(subscriptionDeliveryEntity, that.subscriptionDeliveryEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), status, statusTimestamp, message, subscriptionDeliveryEntity);
    }

    public enum Status {
        EVENT_TRIGGERED,
        EVENT_DETECTED,
        SUBSCRIPTION_TRIGGERED,
        SUBSCRIPTION_DISABLED_HALT,
        OBJECT_SERIALIZED,
        OBJECT_FILTERED,
        OBJECT_FILTER_MISMATCH_HALT,
        ACTION_CALLED,
        ACTION_STEP,
        ACTION_FAILED,
        COMPLETE,
        FAILED
    }
}
