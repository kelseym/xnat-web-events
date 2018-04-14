package org.nrg.xnat.eventservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
public abstract class ActionAttributeConfiguration {

    @Nullable @JsonProperty("description")   public abstract String description();
    @Nullable @JsonProperty("type")          public abstract String type();
    @Nullable @JsonProperty("default-value") public abstract String defaultValue();
    @Nullable @JsonProperty("required")      public abstract Boolean required();
    @Nullable @JsonProperty("user-settable") public abstract Boolean userSettable();
    @Nullable @JsonProperty("restricted-to") public abstract List<String> restrictToValues();

    public static ActionAttributeConfiguration create(@JsonProperty("description")   String description,
                                                      @JsonProperty("type")          String type,
                                                      @JsonProperty("default-value") String defaultValue,
                                                      @JsonProperty("required")      Boolean required,
                                                      @JsonProperty("user-settable") Boolean userSettable,
                                                      @JsonProperty("restrict-to")   List<String> restrictToValues) {
        return builder()
                .description(description)
                .type(type)
                .defaultValue(defaultValue)
                .required(required)
                .userSettable(userSettable)
                .restrictToValues(restrictToValues)
                .build();
    }

    public static Builder builder() {return new AutoValue_ActionAttributeConfiguration.Builder();}

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder description(String description);

        public abstract Builder type(String type);

        public abstract Builder defaultValue(String defaultValue);

        public abstract Builder required(Boolean required);

        public abstract Builder userSettable(Boolean userSettable);

        public abstract Builder restrictToValues(List<String> restrictToValues);

        public abstract ActionAttributeConfiguration build();
    }
}
