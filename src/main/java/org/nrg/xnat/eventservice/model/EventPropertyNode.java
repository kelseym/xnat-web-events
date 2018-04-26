package org.nrg.xnat.eventservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class EventPropertyNode {
    @Nullable
    @JsonProperty("value")          public abstract String  value();
    @JsonProperty("type")           public abstract String  type();

    public abstract Builder toBuilder();

    public static EventPropertyNode create(@Nullable @JsonProperty("value")       String value,
                                           @JsonProperty("type")                  String type) {
        return builder()
                .value(value)
                .type(type)
                .build();
    }

    public static Builder builder() {return new AutoValue_EventPropertyNode.Builder();}


    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder value(String value);

        public abstract Builder type(String type);

        public abstract EventPropertyNode build();
    }
}

