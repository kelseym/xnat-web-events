package org.nrg.xnat.eventservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.nrg.xnat.eventservice.entities.TimedEventStatus;

import javax.annotation.Nullable;
import java.util.List;


@AutoValue
public abstract class SubscriptionDelivery {

    @JsonProperty("id") public abstract Long id();
    @Nullable @JsonProperty("event") public abstract SimpleEvent event();
    @JsonProperty("subscription") public abstract Subscription subscription();
    @JsonProperty("user") public abstract String actionUser();
    @JsonProperty("project") public abstract String projectId();
    @JsonProperty("inputs") public abstract String actionInputs();
    @JsonProperty("status") public abstract List<TimedEventStatus> timedEventStatuses();

    public static SubscriptionDelivery create(Long id, SimpleEvent event, Subscription subscription, String actionUser, String projectId, String actionInputs, List<TimedEventStatus> timedEventStatuses) {
        return builder()
                .id(id)
                .event(event)
                .subscription(subscription)
                .actionUser(actionUser)
                .projectId(projectId)
                .actionInputs(actionInputs)
                .timedEventStatuses(timedEventStatuses)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SubscriptionDelivery.Builder();
    }

    public abstract SubscriptionDelivery.Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(Long id);

        public abstract Builder event(SimpleEvent event);

        public abstract Builder subscription(Subscription subscription);

        public abstract Builder actionUser(String actionUser);

        public abstract Builder projectId(String projectId);

        public abstract Builder actionInputs(String actionInputs);

        public abstract Builder timedEventStatuses(List<TimedEventStatus> timedEventStatuses);

        public abstract SubscriptionDelivery build();
    }
}
