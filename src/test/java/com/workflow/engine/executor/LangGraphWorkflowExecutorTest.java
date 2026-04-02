package com.workflow.engine.executor;

import com.workflow.engine.engine.model.EdgeJSON;
import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.engine.model.WorkflowJSON;
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
 * LangGraphWorkflowExecutor 单元测试
 * <p>
 * 测试基于 LangGraph4j StateGraph 的工作流执行器
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class LangGraphWorkflowExecutorTest {

    @Mock
    private ExecutionHook executionHook;

    @Mock
    private WorkflowCheckpointService checkpointService;

    private LangGraphWorkflowExecutor executor;
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
        executor = new LangGraphWorkflowExecutor(nodeHandlers, executionHook, conditionNodeHandler, checkpointService);
    }

    @Test
    @DisplayName("执行简单工作流 - 开始到结束（LangGraph4j）")
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
        ExecutionResult result = executor.execute(1L, canvas, input);

        // Then: 验证执行结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(WorkflowAgentState.STATUS_SUCCESS);
        assertThat(result.getExecutionPath()).containsExactly("node-1", "node-2");

        // 验证钩子被调用
        verify(executionHook).beforeWorkflowExecution(any(), any());
        verify(executionHook).afterWorkflowExecution(any(), any(), any());
    }

    @Test
    @DisplayName("执行包含 LLM 节点的工作流（LangGraph4j）")
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
        ExecutionResult result = executor.execute(1L, canvas, new HashMap<>());

        // Then: 验证执行结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExecutionPath()).containsExactly("node-1", "node-2", "node-3");
    }

    @Test
    @DisplayName("执行包含工具节点的工作流（LangGraph4j）")
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
        ExecutionResult result = executor.execute(1L, canvas, new HashMap<>());

        // Then: 验证执行结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExecutionPath()).containsExactly("node-1", "node-2", "node-3");
    }

    @Test
    @DisplayName("执行包含条件节点的工作流（LangGraph4j）")
    void execute_workflowWithConditionNode_shouldSucceed() {
        // Given: 创建工作流（start -> condition -> llm/end）
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "start", null, null));
        nodes.add(new NodeJSON("node-2", "condition", null,
                Map.of("conditions", List.of(
                        Map.of("expression", "$status == 'active'", "branch", "branch-true"),
                        Map.of("expression", "$status == 'inactive'", "branch", "branch-false")
                ))));
        nodes.add(new NodeJSON("node-3", "llm", null, Map.of("modelId", "gpt-4")));
        nodes.add(new NodeJSON("node-4", "end", null, null));

        // 条件节点的出边通过 sourcePortID 标识分支
        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));
        edges.add(new EdgeJSON("node-2", "node-3", "branch-true", null));
        edges.add(new EdgeJSON("node-2", "node-4", "branch-false", null));
        edges.add(new EdgeJSON("node-3", "node-4"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);

        // When: 执行工作流
        ExecutionResult result = executor.execute(1L, canvas, Map.of("status", "active"));

        // Then: 验证执行结果
        assertThat(result.isSuccess()).isTrue();
        // 条件节点会将分支结果存储到状态中
    }

    @Test
    @DisplayName("执行缺少开始节点的工作流 - 返回失败结果（LangGraph4j）")
    void execute_workflowWithoutStartNode_shouldReturnFailedResult() {
        // Given: 创建没有开始节点的工作流
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "llm", null, Map.of("modelId", "gpt-4")));
        nodes.add(new NodeJSON("node-2", "end", null, null));

        List<EdgeJSON> edges = new ArrayList<>();
        edges.add(new EdgeJSON("node-1", "node-2"));

        WorkflowJSON canvas = new WorkflowJSON(nodes, edges);

        // When: 执行工作流
        ExecutionResult result = executor.execute(1L, canvas, new HashMap<>());

        // Then: 验证返回失败结果
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(WorkflowAgentState.STATUS_FAILED);
        assertThat(result.getErrorMessage()).contains("缺少开始节点");
    }

    @Test
    @DisplayName("执行包含未知节点类型的工作流 - 返回失败结果（LangGraph4j）")
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
        ExecutionResult result = executor.execute(1L, canvas, new HashMap<>());

        // Then: 验证返回失败结果（LangGraph4j 会跳过未知节点，导致图不完整）
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("使用 WorkflowContext 执行工作流（LangGraph4j）")
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
        ExecutionResult result = executor.execute(context);

        // Then: 验证执行结果
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExecutionId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("异步执行工作流（LangGraph4j）")
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
        var future = executor.executeAsync(context);

        // Then: 验证结果
        ExecutionResult result = future.join();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("流式执行工作流（LangGraph4j）")
    void stream_shouldReturnNodeOutputs() {
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

        // When: 流式执行
        Iterable<org.bsc.langgraph4j.NodeOutput<WorkflowAgentState>> outputs = executor.stream(context);

        // Then: 验证输出
        int count = 0;
        for (var output : outputs) {
            count++;
            assertThat(output.node()).isNotNull();
            assertThat(output.state()).isNotNull();
        }
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("工作流执行失败时调用错误钩子（LangGraph4j）")
    void execute_whenFails_shouldCallErrorHook() {
        // Given: 创建会失败的工作流（缺少开始节点）
        List<NodeJSON> nodes = new ArrayList<>();
        nodes.add(new NodeJSON("node-1", "end", null, null));

        WorkflowJSON canvas = new WorkflowJSON(nodes, new ArrayList<>());

        // When: 执行工作流
        ExecutionResult result = executor.execute(1L, canvas, new HashMap<>());

        // Then: 验证执行失败并调用错误钩子
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(WorkflowAgentState.STATUS_FAILED);
        assertThat(result.getErrorMessage()).contains("缺少开始节点");

        verify(executionHook).onWorkflowError(any(), any(), any());
    }
}