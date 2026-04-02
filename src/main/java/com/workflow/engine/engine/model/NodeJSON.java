package com.workflow.engine.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * 节点 JSON 数据模型
 * <p>
 * 表示 FlowGram.AI 画布中的单个节点
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NodeJSON(
    String id,
    String type,
    Map<String, Object> meta,
    Map<String, Object> data
) {
    @SuppressWarnings("unchecked")
    public <T> T getDataValue(String key) {
        if (data == null) return null;
        return (T) data.get(key);
    }

    public String getModelId() {
        return getDataValue("modelId");
    }

    public String getSystemPrompt() {
        return getDataValue("systemPrompt");
    }

    public String getUserPrompt() {
        return getDataValue("userPrompt");
    }

    public String getToolId() {
        return getDataValue("toolId");
    }
}