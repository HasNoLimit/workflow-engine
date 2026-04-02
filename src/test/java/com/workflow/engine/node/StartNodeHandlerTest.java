package com.workflow.engine.node;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StartNodeHandler 单元测试
 */
class StartNodeHandlerTest {

    private final StartNodeHandler handler = new StartNodeHandler();

    @Test
    @DisplayName("获取节点类型")
    void getNodeType_shouldReturnStart() {
        assertThat(handler.getNodeType()).isEqualTo("start");
    }

    @Test
    @DisplayName("执行开始节点 - 初始化状态")
    void execute_shouldInitializeState() {
        // Given
        NodeJSON node = new NodeJSON("node-1", "start", null, null);
        WorkflowState state = WorkflowState.builder()
                .workflowId(1L)
                .input(Map.of("param1", "value1", "param2", "value2"))
                .status("PENDING")
                .build();

        // When
        WorkflowState result = handler.execute(node, state);

        // Then
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_RUNNING);
        String param1Value = result.getVariable("param1");
        String param2Value = result.getVariable("param2");
        assertThat(param1Value).isEqualTo("value1");
        assertThat(param2Value).isEqualTo("value2");
    }

    @Test
    @DisplayName("执行开始节点 - 空输入")
    void execute_withEmptyInput_shouldStillInitialize() {
        // Given
        NodeJSON node = new NodeJSON("node-1", "start", null, null);
        WorkflowState state = WorkflowState.builder()
                .workflowId(1L)
                .input(new HashMap<>())
                .build();

        // When
        WorkflowState result = handler.execute(node, state);

        // Then
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_RUNNING);
    }

    @Test
    @DisplayName("验证节点配置 - 始终有效")
    void validate_shouldAlwaysReturnTrue() {
        NodeJSON node = new NodeJSON("node-1", "start", null, null);
        assertThat(handler.validate(node)).isTrue();
    }

    @Test
    @DisplayName("获取节点描述")
    void getDescription_shouldReturnDescription() {
        NodeJSON node = new NodeJSON("node-1", "start", null, null);
        assertThat(handler.getDescription(node)).isEqualTo("工作流入口节点");
    }
}