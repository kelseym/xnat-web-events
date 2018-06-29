package org.nrg.xnat.eventservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.Filter;

import javax.annotation.Nullable;


@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class EventSignature {
    @JsonProperty("event-id")   public abstract String eventId();
    @Nullable @JsonProperty("project-id") public abstract String projectId();
    @JsonProperty("status")     public abstract String status();



    public abstract Builder toBuilder();

    public static EventSignature create(@JsonProperty("event-id")   String eventId,
                                        @Nullable @JsonProperty("project-id") String projectId,
                                        @JsonProperty("status")     String status) {
        return builder()
                .eventId(eventId)
                .projectId(projectId)
                .status(status)
                .build();
    }

    public static Builder builder() {return new AutoValue_EventSignature.Builder();}

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder eventId(String eventId);

        public abstract Builder projectId(String projectId);

        public abstract Builder status(String status);

        public abstract EventSignature build();
    }

}
