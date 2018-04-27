package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
public abstract class SimpleEvent {

    @JsonProperty("id") public abstract String id();
    @JsonIgnore
    @JsonProperty("listener") public abstract String listenerService();
    @JsonProperty("display-name") public abstract String displayName();
    @JsonProperty("description") public abstract String description();
    @JsonIgnore
    @JsonProperty("payload") public abstract String payloadClass();
    @JsonProperty("xnat-type") public abstract String xnatType();
    @JsonProperty("is-xsi-type") public abstract boolean isXsiType();
    @Nullable @JsonProperty("node-filters") public abstract Map<String, JsonPathFilterNode> nodeFilters();
    @Nullable @JsonProperty("event-properties") public abstract Map<String, EventPropertyNode> eventProperties();


    public static SimpleEvent create( @JsonProperty("id") String id,
                                     @JsonProperty("listener") String listenerService,
                                     @JsonProperty("display-name") String displayName,
                                     @JsonProperty("description") String description,
                                     @JsonProperty("payload") String payloadClass,
                                     @JsonProperty("xnat-type") String xnatType,
                                     @JsonProperty("is-xsi-type") boolean isXsiType) {
        return builder()
                .id(id)
                .listenerService(listenerService)
                .displayName(displayName)
                .description(description)
                .payloadClass(payloadClass)
                .xnatType(xnatType)
                .isXsiType(isXsiType)
                .build();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_SimpleEvent.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder listenerService(String listenerService);

        public abstract Builder displayName(String displayName);

        public abstract Builder description(String description);

        public abstract Builder payloadClass(String payloadClass);

        public abstract Builder xnatType(String xnatType);

        public abstract Builder isXsiType(boolean isXsiType);

        public abstract Builder nodeFilters(Map<String, JsonPathFilterNode> nodeFilters);

        public abstract Builder eventProperties(Map<String, EventPropertyNode> eventProperties);

        public abstract SimpleEvent build();
    }
}
