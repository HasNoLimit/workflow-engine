package com.workflow.engine.engine.model;

import java.util.Map;

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