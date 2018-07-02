package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;


@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class ProjectSubscriptionCreator {

    @Nullable @JsonProperty("name") public abstract String name();
    @JsonProperty("project-id") public abstract String projectId();
    @Nullable @JsonProperty("active") public abstract Boolean active();
    @JsonProperty("event-type") public abstract String eventType();
    @Nullable @JsonProperty("event-status") public abstract String eventStatus();
    @Nullable @JsonIgnore public abstract String customListenerId();
    @JsonProperty("action-key") public abstract String actionKey();
    @Nullable @JsonProperty("attributes") public abstract Map<String, String> attributes();
    @Nullable @JsonProperty("event-filter") public abstract EventFilter eventFilter();

    public static Builder builder() {
        return new AutoValue_ProjectSubscriptionCreator.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static ProjectSubscriptionCreator create(@Nullable @JsonProperty("name") final String name,
                                                    @JsonProperty("project-id")  String projectId,
                                                    @JsonProperty("active") final Boolean active,
                                                    @Nonnull @JsonProperty("event-type") final String eventType,
                                                    @Nullable @JsonProperty("event-status") final String eventStatus,
                                                    @Nullable @JsonProperty("custom-listener-id") final String customListenerId,
                                                    @Nonnull @JsonProperty("action-key") final String actionKey,
                                                    @JsonProperty("attributes") final Map<String, String> attributes,
                                                    @JsonProperty("event-filter") final EventFilter eventFilter) {

        return builder()
                .name(name)
                .projectId(projectId)
                .active(active)
                .eventType(eventType)
                .eventStatus(eventStatus)
                .customListenerId(customListenerId)
                .actionKey(actionKey)
                .attributes(attributes==null ? Collections.<String, String>emptyMap() : attributes)
                .eventFilter(eventFilter)
                .build();
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder name(String name);

        public abstract Builder projectId(String projectId);

        public abstract Builder attributes(Map<String, String> attributes);

        public abstract Builder active(Boolean active);

        public abstract Builder eventType(String eventId);

        public abstract Builder eventStatus(String eventStatus);

        public abstract Builder customListenerId(String customListenerId);

        public abstract Builder actionKey(String actionKey);

        public abstract Builder eventFilter(EventFilter eventFilter);

        public abstract ProjectSubscriptionCreator build();
    }

}