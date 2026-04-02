package com.workflow.engine.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 工作流 JSON 数据模型
 * <p>
 * 表示 FlowGram.AI 画布的完整数据结构，包含节点和边
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowJSON(
    List<NodeJSON> nodes,
    List<EdgeJSON> edges
) {
    public NodeJSON findNode(String nodeId) {
        if (nodes == null) return null;
        return nodes.stream()
            .filter(n -> nodeId.equals(n.id()))
            .findFirst()
            .orElse(null);
    }

    public NodeJSON findStartNode() {
        if (nodes == null) return null;
        return nodes.stream()
            .filter(n -> "start".equalsIgnoreCase(n.type()))
            .findFirst()
            .orElse(null);
    }

    public NodeJSON findEndNode() {
        if (nodes == null) return null;
        return nodes.stream()
            .filter(n -> "end".equalsIgnoreCase(n.type()))
            .findFirst()
            .orElse(null);
    }
}