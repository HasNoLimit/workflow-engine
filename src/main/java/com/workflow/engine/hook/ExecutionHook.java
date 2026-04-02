package com.workflow.engine.hook;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 执行钩子
 * <p>
 * 利用 LangGraph4j Hook 机制在节点执行前后进行拦截。
 * 用于日志记录、状态推送、性能监控。
 * </p>
 */
@Slf4j
@Component
public class ExecutionHook {

    /**
     * 节点执行前回调
     * @param node 待执行的节点
     * @param state 当前状态
     */
    public void beforeNodeExecution(NodeJSON node, WorkflowState state) {
        log.info("节点开始执行: nodeId={}, type={}, workflowId={}, status={}",
                node.id(), node.type(), state.getWorkflowId(), state.getStatus());

        // TODO: 推送节点执行状态（SSE/WebSocket）
        // TODO: 记录节点开始时间（性能监控）
        // TODO: 更新节点执行日志状态为 RUNNING
    }

    /**
     * 节点执行后回调
     * @param node 执行完成的节点
     * @param state 更新后的状态
     */
    public void afterNodeExecution(NodeJSON node, WorkflowState state) {
        log.info("节点执行完成: nodeId={}, type={}, status={}, executionPath={}",
                node.id(), node.type(), state.getStatus(), state.getExecutionPath());

        // TODO: 推送节点完成状态（SSE/WebSocket）
        // TODO: 记录节点执行耗时
        // TODO: 更新节点执行日志状态为 SUCCESS
    }

    /**
     * 节点执行错误回调
     * @param node 执行失败的节点
     * @param state 当前状态
     * @param error 错误信息
     */
    public void onNodeError(NodeJSON node, WorkflowState state, Throwable error) {
        log.error("节点执行失败: nodeId={}, type={}, workflowId={}, error={}",
                node.id(), node.type(), state.getWorkflowId(), error.getMessage(), error);

        // TODO: 推送错误状态（SSE/WebSocket）
        // TODO: 更新节点执行日志状态为 FAILED
        // TODO: 记录错误详情到数据库
    }

    /**
     * 工作流开始前回调
     * @param workflowId 工作流ID
     * @param context 执行上下文
     */
    public void beforeWorkflowExecution(Long workflowId, com.workflow.engine.executor.WorkflowContext context) {
        log.info("工作流开始执行: workflowId={}, threadId={}, mode={}",
                workflowId, context.getOrCreateThreadId(), context.getExecutionMode());

        // TODO: 创建执行记录（WorkflowExecution）
        // TODO: 推送工作流开始状态
    }

    /**
     * 工作流完成后回调
     * @param workflowId 工作流ID
     * @param result 执行结果
     * @param context 执行上下文
     */
    public void afterWorkflowExecution(Long workflowId,
            com.workflow.engine.executor.ExecutionResult result,
            com.workflow.engine.executor.WorkflowContext context) {
        log.info("工作流执行完成: workflowId={}, success={}, status={}, durationMs={}",
                workflowId, result.isSuccess(), result.getStatus(), result.getDurationMs());

        // TODO: 更新执行记录状态
        // TODO: 推送工作流完成状态
        // TODO: 记录执行统计信息
    }

    /**
     * 工作流执行错误回调
     * @param workflowId 工作流ID
     * @param context 执行上下文
     * @param error 错误信息
     */
    public void onWorkflowError(Long workflowId,
            com.workflow.engine.executor.WorkflowContext context,
            Throwable error) {
        log.error("工作流执行失败: workflowId={}, threadId={}, error={}",
                workflowId, context.getOrCreateThreadId(), error.getMessage(), error);

        // TODO: 更新执行记录状态为 FAILED
        // TODO: 推送工作流失败状态
        // TODO: 记录错误详情
    }
}