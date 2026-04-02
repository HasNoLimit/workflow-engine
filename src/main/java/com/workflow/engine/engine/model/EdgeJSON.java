package com.workflow.engine.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 边 JSON 数据模型
 * <p>
 * 表示 FlowGram.AI 画布中节点之间的连接边
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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