package com.workflow.engine.node;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EndNodeHandler 单元测试
 */
class EndNodeHandlerTest {

    private final EndNodeHandler handler = new EndNodeHandler();

    @Test
    @DisplayName("获取节点类型")
    void getNodeType_shouldReturnEnd() {
        assertThat(handler.getNodeType()).isEqualTo("end");
    }

    @Test
    @DisplayName("执行结束节点 - 标记成功")
    void execute_shouldMarkSuccess() {
        // Given
        NodeJSON node = new NodeJSON("node-1", "end", null, null);
        WorkflowState state = WorkflowState.builder()
                .workflowId(1L)
                .status(WorkflowState.STATUS_RUNNING)
                .executionPath(java.util.List.of("start-node"))
                .build();

        // When
        WorkflowState result = handler.execute(node, state);

        // Then
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_SUCCESS);
    }

    @Test
    @DisplayName("执行结束节点 - 提取指定输出字段")
    void execute_withOutputFields_shouldExtractOutput() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("outputFields", java.util.List.of("result", "summary"));

        NodeJSON node = new NodeJSON("node-1", "end", null, data);

        WorkflowState state = WorkflowState.builder()
                .workflowId(1L)
                .status(WorkflowState.STATUS_RUNNING)
                .variables(Map.of("result", "final result", "summary", "execution summary"))
                .build();

        // When
        WorkflowState result = handler.execute(node, state);

        // Then
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_SUCCESS);
        assertThat(result.getOutput()).containsEntry("result", "final result");
        assertThat(result.getOutput()).containsEntry("summary", "execution summary");
    }

    @Test
    @DisplayName("验证节点配置 - 始终有效")
    void validate_shouldAlwaysReturnTrue() {
        NodeJSON node = new NodeJSON("node-1", "end", null, null);
        assertThat(handler.validate(node)).isTrue();
    }

    @Test
    @DisplayName("获取节点描述")
    void getDescription_shouldReturnDescription() {
        NodeJSON node = new NodeJSON("node-1", "end", null, null);
        assertThat(handler.getDescription(node)).isEqualTo("工作流终止节点");
    }
}