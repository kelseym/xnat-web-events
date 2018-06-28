package org.nrg.xnat.eventservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class EventSignature {
    @JsonProperty("event-id")   public abstract String eventId();
    @JsonProperty("project-ids") public abstract List<String> projectIds();
    @JsonProperty("status")     public abstract String status();



    public abstract Builder toBuilder();

    public static EventSignature create(@JsonProperty("event-id")   String eventId,
                                        @JsonProperty("project-ids") List<String> projectIds,
                                        @JsonProperty("status")     String status) {
        return builder()
                .eventId(eventId)
                .projectIds(projectIds)
                .status(status)
                .build();
    }

    public static Builder builder() {return new AutoValue_EventSignature.Builder();}

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder eventId(String eventId);

        public abstract Builder projectIds(List<String> projectIds);

        public abstract Builder status(String status);

        public abstract EventSignature build();
    }
}
