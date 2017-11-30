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
    @JsonProperty("event-id") public abstract String eventId();
    @Nullable @JsonProperty("custom-listener-id") public abstract String customListenerId();
    @JsonProperty("action-key") public abstract String actionKey();
    @Nullable @JsonProperty("attributes") public abstract Map<String, String> attributes();
    @Nullable @JsonProperty("event-filter") public abstract EventFilter eventFilter();
    @JsonProperty("act-as-event-user") public abstract boolean actAsEventUser();

    public static Builder builder() {
        return new AutoValue_SubscriptionCreator.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static SubscriptionCreator create(@Nonnull @JsonProperty("name") final String name,
                                             @JsonProperty("active") final Boolean active,
                                             @Nonnull @JsonProperty("event-id") final String eventId,
                                             @JsonProperty("custom-listener-id") final String customListenerId,
                                             @Nonnull @JsonProperty("action-key") final String actionKey,
                                             @JsonProperty("attributes") final Map<String, String> attributes,
                                             @JsonProperty("eventId-filter") final EventFilter eventFilter,
                                             @JsonProperty("act-as-eventId-user") final Boolean actAsEventUser) {
        return builder()
                .name(name)
                .active(active)
                .eventId(eventId)
                .customListenerId(customListenerId)
                .actionKey(actionKey)
                .attributes(attributes==null ? Collections.<String, String>emptyMap() : attributes)
                .eventFilter(eventFilter)
                .actAsEventUser(actAsEventUser == null ? false : actAsEventUser)
                .build();
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder name(String name);

        public abstract Builder attributes(Map<String, String> attributes);

        public abstract Builder active(Boolean active);

        public abstract Builder eventId(String eventId);

        public abstract Builder customListenerId(String customListenerId);

        public abstract Builder actionKey(String actionKey);

        public abstract Builder eventFilter(EventFilter eventFilter);

        public abstract Builder actAsEventUser(boolean actAsEventUser);

        public abstract SubscriptionCreator build();
    }

}