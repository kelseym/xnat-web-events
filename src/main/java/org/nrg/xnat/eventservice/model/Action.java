package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.nrg.xnat.eventservice.services.EventServiceActionProvider;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class Action {

    @JsonIgnore
    @JsonProperty("id") public abstract String id();
    @JsonProperty("action-key") public abstract String actionKey();
    @Nullable
    @JsonProperty("display-name") public abstract String displayName();
    @Nullable
    @JsonProperty("description") public abstract String description();
    @JsonIgnore
    @JsonProperty("provider") public abstract EventServiceActionProvider provider();
    @Nullable
    @JsonProperty("attributes") public abstract Map<String, String> attributes();
    @Nullable
    @JsonProperty("events") public abstract List<String> events();

    public static Action create(String id,
                                String actionKey,
                                String displayName,
                                String description,
                                EventServiceActionProvider provider,
                                Map<String, String> attributes,
                                List<String> events) {
        return builder()
                .id(id)
                .actionKey(actionKey)
                .displayName(displayName)
                .description(description)
                .provider(provider)
                .attributes(attributes==null ? Collections.<String, String>emptyMap() : attributes)
                .events(events)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_Action.Builder();
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder id(String className);

        public abstract Builder actionKey(String actionKey);

        public abstract Builder displayName(String displayName);

        public abstract Builder description(String description);

        public abstract Builder provider(EventServiceActionProvider provider);

        public abstract Builder attributes(Map<String, String> attributes);

        public abstract Builder events(List<String> events);

        public abstract Action build();
    }
}
