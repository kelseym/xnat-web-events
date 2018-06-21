package org.nrg.xnat.eventservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.springframework.http.HttpHeaders;

import javax.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class InternalRestEndpoint {

    @Nullable @JsonProperty("id")           public abstract Long id();
    @JsonProperty("name")                   public abstract String name();
    @Nullable @JsonProperty("project-id")   public abstract String projectId();
    @Nullable @JsonProperty("active")       public abstract Boolean active();
    @JsonProperty("method")                 public abstract Method method();
    @JsonProperty("url")                    public abstract String url();
    @Nullable @JsonProperty("headers")      public abstract HttpHeaders httpHeaders();

    @JsonCreator
    public static InternalRestEndpoint create(@Nullable @JsonProperty("id")         Long id,
                                              @JsonProperty("name")                 String name,
                                              @Nullable @JsonProperty("project-id") String projectId,
                                              @Nullable @JsonProperty("active")     Boolean active,
                                              @JsonProperty("method")               Method method,
                                              @JsonProperty("url")                  String url,
                                              @Nullable @JsonProperty("headers")    HttpHeaders httpHeaders) {
        return builder()
                .id(id)
                .name(name)
                .projectId(projectId)
                .active(active)
                .method(method)
                .url(url)
                .httpHeaders(httpHeaders)
                .build();
    }

    public static InternalRestEndpoint.Builder builder() { return new AutoValue_InternalRestEndpoint.Builder(); }





    public enum Method {GET, POST, PUT, DELETE}

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(Long id);

        public abstract Builder name(String name);

        public abstract Builder projectId(String projectId);

        public abstract Builder active(Boolean active);

        public abstract Builder method(Method method);

        public abstract Builder url(String url);

        public abstract Builder httpHeaders(HttpHeaders httpHeaders);

        public abstract InternalRestEndpoint build();
    }
}
