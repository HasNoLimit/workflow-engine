package com.workflow.engine.engine.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowJSONTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeFlowGramAIJson() throws Exception {
        String json = """
            {
              "nodes": [
                {
                  "id": "start_0",
                  "type": "start",
                  "meta": { "position": { "x": 0, "y": 0 } },
                  "data": { "title": "Start" }
                },
                {
                  "id": "llm_0",
                  "type": "LLM",
                  "meta": { "position": { "x": 200, "y": 0 } },
                  "data": {
                    "modelId": "qwen-max",
                    "systemPrompt": "You are an assistant"
                  }
                }
              ],
              "edges": [
                { "sourceNodeID": "start_0", "targetNodeID": "llm_0" }
              ]
            }
            """;

        WorkflowJSON workflow = objectMapper.readValue(json, WorkflowJSON.class);

        assertNotNull(workflow);
        assertEquals(2, workflow.nodes().size());
        assertEquals(1, workflow.edges().size());
        assertEquals("start_0", workflow.nodes().get(0).id());
        assertEquals("LLM", workflow.nodes().get(1).type());
        assertEquals("start_0", workflow.edges().get(0).sourceNodeID());
    }

    @Test
    void shouldFindNodeById() throws Exception {
        String json = """
            {
              "nodes": [
                { "id": "start_0", "type": "start", "data": {} },
                { "id": "llm_0", "type": "LLM", "data": {} }
              ],
              "edges": []
            }
            """;

        WorkflowJSON workflow = objectMapper.readValue(json, WorkflowJSON.class);

        NodeJSON node = workflow.findNode("llm_0");
        assertNotNull(node);
        assertEquals("LLM", node.type());
    }

    @Test
    void shouldFindStartNode() throws Exception {
        String json = """
            {
              "nodes": [
                { "id": "start_0", "type": "start", "data": {} },
                { "id": "llm_0", "type": "LLM", "data": {} }
              ],
              "edges": []
            }
            """;

        WorkflowJSON workflow = objectMapper.readValue(json, WorkflowJSON.class);

        NodeJSON startNode = workflow.findStartNode();
        assertNotNull(startNode);
        assertEquals("start_0", startNode.id());
    }

    @Test
    void shouldFindEndNode() throws Exception {
        String json = """
            {
              "nodes": [
                { "id": "start_0", "type": "start", "data": {} },
                { "id": "llm_0", "type": "LLM", "data": {} },
                { "id": "end_0", "type": "end", "data": {} }
              ],
              "edges": []
            }
            """;

        WorkflowJSON workflow = objectMapper.readValue(json, WorkflowJSON.class);

        NodeJSON endNode = workflow.findEndNode();
        assertNotNull(endNode);
        assertEquals("end_0", endNode.id());
    }

    @Test
    void shouldDeserializeEdgeWithPorts() throws Exception {
        String json = """
            {
              "nodes": [
                { "id": "parallel_0", "type": "Parallel", "data": {} },
                { "id": "task_a", "type": "Task", "data": {} }
              ],
              "edges": [
                {
                  "sourceNodeID": "parallel_0",
                  "targetNodeID": "task_a",
                  "sourcePortID": "branch_0",
                  "targetPortID": "input"
                }
              ]
            }
            """;

        WorkflowJSON workflow = objectMapper.readValue(json, WorkflowJSON.class);

        EdgeJSON edge = workflow.edges().get(0);
        assertEquals("parallel_0", edge.sourceNodeID());
        assertEquals("task_a", edge.targetNodeID());
        assertEquals("branch_0", edge.sourcePortID());
        assertEquals("input", edge.targetPortID());
    }

    @Test
    void shouldGetNodeDataValues() throws Exception {
        String json = """
            {
              "nodes": [
                {
                  "id": "llm_0",
                  "type": "LLM",
                  "data": {
                    "modelId": "qwen-max",
                    "systemPrompt": "You are helpful",
                    "userPrompt": "Hello",
                    "toolId": "http_request"
                  }
                }
              ],
              "edges": []
            }
            """;

        WorkflowJSON workflow = objectMapper.readValue(json, WorkflowJSON.class);
        NodeJSON node = workflow.nodes().get(0);

        assertEquals("qwen-max", node.getModelId());
        assertEquals("You are helpful", node.getSystemPrompt());
        assertEquals("Hello", node.getUserPrompt());
        assertEquals("http_request", node.getToolId());
    }
}