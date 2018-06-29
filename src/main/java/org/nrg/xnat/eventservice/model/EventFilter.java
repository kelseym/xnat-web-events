package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.Filter;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class EventFilter {
    @JsonIgnore @Nullable @JsonProperty("id") public abstract Long id();
    @Nullable @JsonProperty("name") public abstract String name();

    // ** Reactor bus filter components ** //
    @JsonProperty("event-id")   public abstract String eventId();
    @Nullable @JsonProperty("project-ids") public abstract List<String> projectIds();
    @JsonProperty("status")     public abstract String status();

    // ** Post-detection filter components ** //
    @Nullable @JsonProperty("json-path-filter") public abstract String jsonPathFilter();
    @Nullable @JsonProperty("filter-nodes") public abstract Map<String, JsonPathFilterNode> nodeFilters();

    public static Builder builder() {
        return new AutoValue_EventFilter.Builder();
    }

    public abstract Builder toBuilder();

    public void populateJsonPathFilter() {
        // TODO: construct JSONPath filter from nodeFilters
    }

    // return filter to match serialized EventSignature object
    // {
    //   "event-id":"some.test.EventId",
    //   "project-ids":"ProjectId1",
    //   "status":"CREATED"
    // }
    public Filter buildReactorFilter(){
        if(projectIds() == null || projectIds().isEmpty() || (projectIds().size() == 1 && projectIds().get(0) == "")) {
            return Filter.filter(
                    Criteria.where("event-id").exists(true).and("event-id").is(eventId())
                            .and("status").exists(true).and("status").is(status()));
        } else {
            return Filter.filter(
                    Criteria.where("event-id").exists(true).and("event-id").is(eventId())
                            .and("status").exists(true).and("status").is(status())
                            .and("project-id").exists(true).and("project-id").in(projectIds()));
        }
    }

    public static EventFilter create(Long id, String eventId, List<String> projectIds, String status,
                                     String jsonPathFilter,
                                     Map<String, JsonPathFilterNode> nodeFilters) {
        return builder()
                .id(id)
                .eventId(eventId)
                .projectIds(projectIds)
                .status(status)
                .jsonPathFilter(jsonPathFilter)
                .nodeFilters(nodeFilters)
                .build();
    }

    public String toRegexMatcher(String eventType, String projectId) {
        String pattern = ".*(?:" + eventType + ")";
        pattern += "__";
        pattern += "project-id:";
        if (!Strings.isNullOrEmpty(projectId)) {
            pattern += ".*(?:" + projectId + ")";
        } else {
            pattern += ".*";
        }
        return pattern;
    }

    public String toRegexKey(String eventType, String projectId) {
        String pattern = "event-type:" + eventType;
        pattern += "__";
        pattern += "project-id:";
        if (!Strings.isNullOrEmpty(projectId)) {
            pattern += projectId;
        }
        return pattern;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(Long id);

        public abstract Builder name(String name);

        public abstract Builder eventId(String eventId);

        public abstract Builder projectIds(List<String> projectIds);

        public abstract Builder status(String status);

        public abstract Builder jsonPathFilter(String jsonPathFilter);

        public abstract Builder nodeFilters(Map<String, JsonPathFilterNode> nodeFilters);

        public abstract EventFilter build();
    }
}
