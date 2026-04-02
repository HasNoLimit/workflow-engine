package com.workflow.engine.executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WorkflowState 单元测试
 */
class WorkflowStateTest {

    @Test
    @DisplayName("创建 WorkflowState")
    void builder_shouldCreateState() {
        WorkflowState state = WorkflowState.builder()
                .workflowId(1L)
                .executionId(100L)
                .input(Map.of("key", "value"))
                .status(WorkflowState.STATUS_RUNNING)
                .build();

        assertThat(state.getWorkflowId()).isEqualTo(1L);
        assertThat(state.getExecutionId()).isEqualTo(100L);
        assertThat(state.getInput()).containsEntry("key", "value");
        assertThat(state.getStatus()).isEqualTo(WorkflowState.STATUS_RUNNING);
    }

    @Test
    @DisplayName("withStatus 更新状态")
    void withStatus_shouldUpdateStatus() {
        WorkflowState state = WorkflowState.builder()
                .workflowId(1L)
                .status(WorkflowState.STATUS_RUNNING)
                .build();

        WorkflowState newState = state.withStatus(WorkflowState.STATUS_SUCCESS);

        assertThat(newState.getStatus()).isEqualTo(WorkflowState.STATUS_SUCCESS);
        assertThat(newState.getWorkflowId()).isEqualTo(1L);
        // 验证不可变性：原状态不变
        assertThat(state.getStatus()).isEqualTo(WorkflowState.STATUS_RUNNING);
    }

    @Test
    @DisplayName("withCurrentNode 更新当前节点并添加执行路径")
    void withCurrentNode_shouldUpdateNodeAndPath() {
        WorkflowState state = WorkflowState.builder()
                .workflowId(1L)
                .executionPath(new java.util.ArrayList<>())
                .build();

        WorkflowState newState = state.withCurrentNode("node-1");

        assertThat(newState.getCurrentNodeId()).isEqualTo("node-1");
        assertThat(newState.getExecutionPath()).containsExactly("node-1");
    }

    @Test
    @DisplayName("withError 设置错误信息")
    void withError_shouldSetError() {
        WorkflowState state = WorkflowState.builder()
                .workflowId(1L)
                .status(WorkflowState.STATUS_RUNNING)
                .build();

        WorkflowState newState = state.withError("执行失败");

        assertThat(newState.getErrorMessage()).isEqualTo("执行失败");
        assertThat(newState.getStatus()).isEqualTo(WorkflowState.STATUS_FAILED);
    }

    @Test
    @DisplayName("setVariable 设置变量")
    void setVariable_shouldSetVariable() {
        WorkflowState state = WorkflowState.builder()
                .workflowId(1L)
                .variables(new HashMap<>())
                .build();

        WorkflowState newState = state.setVariable("key1", "value1");

        String variableValue = newState.getVariable("key1");
        assertThat(variableValue).isEqualTo("value1");
    }

    @Test
    @DisplayName("addChatMessage 添加对话消息")
    void addChatMessage_shouldAddMessage() {
        WorkflowState state = WorkflowState.builder()
                .workflowId(1L)
                .chatHistory(new java.util.ArrayList<>())
                .build();

        WorkflowState newState = state.addChatMessage("user", "你好");
        newState = newState.addChatMessage("assistant", "你好，有什么可以帮助你的？");

        assertThat(newState.getChatHistory()).hasSize(2);
        assertThat(newState.getChatHistory().get(0)).containsEntry("role", "user");
        assertThat(newState.getChatHistory().get(1)).containsEntry("role", "assistant");
    }

    @Test
    @DisplayName("getVariable 获取类型安全的变量值")
    void getVariable_shouldReturnTypedValue() {
        WorkflowState state = WorkflowState.builder()
                .variables(Map.of("stringVar", "text", "intVar", 42))
                .build();

        String stringValue = state.getVariable("stringVar");
        Integer intValue = state.getVariable("intVar");

        assertThat(stringValue).isEqualTo("text");
        assertThat(intValue).isEqualTo(42);
    }
}