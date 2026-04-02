package com.workflow.engine.hook;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;

/**
 * 节点执行监听器接口
 * <p>
 * 用于监听节点执行过程中的事件。
 * 实现此接口可以自定义监听逻辑，如状态推送、审计日志等。
 * </p>
 */
public interface NodeExecutionListener {

    /**
     * 节点开始执行事件
     * @param node 节点配置
     * @param state 当前状态
     * @param startTime 开始时间（毫秒）
     */
    void onNodeStart(NodeJSON node, WorkflowState state, long startTime);

    /**
     * 节点执行完成事件
     * @param node 节点配置
     * @param state 更新后的状态
     * @param startTime 开始时间（毫秒）
     * @param endTime 结束时间（毫秒）
     */
    void onNodeComplete(NodeJSON node, WorkflowState state, long startTime, long endTime);

    /**
     * 节点执行失败事件
     * @param node 节点配置
     * @param state 当前状态
     * @param error 错误信息
     * @param startTime 开始时间（毫秒）
     */
    void onNodeError(NodeJSON node, WorkflowState state, Throwable error, long startTime);

    /**
     * 获取监听器名称
     * @return 监听器名称
     */
    String getName();

    /**
     * 获取监听器优先级（数值越小优先级越高）
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否启用
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}