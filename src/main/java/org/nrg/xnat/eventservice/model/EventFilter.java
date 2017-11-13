package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class EventFilter {

    @JsonIgnore @Nullable @JsonProperty("id") public abstract Long id();
    @Nullable @JsonProperty("name") public abstract String name();
    @JsonProperty("project-ids") public abstract ImmutableList<String> projectIds();

    public static Builder builder() {
        return new AutoValue_EventFilter.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(Long id);

        public abstract Builder projectIds(@Nonnull List<String> projectIds);

        abstract ImmutableList.Builder<String> projectIdsBuilder();

        public Builder addProjectId(final @Nonnull String projectId) {
            projectIdsBuilder().add(projectId);
            return this;
        }

        public abstract Builder name(String name);

        public abstract EventFilter build();


    }


    public String toRegexPattern() {
        String pattern = "";
        if (projectIds() != null && !projectIds().isEmpty()) {
            pattern = "project-id:(" + Joiner.on('|').join(projectIds()) + ")";
        }
        return pattern;
    }

    @JsonCreator
    public static EventFilter create(@JsonProperty("id") final Long id,
                                     @JsonProperty("name") final String name,
                                     @JsonProperty("project-ids") final List<String> projectIds) {
        return builder()
                .id(id)
                .name(name)
                .projectIds(projectIds == null ? Collections.<String>emptyList() : projectIds)
                .build();
    }
}
