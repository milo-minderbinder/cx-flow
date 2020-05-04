package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "ref",
        "node_id",
        "url",
        "object",
})
public class Ref {

    public enum Type {
        BLOB,
        TREE,
        COMMIT,
        TAG;

        @JsonValue
        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        public static Type fromString(String value) {
            for(Type t : Type.values()) {
                if (t.toString().equals(value)) {
                    return t;
                }
            }
            throw new IllegalArgumentException(String.format("Value does not map to a valid %s: %s",
                    Type.class.getSimpleName(), value));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "sha",
            "type",
            "url",
    })
    public static class Object {
        @JsonProperty("sha")
        private String sha;
        @JsonProperty("type")
        private Type type;
        @JsonProperty("url")
        private String url;
        @JsonIgnore
        private Map<String, java.lang.Object> additionalProperties = new HashMap<>();

        @JsonProperty("sha")
        public String getSha() {
            return sha;
        }

        @JsonProperty("sha")
        public void setSha(String sha) {
            this.sha = sha;
        }

        @JsonProperty("type")
        public Type getType() {
            return type;
        }

        @JsonProperty("type")
        public void setType(Type type) {
            this.type = type;
        }

        @JsonProperty("url")
        public String getUrl() {
            return url;
        }

        @JsonProperty("url")
        public void setUrl(String url) {
            this.url = url;
        }

        @JsonAnyGetter
        public Map<String, java.lang.Object> getAdditionalProperties() {
            return this.additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperty(String name, java.lang.Object value) {
            this.additionalProperties.put(name, value);
        }
    }

    @JsonProperty("ref")
    private String ref;
    @JsonProperty("node_id")
    private String nodeId;
    @JsonProperty("url")
    private String url;
    @JsonProperty("object")
    private Object object;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonProperty("ref")
    public String getRef() {
        return ref;
    }

    @JsonProperty("ref")
    public void setRef(String ref) {
        this.ref = ref;
    }

    @JsonProperty("node_id")
    public String getNodeId() {
        return nodeId;
    }

    @JsonProperty("node_id")
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("object")
    public Object getObject() {
        return object;
    }

    @JsonProperty("object")
    public void setObject(Object object) {
        this.object = object;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
