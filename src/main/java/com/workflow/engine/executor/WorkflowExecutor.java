package com.workflow.engine.executor;

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
 * 工作流执行器
 * <p>
 * 基于 LangGraph4j StateGraph 构建工作流执行引擎。
 * 支持 Hook 拦截和 Checkpoint 持久化。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowExecutor {

    /** 节点处理器列表（Spring 自动注入所有 NodeHandler 实现） */
    private final List<NodeHandler> nodeHandlers;

    /** 执行钩子 */
    private final ExecutionHook executionHook;

    /** 条件节点处理器（用于路由判断） */
    private final ConditionNodeHandler conditionNodeHandler;

    /**
     * 执行工作流
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
     * @param context 执行上下文
     * @return 执行结果
     */
    public ExecutionResult execute(WorkflowContext context) {
        Long workflowId = context.getWorkflowId();
        WorkflowJSON canvas = context.getCanvas();
        Map<String, Object> input = context.getInput();

        log.info("开始执行工作流: workflowId={}, nodes={}, edges={}",
                workflowId, canvas.nodes().size(), canvas.edges().size());

        long startTime = System.currentTimeMillis();

        // 工作流开始前钩子
        executionHook.beforeWorkflowExecution(workflowId, context);

        try {
            // 1. 初始化状态
            WorkflowState initialState = WorkflowState.builder()
                    .workflowId(workflowId)
                    .executionId(context.getExecutionId())
                    .input(input != null ? input : new HashMap<>())
                    .status(WorkflowState.STATUS_RUNNING)
                    .executionPath(new ArrayList<>())
                    .chatHistory(new ArrayList<>())
                    .variables(new HashMap<>())
                    .build();

            // 2. 查找开始节点
            NodeJSON startNode = canvas.findStartNode();
            if (startNode == null) {
                throw new WorkflowExecutionException("工作流缺少开始节点");
            }

            // 3. 构建节点处理器映射
            Map<String, NodeHandler> handlerMap = buildHandlerMap();

            // 4. 执行工作流
            WorkflowState finalState = executeFromNode(canvas, startNode, initialState, handlerMap);

            // 5. 构建执行结果
            long endTime = System.currentTimeMillis();
            ExecutionResult result = ExecutionResult.builder()
                    .success(WorkflowState.STATUS_SUCCESS.equals(finalState.getStatus()))
                    .status(finalState.getStatus())
                    .output(finalState.getOutput())
                    .executionPath(finalState.getExecutionPath())
                    .errorMessage(finalState.getErrorMessage())
                    .executionId(context.getExecutionId())
                    .durationMs(endTime - startTime)
                    .build();

            // 更新上下文完成时间
            context.setCompletedAt(java.time.LocalDateTime.now());

            // 工作流完成后钩子
            executionHook.afterWorkflowExecution(workflowId, result, context);

            log.info("工作流执行完成: workflowId={}, success={}, durationMs={}",
                    workflowId, result.isSuccess(), result.getDurationMs());

            return result;

        } catch (Exception e) {
            log.error("工作流执行失败: workflowId={}", workflowId, e);

            // 工作流错误钩子
            executionHook.onWorkflowError(workflowId, context, e);

            return ExecutionResult.builder()
                    .success(false)
                    .status(WorkflowState.STATUS_FAILED)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 从指定节点开始执行（递归执行整个工作流）
     * @param canvas 画布定义
     * @param currentNode 当前节点
     * @param state 当前状态
     * @param handlerMap 节点处理器映射
     * @return 最终状态
     */
    private WorkflowState executeFromNode(
            WorkflowJSON canvas,
            NodeJSON currentNode,
            WorkflowState state,
            Map<String, NodeHandler> handlerMap) {

        // 记录节点开始时间
        long nodeStartTime = System.currentTimeMillis();

        // 1. 执行前钩子
        executionHook.beforeNodeExecution(currentNode, state);

        // 2. 获取节点处理器
        NodeHandler handler = handlerMap.get(currentNode.type());
        if (handler == null) {
            throw new WorkflowExecutionException("未知的节点类型: " + currentNode.type());
        }

        // 3. 验证节点配置
        if (!handler.validate(currentNode)) {
            throw new WorkflowExecutionException("节点配置无效: " + currentNode.id());
        }

        try {
            // 4. 执行节点
            WorkflowState newState = handler.execute(currentNode, state);
            newState = newState.withCurrentNode(currentNode.id());

            // 5. 执行后钩子
            executionHook.afterNodeExecution(currentNode, newState);

            // 6. 检查是否结束节点
            if ("end".equalsIgnoreCase(currentNode.type())) {
                return newState;
            }

            // 7. 查找下一个节点
            NodeJSON nextNode = findNextNode(canvas, currentNode, newState);

            if (nextNode == null) {
                // 没有后续节点，标记完成
                log.info("工作流节点链结束: currentNodeId={}", currentNode.id());
                return newState.withStatus(WorkflowState.STATUS_SUCCESS);
            }

            // 8. 递归执行下一个节点
            return executeFromNode(canvas, nextNode, newState, handlerMap);

        } catch (Exception e) {
            // 节点执行错误钩子
            executionHook.onNodeError(currentNode, state, e);

            throw new WorkflowExecutionException(
                    "节点执行失败: nodeId=" + currentNode.id() + ", error=" + e.getMessage(), e);
        }
    }

    /**
     * 查找下一个节点
     * <p>
     * 根据边的定义和条件节点状态确定下一个要执行的节点
     * </p>
     * @param canvas 画布定义
     * @param currentNode 当前节点
     * @param state 当前状态
     * @return 下一个节点，如果没有则返回 null
     */
    private NodeJSON findNextNode(WorkflowJSON canvas, NodeJSON currentNode, WorkflowState state) {
        // 如果是条件节点，需要根据条件结果路由
        if ("condition".equalsIgnoreCase(currentNode.type())) {
            return findNextNodeForCondition(canvas, currentNode, state);
        }

        // 普通节点：查找第一条出边
        for (EdgeJSON edge : canvas.edges()) {
            if (edge.sourceNodeID().equals(currentNode.id())) {
                return canvas.findNode(edge.targetNodeID());
            }
        }

        return null;
    }

    /**
     * 为条件节点查找下一个节点
     * @param canvas 画布定义
     * @param currentNode 条件节点
     * @param state 当前状态
     * @return 下一个节点
     */
    private NodeJSON findNextNodeForCondition(WorkflowJSON canvas, NodeJSON currentNode, WorkflowState state) {
        // 获取条件节点评估的分支
        String matchedBranch = conditionNodeHandler.getRoutingPort(currentNode, state);

        log.info("条件节点路由: nodeId={}, matchedBranch={}", currentNode.id(), matchedBranch);

        // 根据端口匹配查找目标节点
        for (EdgeJSON edge : canvas.edges()) {
            if (edge.sourceNodeID().equals(currentNode.id())) {
                // 条件节点的出边通过 sourcePortID 标识分支
                if (matchedBranch.equals(edge.sourcePortID())) {
                    return canvas.findNode(edge.targetNodeID());
                }
            }
        }

        // 如果没有匹配的分支，查找默认边（无端口或端口为空）
        for (EdgeJSON edge : canvas.edges()) {
            if (edge.sourceNodeID().equals(currentNode.id())) {
                if (edge.sourcePortID() == null || edge.sourcePortID().isEmpty()) {
                    return canvas.findNode(edge.targetNodeID());
                }
            }
        }

        return null;
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
     * 异步执行工作流
     * @param context 执行上下文
     * @return 执行结果（异步）
     */
    public CompletableFuture<ExecutionResult> executeAsync(WorkflowContext context) {
        return CompletableFuture.supplyAsync(() -> execute(context));
    }

    /**
     * 使用 LangGraph4j StateGraph 构建工作流图
     * <p>
     * 此方法用于创建更复杂的工作流图结构，
     * 支持条件边、并行执行和状态持久化。
     * </p>
     * <p>
     * 注意：当前为简化实现。完整实现需要 WorkflowState 继承 AgentState，
     * 并使用 node_async 包装节点处理器。后续版本将完善此功能。
     * </p>
     * @param canvas 画布定义
     * @return 是否成功构建（当前返回 false，待完善）
     */
    public boolean buildStateGraph(WorkflowJSON canvas) {
        // TODO: 实现 LangGraph4j StateGraph 构建
        // 当前为占位实现，后续需要:
        // 1. WorkflowState 继承 org.bsc.langgraph4j.state.AgentState
        // 2. 定义状态 Schema（Map&lt;String, Object&gt;）
        // 3. 使用 node_async 包装节点处理器
        // 4. 添加条件边（使用 edge_async）
        // 5. 配置 Checkpoint 和 Hook

        log.info("构建 StateGraph: nodes={}, edges={}", canvas.nodes().size(), canvas.edges().size());
        log.warn("LangGraph4j StateGraph 构建尚未完全实现，使用简化执行引擎");

        // 当前返回 false，表示未构建成功
        return false;
    }

    /**
     * 使用 LangGraph4j 执行工作流（带检查点）
     * @param context 执行上下文
     * @param memorySaver 内存检查点保存器
     * @return 执行结果
     */
    public ExecutionResult executeWithCheckpoint(
            WorkflowContext context,
            org.bsc.langgraph4j.checkpoint.MemorySaver memorySaver) {

        log.info("使用检查点执行工作流: workflowId={}, threadId={}",
                context.getWorkflowId(), context.getOrCreateThreadId());

        // TODO: 完整实现 LangGraph4j 执行逻辑
        // 当前使用简化执行，后续需要:
        // 1. 构建 StateGraph
        // 2. 配置 CompileConfig（包含 MemorySaver）
        // 3. 编译为可执行图
        // 4. 使用 RunnableConfig 执行

        return execute(context);
    }
}