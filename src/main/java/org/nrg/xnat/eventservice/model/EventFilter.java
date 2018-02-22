package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

@AutoValue
public abstract class EventFilter {

    @JsonIgnore @Nullable @JsonProperty("id") public abstract Long id();
    @Nullable @JsonProperty("name") public abstract String name();
    @Nullable @JsonProperty("json-path-filter") public abstract String jsonPathFilter();

    public static Builder builder() {
        return new AutoValue_EventFilter.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(Long id);

        public abstract Builder jsonPathFilter(String jsonPathFilter);

        public abstract Builder name(String name);

        public abstract EventFilter build();


    }


    public String toRegexMatcher(String eventType, List<String> projectIds) {
        String pattern = ".*(?:" + eventType + ")";
        pattern += "__";
        pattern += "PROJECT-IDS";
        if (projectIds == null || projectIds.isEmpty() || Strings.isNullOrEmpty(projectIds.get(0))) {
            pattern += ".*";
        } else {
            pattern += "(";
            for(ListIterator<String> iter = projectIds.listIterator(); iter.hasNext();) {
                String projectId = iter.next();
                pattern += ":" + projectId + ":";
                if(iter.hasNext()){
                    pattern += "|";
                }
            }
            pattern += ")";
        }
        return pattern;
    }

    // ** Expected regexKey format is "EVENT-TYPE:eventtypevalue__PROJECT-IDS:projectid1:projectid2:projectid3 ** //
    // ** e.g. "EVENT-TYPE:org.nrg.xnat.eventservice.events.ProjectCreatedEvent__PROJECT-IDS:TestProject1:TestProject2:TestProject3
    public String toRegexKey(String eventType, List<String> projectIds) {
        String pattern = "EVENT-TYPE:" + eventType;
        pattern += "__";
        pattern += "PROJECT-IDS:";
        for(ListIterator<String> iter = projectIds.listIterator(); iter.hasNext();) {
            String projectId = iter.next();
            if (!Strings.isNullOrEmpty(projectId)) {
                pattern += projectId + ":";
            }
        }
        return pattern;
    }

    // ** Expected regexKey format is "EVENT-TYPE:eventtypevalue__PROJECT-IDS:projectid1: ** //
    // ** e.g. "EVENT-TYPE:org.nrg.xnat.eventservice.events.ProjectCreatedEvent__PROJECT-IDS:TestProject1:
    public String toRegexKey(String eventType, String projectId) {
        return toRegexKey(eventType, Arrays.asList(projectId));
    }

    @JsonCreator
    public static EventFilter create(@JsonProperty("id") final Long id,
                                     @JsonProperty("name") final String name,
                                     @JsonProperty("json-path-filter") final String jsonPathFilter) {
        return builder()
                .id(id)
                .name(name)
                .jsonPathFilter(jsonPathFilter)
                .build();
    }
}
