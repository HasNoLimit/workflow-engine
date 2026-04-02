package com.workflow.engine.executor;

import com.workflow.engine.engine.model.EdgeJSON;
import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.engine.model.WorkflowJSON;
import com.workflow.engine.exception.WorkflowExecutionException;
import com.workflow.engine.hook.ExecutionHook;
import com.workflow.engine.node.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WorkflowExecutor 单元测试
 */
@ExtendWith(MockitoExtension.class)
class WorkflowExecutorTest {

    @Mock
    private ExecutionHook executionHook;

    private WorkflowExecutor workflowExecutor;
    private List<NodeHandler> nodeHandlers;

    @BeforeEach
    void setUp() {
        // 创建节点处理器列表
        nodeHandlers = new ArrayList<>();
        nodeHandlers.add(new StartNodeHandler());
        nodeHandlers.add(new EndNodeHandler());
        nodeHandlers.add(new LLMNodeHandler());
        nodeHandlers.add(new ToolNodeHandler());
        nodeHandlers.add(new ConditionNodeHandler());
        nodeHandlers.add(new ParallelNodeHandler());

        ConditionNodeHandler conditionNodeHandler = new ConditionNodeHandler();
        workflowExecutor = new WorkflowExecutor(nodeHandlers, executionHook, conditionNodeHandler);
    }

