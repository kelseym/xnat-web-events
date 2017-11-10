package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;


@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class SubscriptionCreator {

    @JsonProperty("name") public abstract String name();
    @Nullable @JsonProperty("active") public abstract Boolean active();
    @JsonProperty("event-id") public abstract String event();
    @JsonProperty("action") public abstract Action action();
    @Nullable @JsonProperty("event-filter") public abstract EventFilter eventFilter();
    @JsonProperty("act-as-event-user") public abstract boolean actAsEventUser();

    public static Builder builder() {
        return new AutoValue_SubscriptionCreator.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static SubscriptionCreator create(@Nonnull @JsonProperty("name") final String name,
                                              @JsonProperty("active") final Boolean active,
                                             @Nonnull @JsonProperty("event-id") final String event,
                                             @Nonnull @JsonProperty("action") final Action action,
                                                  @JsonProperty("event-filter") final EventFilter eventFilter,
                                                  @JsonProperty("act-as-event-user") final Boolean actAsEventUser) {
        return builder()
                .name(name)
                .active(active)
                .event(event)
                .action(action)
                .eventFilter(eventFilter)
                .actAsEventUser(actAsEventUser == null ? false : actAsEventUser)
                .build();
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder name(String name);

        public abstract Builder active(Boolean active);

        public abstract Builder event(String event);

        public abstract Builder action(Action action);

        public abstract Builder eventFilter(EventFilter eventFilter);

        public abstract Builder actAsEventUser(boolean actAsEventUser);

        public abstract SubscriptionCreator build();
    }

}