package com.workflow.engine.engine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EdgeJSON(
    @JsonProperty("sourceNodeID") String sourceNodeID,
    @JsonProperty("targetNodeID") String targetNodeID,
    @JsonProperty("sourcePortID") String sourcePortID,
    @JsonProperty("targetPortID") String targetPortID
) {
    public EdgeJSON(String sourceNodeID, String targetNodeID) {
        this(sourceNodeID, targetNodeID, null, null);
    }
}