package com.workflow.engine.executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WorkflowAgentState 单元测试
 * <p>
 * 测试 LangGraph4j AgentState 的工作流适配
 * </p>
 */
class WorkflowAgentStateTest {

    @Test
    @DisplayName("创建初始状态")
    void createInitialState_shouldContainAllFields() {
        // Given: 工作流ID、执行ID和输入数据
        Long workflowId = 1L;
        Long executionId = 100L;
        Map<String, Object> input = Map.of("param1", "value1", "param2", "value2");

        // When: 创建初始状态
        WorkflowAgentState state = WorkflowAgentState.createInitialState(workflowId, executionId, input);

        // Then: 验证状态字段
        assertThat(state.workflowId()).isPresent().contains(workflowId);
        assertThat(state.executionId()).isPresent().contains(executionId);
        assertThat(state.input()).isPresent().contains(input);
        assertThat(state.status()).isPresent().contains(WorkflowAgentState.STATUS_RUNNING);
        assertThat(state.executionPath()).isPresent();
        assertThat(state.chatHistory()).isPresent();
        assertThat(state.variables()).isPresent();

        // 验证输入数据已添加到变量存储
        assertThat(state.getVariable("param1")).isPresent().contains("value1");
        assertThat(state.getVariable("param2")).isPresent().contains("value2");
    }

    @Test
    @DisplayName("状态数据继承 AgentState")
    void state_shouldExtendAgentState() {
        // Given: 创建状态
        WorkflowAgentState state = WorkflowAgentState.createInitialState(1L, 100L, new HashMap<>());

        // Then: 验证继承 AgentState
        assertThat(state).isInstanceOf(org.bsc.langgraph4j.state.AgentState.class);
        assertThat(state.data()).isNotEmpty();
    }

    @Test
    @DisplayName("Schema 定义正确")
    void schema_shouldContainListChannels() {
        // Then: 验证 Schema 定义（只有追加模式的列表字段）
        assertThat(WorkflowAgentState.SCHEMA).containsKeys(
                WorkflowAgentState.CHAT_HISTORY,
                WorkflowAgentState.EXECUTION_PATH
        );
    }

    @Test
    @DisplayName("状态字段常量定义正确")
    void constants_shouldBeCorrect() {
        // Then: 验证常量定义
        assertThat(WorkflowAgentState.WORKFLOW_ID).isEqualTo("workflowId");
        assertThat(WorkflowAgentState.EXECUTION_ID).isEqualTo("executionId");
        assertThat(WorkflowAgentState.CURRENT_NODE).isEqualTo("currentNode");
        assertThat(WorkflowAgentState.STATUS).isEqualTo("status");
        assertThat(WorkflowAgentState.ERROR).isEqualTo("error");
        assertThat(WorkflowAgentState.INPUT).isEqualTo("input");
        assertThat(WorkflowAgentState.OUTPUT).isEqualTo("output");
        assertThat(WorkflowAgentState.CHAT_HISTORY).isEqualTo("chatHistory");
        assertThat(WorkflowAgentState.EXECUTION_PATH).isEqualTo("executionPath");
        assertThat(WorkflowAgentState.VARIABLES).isEqualTo("variables");
        assertThat(WorkflowAgentState.CONDITION_BRANCH).isEqualTo("conditionBranch");

        // 验证状态常量
        assertThat(WorkflowAgentState.STATUS_RUNNING).isEqualTo("RUNNING");
        assertThat(WorkflowAgentState.STATUS_SUCCESS).isEqualTo("SUCCESS");
        assertThat(WorkflowAgentState.STATUS_FAILED).isEqualTo("FAILED");
        assertThat(WorkflowAgentState.STATUS_PAUSED).isEqualTo("PAUSED");
    }

    @Test
    @DisplayName("空输入数据创建状态")
    void createInitialState_withNullInput_shouldSucceed() {
        // When: 创建空输入状态
        WorkflowAgentState state = WorkflowAgentState.createInitialState(1L, 100L, null);

        // Then: 验证空输入处理
        assertThat(state.input()).isPresent();
        assertThat(state.input().get()).isEmpty();
        assertThat(state.variables()).isPresent();
        assertThat(state.variables().get()).isEmpty();
    }

    @Test
    @DisplayName("创建初始状态数据 Map")
    void createInitialData_shouldReturnValidMap() {
        // Given: 工作流参数
        Long workflowId = 1L;
        Long executionId = 100L;
        Map<String, Object> input = Map.of("key", "value");

        // When: 创建初始状态数据
        Map<String, Object> data = WorkflowAgentState.createInitialData(workflowId, executionId, input);

        // Then: 验证返回的数据
        assertThat(data).containsEntry(WorkflowAgentState.WORKFLOW_ID, workflowId);
        assertThat(data).containsEntry(WorkflowAgentState.EXECUTION_ID, executionId);
        assertThat(data).containsEntry(WorkflowAgentState.INPUT, input);
        assertThat(data).containsEntry(WorkflowAgentState.STATUS, WorkflowAgentState.STATUS_RUNNING);
        assertThat(data).containsKey(WorkflowAgentState.EXECUTION_PATH);
        assertThat(data).containsKey(WorkflowAgentState.CHAT_HISTORY);
    }

    @Test
    @DisplayName("toMap 返回状态数据")
    void toMap_shouldReturnStateData() {
        // Given: 创建状态
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("testKey", "testValue");
        WorkflowAgentState state = WorkflowAgentState.createInitialState(1L, 100L, inputData);

        // When: 获取状态数据 Map
        Map<String, Object> data = state.toMap();

        // Then: 验证数据
        assertThat(data).isNotNull();
        assertThat(data.get(WorkflowAgentState.WORKFLOW_ID)).isEqualTo(1L);
    }

    @Test
    @DisplayName("从 Map 创建状态")
    void createFromMap_shouldContainAllData() {
        // Given: 状态数据
        Map<String, Object> data = new HashMap<>();
        data.put(WorkflowAgentState.WORKFLOW_ID, 2L);
        data.put(WorkflowAgentState.EXECUTION_ID, 200L);
        data.put(WorkflowAgentState.STATUS, WorkflowAgentState.STATUS_SUCCESS);
        data.put(WorkflowAgentState.OUTPUT, Map.of("result", "done"));

        // When: 创建状态
        WorkflowAgentState state = new WorkflowAgentState(data);

        // Then: 验证状态
        assertThat(state.workflowId()).isPresent().contains(2L);
        assertThat(state.executionId()).isPresent().contains(200L);
        assertThat(state.status()).isPresent().contains(WorkflowAgentState.STATUS_SUCCESS);
        assertThat(state.output()).isPresent();
        assertThat(state.output().get()).containsEntry("result", "done");
    }

    @Test
    @DisplayName("访问不存在的字段返回空 Optional")
    void accessNonExistentField_shouldReturnEmpty() {
        // Given: 空状态
        WorkflowAgentState state = new WorkflowAgentState(new HashMap<>());

        // Then: 验证不存在的字段返回空
        assertThat(state.workflowId()).isEmpty();
        assertThat(state.error()).isEmpty();
        assertThat(state.getVariable("nonexistent")).isEmpty();
    }
}