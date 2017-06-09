package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SimpleEvent {

    @JsonProperty("id") public abstract String id();
    @JsonProperty("listener") public abstract String listenerService();
    @JsonProperty("display-name") public abstract String displayName();
    @JsonProperty("description") public abstract String description();
    @JsonProperty("payload") public abstract String payloadClass();

    public static SimpleEvent create( @JsonProperty("id") String id,
                                     @JsonProperty("listener") String listenerService,
                                     @JsonProperty("display-name") String displayName,
                                     @JsonProperty("description") String description,
                                     @JsonProperty("payload") String payloadClass) {
        return builder()
                .id(id)
                .listenerService(listenerService)
                .displayName(displayName)
                .description(description)
                .payloadClass(payloadClass)
                .build();
    }

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

        public abstract SimpleEvent build();
    }
}
