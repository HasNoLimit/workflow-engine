package com.workflow.engine.engine.model;

import java.util.List;

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