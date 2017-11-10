package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Map;


@AutoValue
public abstract class Subscription {

    @Nullable @JsonProperty("id") public abstract Long id();
    @Nullable @JsonProperty("name") public abstract String name();
    @Nullable @JsonProperty("active") public abstract Boolean active();
    @Nullable @JsonProperty("registration-key") public abstract  Integer listenerRegistrationKey();
    @Nullable @JsonProperty("event-id") public abstract String event();
    @Nullable @JsonProperty("action") public abstract Action action();
    @Nullable @JsonProperty("event-filter") public abstract EventFilter eventFilter();
    @Nullable @JsonProperty("act-as-event-user") public abstract Boolean actAsEventUser();

    public static Builder builder() {
        return new AutoValue_Subscription.Builder();
    }

    public abstract Builder toBuilder();


    @JsonCreator
    public static Subscription create(@JsonProperty("id") final Long id,
                                           @JsonProperty("name") final String name,
                                           @JsonProperty("active") final Boolean active,
                                           @JsonProperty("registration-key") final Integer listenerRegistrationKey,
                                           @JsonProperty("event-id") final String event,
                                           @JsonProperty("action") final Action action,
                                           @JsonProperty("event-filter") final EventFilter eventFilter,
                                           @JsonProperty("act-as-event-user") final Boolean actAsEventUser) {
        return builder()
                .id(id)
                .name(name)
                .active(active)
                .listenerRegistrationKey(listenerRegistrationKey)
                .event(event)
                .action(action)
                .eventFilter(eventFilter)
                .actAsEventUser(actAsEventUser)
                .build();
    }

    public static Subscription create(final SubscriptionCreator creator) {
        return builder()
                .name(creator.name())
                .active(creator.active())
                .event(creator.event())
                .action(creator.action())
                .eventFilter(creator.eventFilter())
                .actAsEventUser(creator.actAsEventUser())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(Long id);

        public abstract Builder name(String name);

        public abstract Builder listenerRegistrationKey(Integer listenerRegistrationKey);

        public abstract Builder active(Boolean active);

        public abstract Builder event(String event);

        public abstract Builder action(Action action);

        public abstract Builder eventFilter(EventFilter eventFilter);

        public abstract Builder actAsEventUser(Boolean actAsEventUser);

        public abstract Subscription build();
    }
}
