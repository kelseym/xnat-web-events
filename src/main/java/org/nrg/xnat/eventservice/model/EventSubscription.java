package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class EventSubscription {

    @JsonProperty("id") public abstract long id();
    @JsonProperty("name") public abstract String name();
    @JsonProperty("enabled") public abstract boolean isEnabled();
    @Nullable @JsonProperty("subscriptionKey") public abstract Integer subscriptionKey();
    @JsonProperty("eventType") public abstract String eventType();
    @JsonProperty("consumerType") public abstract String consumerType();
    @Nullable @JsonProperty("projectId") public abstract String projectId();

    public static EventSubscription create(long id, String name, boolean isEnabled, Integer subscriptionKey, String eventType, String consumerType, String projectId) {
        return builder()
                .id(id)
                .name(name)
                .isEnabled(isEnabled)
                .subscriptionKey(subscriptionKey)
                .eventType(eventType)
                .consumerType(consumerType)
                .projectId(projectId)
                .build();
    }

    abstract Builder toBuilder();


    public EventSubscription withSubscriptionKey(final Integer subscriptionKey) {
        return toBuilder().subscriptionKey(subscriptionKey).build();
    }

    public static Builder builder() {
        return new AutoValue_EventSubscription.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(long id);

        public abstract Builder name(String name);

        public abstract Builder isEnabled(boolean isEnabled);

        public abstract Builder subscriptionKey(Integer subscriptionKey);

        public abstract Builder eventType(String eventType);

        public abstract Builder consumerType(String consumerType);

        public abstract Builder projectId(String projectId);

        public abstract EventSubscription build();
    }
}