    @Test
    @DisplayName("执行简单工作流 - 开始到结束")
    void execute_simpleWorkflow_shouldSucceed() {
        // Given: 创建一个简单的工作流（start -> end）
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "start", null, null));
        nodes.add(new NodeJSON("node-2", "end", null, null));

        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);
        Map<String, Object> input = Map.of("param1", "value1");

        // When: 执行工作流
        ExecutionResult result = workflowExecutor.execute(1L, canvas, input);

        // Then: 验证执行结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_SUCCESS);
        assertThat(result.getExecutionPath()).containsExactly("node-1", "node-2");

        // 验证钩子被调用
        verify(executionHook).beforeWorkflowExecution(any(), any());
        verify(executionHook).afterWorkflowExecution(any(), any(), any());
        verify(executionHook, times(2)).beforeNodeExecution(any(), any());
        verify(executionHook, times(2)).afterNodeExecution(any(), any());
    }

    @Test
    @DisplayName("执行包含 LLM 节点的工作流")
    void execute_workflowWithLLMNode_shouldSucceed() {
        // Given: 创建工作流（start -> llm -> end）
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "start", null, null));
        nodes.add(new NodeJSON("node-2", "llm", null,
                Map.of("modelId", "gpt-4", "systemPrompt", "测试提示词")));
        nodes.add(new NodeJSON("node-3", "end", null, null));

        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));
        edges.add(new EdgeJSON("node-2", "node-3"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);

        // When: 执行工作流
        ExecutionResult result = workflowExecutor.execute(1L, canvas, new HashMap<>());

        // Then: 验证执行结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExecutionPath()).containsExactly("node-1", "node-2", "node-3");
    }

    @Test
    @DisplayName("执行包含工具节点的工作流")
    void execute_workflowWithToolNode_shouldSucceed() {
        // Given: 创建工作流（start -> tool -> end）
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "start", null, null));
        nodes.add(new NodeJSON("node-2", "tool", null,
                Map.of("toolId", "weather-api", "params", Map.of("city", "Beijing"))));
        nodes.add(new NodeJSON("node-3", "end", null, null));

        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));
        edges.add(new EdgeJSON("node-2", "node-3"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);

        // When: 执行工作流
        ExecutionResult result = workflowExecutor.execute(1L, canvas, new HashMap<>());

        // Then: 验证执行结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExecutionPath()).containsExactly("node-1", "node-2", "node-3");
    }

    @Test
    @DisplayName("执行缺少开始节点的工作流 - 返回失败结果")
    void execute_workflowWithoutStartNode_shouldReturnFailedResult() {
        // Given: 创建没有开始节点的工作流
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "llm", null, Map.of("modelId", "gpt-4")));
        nodes.add(new NodeJSON("node-2", "end", null, null));

        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);

        // When: 执行工作流
        ExecutionResult result = workflowExecutor.execute(1L, canvas, new HashMap<>());

        // Then: 验证返回失败结果
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_FAILED);
        assertThat(result.getErrorMessage()).contains("缺少开始节点");
    }

    @Test
    @DisplayName("执行包含未知节点类型的工作流 - 返回失败结果")
    void execute_workflowWithUnknownNodeType_shouldReturnFailedResult() {
        // Given: 创建包含未知节点类型的工作流
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "start", null, null));
        nodes.add(new NodeJSON("node-2", "unknown-type", null, null));
        nodes.add(new NodeJSON("node-3", "end", null, null));

        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));
        edges.add(new EdgeJSON("node-2", "node-3"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);

        // When: 执行工作流
        ExecutionResult result = workflowExecutor.execute(1L, canvas, new HashMap<>());

        // Then: 验证返回失败结果
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_FAILED);
        assertThat(result.getErrorMessage()).contains("未知的节点类型");
    }

    @Test
    @DisplayName("执行包含 LLM 节点但缺少模型配置的工作流 - 返回失败结果")
    void execute_llmNodeWithoutModelConfig_shouldReturnFailedResult() {
        // Given: 创建 LLM 节点缺少模型配置的工作流
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "start", null, null));
        nodes.add(new NodeJSON("node-2", "llm", null, new HashMap<>())); // 缺少 modelId
        nodes.add(new NodeJSON("node-3", "end", null, null));

        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));
        edges.add(new EdgeJSON("node-2", "node-3"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);

        // When: 执行工作流
        ExecutionResult result = workflowExecutor.execute(1L, canvas, new HashMap<>());

        // Then: 验证返回失败结果
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_FAILED);
        assertThat(result.getErrorMessage()).contains("节点配置无效");
    }

    @Test
    @DisplayName("使用 WorkflowContext 执行工作流")
    void execute_withContext_shouldSucceed() {
        // Given: 创建工作流和上下文
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "start", null, null));
        nodes.add(new NodeJSON("node-2", "end", null, null));

        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);

        WorkflowContext context = WorkflowContext.builder()
                .workflowId(1L)
                .canvas(canvas)
                .input(Map.of("key", "value"))
                .executionId(100L)
                .build();

        // When: 执行工作流
        ExecutionResult result = workflowExecutor.execute(context);

        // Then: 验证执行结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExecutionId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("异步执行工作流")
    void executeAsync_shouldReturnCompletableFuture() {
        // Given: 创建简单工作流
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "start", null, null));
        nodes.add(new NodeJSON("node-2", "end", null, null));

        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);

        WorkflowContext context = WorkflowContext.builder()
                .workflowId(1L)
                .canvas(canvas)
                .input(new HashMap<>())
                .build();

        // When: 异步执行
        var future = workflowExecutor.executeAsync(context);

        // Then: 验证结果
        ExecutionResult result = future.join();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("工作流执行失败时调用错误钩子")
    void execute_whenFails_shouldCallErrorHook() {
        // Given: 创建会失败的工作流（缺少开始节点）
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "end", null, null));

        WorkflowJSON canvas = new WorkflowJSON(nodes, new ArrayList<>());

        // When: 执行工作流
        ExecutionResult result = workflowExecutor.execute(1L, canvas, new HashMap<>());

        // Then: 验证执行失败并调用错误钩子
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_FAILED);
        assertThat(result.getErrorMessage()).contains("缺少开始节点");

        verify(executionHook).onWorkflowError(any(), any(), any());
    }

    @Test
    @DisplayName("构建 StateGraph - 当前返回 false（待完善）")
    void buildStateGraph_shouldReturnFalseForNow() {
        // Given: 创建工作流
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "start", null, null));
        nodes.add(new NodeJSON("node-2", "end", null, null));

        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);

        // When: 构建 StateGraph
        boolean result = workflowExecutor.buildStateGraph(canvas);

        // Then: 当前返回 false，待完善 LangGraph4j 集成
        assertThat(result).isFalse();
    }
}