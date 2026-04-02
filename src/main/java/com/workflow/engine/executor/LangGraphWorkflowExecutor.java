package com.workflow.engine.executor;

import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.action.EdgeAction;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

import com.workflow.engine.engine.model.EdgeJSON;
import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.engine.model.WorkflowJSON;
import com.workflow.engine.exception.WorkflowExecutionException;
import com.workflow.engine.hook.ExecutionHook;
import com.workflow.engine.node.ConditionNodeHandler;
import com.workflow.engine.node.NodeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 基于 LangGraph4j 的工作流执行器
 * <p>
 * 使用 StateGraph 构建工作流图，支持 Checkpoint 持久化、
 * 条件路由、Hook 拦截等 LangGraph4j 特性。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LangGraphWorkflowExecutor {

    /** 节点处理器列表（Spring 自动注入所有 NodeHandler 实现） */
    private final List<NodeHandler> nodeHandlers;

    /** 执行钩子 */
    private final ExecutionHook executionHook;

    /** 条件节点处理器（用于路由判断） */
    private final ConditionNodeHandler conditionNodeHandler;

    /** 检查点服务（可选，用于状态恢复） */
    private final WorkflowCheckpointService checkpointService;

    /**
     * 执行工作流
     * <p>
     * 构建 LangGraph4j StateGraph，编译并执行工作流。
     * </p>
     * @param workflowId 工作流ID
     * @param canvas 画布定义（FlowGram.AI JSON）
     * @param input 输入参数
     * @return 执行结果
     */
    public ExecutionResult execute(Long workflowId, WorkflowJSON canvas, Map<String, Object> input) {
        // 创建执行上下文
        WorkflowContext context = WorkflowContext.builder()
                .workflowId(workflowId)
                .canvas(canvas)
                .input(input)
                .startedAt(java.time.LocalDateTime.now())
                .build();

        return execute(context);
    }

    /**
     * 执行工作流（带完整上下文）
     * <p>
     * 使用 LangGraph4j StateGraph 构建并执行工作流，
     * 支持 Checkpoint 持久化和状态恢复。
     * </p>
     * @param context 执行上下文
     * @return 执行结果
     */
    public ExecutionResult execute(WorkflowContext context) {
        Long workflowId = context.getWorkflowId();
        WorkflowJSON canvas = context.getCanvas();
        Map<String, Object> input = context.getInput();

        log.info("开始执行工作流（LangGraph4j）: workflowId={}, nodes={}, edges={}",
                workflowId, canvas.nodes().size(), canvas.edges().size());

        long startTime = System.currentTimeMillis();

        // 工作流开始前钩子
        executionHook.beforeWorkflowExecution(workflowId, context);

        try {
            // 1. 查找开始节点
            NodeJSON startNode = canvas.findStartNode();
            if (startNode == null) {
                throw new WorkflowExecutionException("工作流缺少开始节点");
            }

            // 2. 构建节点处理器映射
            Map<String, NodeHandler> handlerMap = buildHandlerMap();

            // 3. 构建 StateGraph
            StateGraph<WorkflowAgentState> graph = buildStateGraph(canvas, handlerMap);

            // 4. 配置 Checkpoint
            CompileConfig compileConfig = buildCompileConfig(context);

            // 5. 编译图
            CompiledGraph<WorkflowAgentState> app = graph.compile(compileConfig);

            // 6. 创建初始状态
            WorkflowAgentState initialState = WorkflowAgentState.createInitialState(
                    workflowId,
                    context.getExecutionId(),
                    input
            );

            // 7. 创建运行配置
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(context.getOrCreateThreadId())
                    .build();

            // 8. 执行工作流（invoke 接受 Map，返回 Optional<WorkflowAgentState>）
            Optional<WorkflowAgentState> resultState = app.invoke(initialState.toMap(), runnableConfig);

            // 9. 构建执行结果
            WorkflowAgentState finalState = resultState.orElse(initialState);
            ExecutionResult result = buildResult(finalState, startTime);

            // 更新上下文完成时间
            context.setCompletedAt(java.time.LocalDateTime.now());

            // 工作流完成后钩子
            executionHook.afterWorkflowExecution(workflowId, result, context);

            log.info("工作流执行完成（LangGraph4j）: workflowId={}, success={}, durationMs={}",
                    workflowId, result.isSuccess(), result.getDurationMs());

            return result;

        } catch (Exception e) {
            log.error("工作流执行失败（LangGraph4j）: workflowId={}", workflowId, e);

            // 工作流错误钩子
            executionHook.onWorkflowError(workflowId, context, e);

            return ExecutionResult.builder()
                    .success(false)
                    .status(WorkflowAgentState.STATUS_FAILED)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 构建 LangGraph4j StateGraph
     * <p>
     * 根据 FlowGram.AI 画布定义构建状态图，
     * 包括节点、边和条件路由。
     * </p>
     * @param canvas 画布定义
     * @param handlerMap 节点处理器映射
     * @return StateGraph
     */
    private StateGraph<WorkflowAgentState> buildStateGraph(WorkflowJSON canvas, Map<String, NodeHandler> handlerMap) throws org.bsc.langgraph4j.GraphStateException {
        log.info("构建 StateGraph: nodes={}, edges={}", canvas.nodes().size(), canvas.edges().size());

        // 创建 StateGraph，使用 WorkflowAgentState 的构造函数
        StateGraph<WorkflowAgentState> graph = new StateGraph<>(WorkflowAgentState::new);

        // 添加所有节点
        for (NodeJSON node : canvas.nodes()) {
            NodeHandler handler = handlerMap.get(node.type());
            if (handler == null) {
                log.warn("未知的节点类型: type={}, nodeId={}", node.type(), node.id());
                continue;
            }

            // 验证节点配置
            if (!handler.validate(node)) {
                throw new WorkflowExecutionException("节点配置无效: " + node.id());
            }

            // 创建节点动作适配器并添加到图
            NodeActionAdapter adapter = new NodeActionAdapter(handler, node);
            graph.addNode(node.id(), node_async(adapter));

            log.debug("添加节点: nodeId={}, type={}", node.id(), node.type());
        }

        // 构建边的映射：sourceNodeId -> List<EdgeJSON>
        Map<String, List<EdgeJSON>> edgesBySource = new HashMap<>();
        for (EdgeJSON edge : canvas.edges()) {
            edgesBySource.computeIfAbsent(edge.sourceNodeID(), k -> new ArrayList<>()).add(edge);
        }

        // 添加边和条件边
        NodeJSON startNode = canvas.findStartNode();
        if (startNode != null) {
            // 设置入口点：START -> startNode
            graph.addEdge(START, startNode.id());
        }

        // 处理每个节点的出边
        for (NodeJSON node : canvas.nodes()) {
            List<EdgeJSON> outEdges = edgesBySource.get(node.id());
            if (outEdges == null || outEdges.isEmpty()) {
                // 没有出边的节点，直接连接到 END
                // 包括结束节点在内，所有没有出边的节点都连接到 END
                graph.addEdge(node.id(), END);
                log.debug("添加边到END: {} -> END", node.id());
                continue;
            }

            // 检查是否是条件节点
            if ("condition".equalsIgnoreCase(node.type())) {
                // 条件节点：添加条件边
                addConditionalEdges(graph, node, outEdges, canvas);
            } else {
                // 普通节点：添加普通边
                for (EdgeJSON edge : outEdges) {
                    graph.addEdge(node.id(), edge.targetNodeID());
                    log.debug("添加边: {} -> {}", edge.sourceNodeID(), edge.targetNodeID());
                }
            }
        }

        log.info("StateGraph 构建完成");
        return graph;
    }

    /**
     * 添加条件边
     * <p>
     * 条件节点根据状态中的分支结果决定路由方向。
     * </p>
     * @param graph 状态图
     * @param conditionNode 条件节点
     * @param outEdges 条件节点的出边列表
     * @param canvas 画布定义
     */
    private void addConditionalEdges(
            StateGraph<WorkflowAgentState> graph,
            NodeJSON conditionNode,
            List<EdgeJSON> outEdges,
            WorkflowJSON canvas) throws org.bsc.langgraph4j.GraphStateException {

        log.info("添加条件边: nodeId={}, branches={}", conditionNode.id(), outEdges.size());

        // 构建分支映射：branchId -> targetNodeId
        Map<String, String> branchMap = new HashMap<>();
        for (EdgeJSON edge : outEdges) {
            String branchId = edge.sourcePortID() != null ? edge.sourcePortID() : "default";
            branchMap.put(branchId, edge.targetNodeID());
        }
        // 添加默认结束分支
        branchMap.put(END, END);

        // 创建路由函数：根据状态中的条件分支结果决定下一个分支
        // 注意：返回的是 branchMap 的 key（分支ID），而不是节点ID
        EdgeAction<WorkflowAgentState> routingAction = state -> {
            // 从状态中获取条件分支结果
            String branch = state.conditionBranch().orElse("default");
            log.debug("条件路由: nodeId={}, branch={}", conditionNode.id(), branch);

            // 检查分支是否存在于映射中
            if (branchMap.containsKey(branch)) {
                return branch;
            }

            // 如果没有匹配的分支，查找默认分支
            if (branchMap.containsKey("default")) {
                return "default";
            }

            // 没有匹配的边，结束执行
            return END;
        };

        // 添加条件边
        graph.addConditionalEdges(conditionNode.id(), edge_async(routingAction), branchMap);
    }

    /**
     * 构建编译配置
     * <p>
     * 配置 Checkpoint Saver，用于状态持久化
     * </p>
     * @param context 执行上下文
     * @return 编译配置
     */
    private CompileConfig buildCompileConfig(WorkflowContext context) {
        CompileConfig.Builder configBuilder = CompileConfig.builder();

        // 如果启用检查点，配置持久化
        if (context.isCheckpointEnabled()) {
            // 使用内存保存器（用于短期状态恢复）
            MemorySaver memorySaver = new MemorySaver();
            configBuilder.checkpointSaver(memorySaver);

            // 如果有数据库持久化需求，可以使用自定义 saver
            // configBuilder.checkpointSaver(checkpointSaver);

            log.info("配置 Checkpoint: threadId={}", context.getOrCreateThreadId());
        }

        return configBuilder.build();
    }

    /**
     * 构建节点处理器映射
     * @return 类型 -> 处理器的映射
     */
    private Map<String, NodeHandler> buildHandlerMap() {
        Map<String, NodeHandler> map = new HashMap<>();
        for (NodeHandler handler : nodeHandlers) {
            map.put(handler.getNodeType(), handler);
        }
        return map;
    }

    /**
     * 构建执行结果
     * @param finalState 最终状态
     * @param startTime 开始时间
     * @return 执行结果
     */
    private ExecutionResult buildResult(WorkflowAgentState finalState, long startTime) {
        return ExecutionResult.builder()
                .success(WorkflowAgentState.STATUS_SUCCESS.equals(finalState.status().orElse(null)))
                .status(finalState.status().orElse(WorkflowAgentState.STATUS_FAILED))
                .output(finalState.output().orElse(new HashMap<>()))
                .executionPath(finalState.executionPath().orElse(new ArrayList<>()))
                .errorMessage(finalState.error().orElse(null))
                .executionId(finalState.executionId().orElse(null))
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * 异步执行工作流
     * @param context 执行上下文
     * @return 执行结果（异步）
     */
    public CompletableFuture<ExecutionResult> executeAsync(WorkflowContext context) {
        return CompletableFuture.supplyAsync(() -> execute(context));
    }

    /**
     * 流式执行工作流
     * <p>
     * 返回执行过程中的每个节点输出，用于实时监控
     * </p>
     * @param context 执行上下文
     * @return 流式输出迭代器
     */
    public Iterable<org.bsc.langgraph4j.NodeOutput<WorkflowAgentState>> stream(WorkflowContext context) {
        Long workflowId = context.getWorkflowId();
        WorkflowJSON canvas = context.getCanvas();
        Map<String, Object> input = context.getInput();

        log.info("流式执行工作流: workflowId={}", workflowId);

        try {
            // 1. 查找开始节点
            NodeJSON startNode = canvas.findStartNode();
            if (startNode == null) {
                throw new WorkflowExecutionException("工作流缺少开始节点");
            }

            // 2. 构建节点处理器映射
            Map<String, NodeHandler> handlerMap = buildHandlerMap();

            // 3. 构建 StateGraph
            StateGraph<WorkflowAgentState> graph = buildStateGraph(canvas, handlerMap);

            // 4. 编译图
            CompiledGraph<WorkflowAgentState> app = graph.compile();

            // 5. 创建初始状态
            WorkflowAgentState initialState = WorkflowAgentState.createInitialState(
                    workflowId,
                    context.getExecutionId(),
                    input
            );

            // 6. 流式执行
            return app.stream(initialState.toMap());

        } catch (Exception e) {
            log.error("流式执行失败: workflowId={}", workflowId, e);
            throw new WorkflowExecutionException("流式执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 恢复执行工作流
     * <p>
     * 从检查点恢复工作流执行，用于暂停/继续功能
     * </p>
     * @param threadId 线程ID
     * @param canvas 画布定义
     * @return 执行结果
     */
    public ExecutionResult resume(String threadId, WorkflowJSON canvas) {
        log.info("恢复工作流执行: threadId={}", threadId);

        try {
            // 1. 从数据库加载检查点
            Optional<WorkflowAgentState> savedStateOpt = checkpointService.loadFromDatabase(threadId);

            if (savedStateOpt.isEmpty()) {
                throw new WorkflowExecutionException("检查点不存在: " + threadId);
            }

            WorkflowAgentState savedState = savedStateOpt.get();

            // 2. 构建节点处理器映射
            Map<String, NodeHandler> handlerMap = buildHandlerMap();

            // 3. 构建 StateGraph
            StateGraph<WorkflowAgentState> graph = buildStateGraph(canvas, handlerMap);

            // 4. 配置 Checkpoint（使用内存保存器）
            CompileConfig compileConfig = CompileConfig.builder()
                    .checkpointSaver(checkpointService.getMemorySaver())
                    .build();

            // 5. 编译图
            CompiledGraph<WorkflowAgentState> app = graph.compile(compileConfig);

            // 6. 创建运行配置
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();

            // 7. 继续执行
            Optional<WorkflowAgentState> resultState = app.invoke(savedState.toMap(), runnableConfig);

            // 8. 构建结果
            WorkflowAgentState finalState = resultState.orElse(savedState);
            return buildResult(finalState, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("恢复执行失败: threadId={}", threadId, e);
            return ExecutionResult.builder()
                    .success(false)
                    .status(WorkflowAgentState.STATUS_FAILED)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}