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
}