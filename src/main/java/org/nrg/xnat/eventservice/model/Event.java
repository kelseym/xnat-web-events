package org.nrg.xnat.eventservice.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@Deprecated()
// Use SimpleEvent instead
@AutoValue
public abstract class Event {

    @JsonProperty("id") public abstract long id();
    @JsonProperty("class-name") public abstract String className();
    @JsonProperty("display-name") public abstract String displayName();
    @JsonProperty("description") public abstract String description();
    @JsonProperty("object") public abstract String object();
    @JsonProperty("object-class") public abstract String objectClass();
    @JsonProperty("operation") public abstract String operation();

 }
