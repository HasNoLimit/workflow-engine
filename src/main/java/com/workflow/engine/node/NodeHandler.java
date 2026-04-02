package com.workflow.engine.node;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;

/**
 * 节点处理器接口
 * <p>
 * 所有工作流节点处理器必须实现此接口。
 * 每种节点类型对应一个处理器实现。
 * </p>
 */
public interface NodeHandler {

    /**
     * 获取节点类型
     * @return 节点类型标识（如 start, end, llm, tool, condition, parallel）
     */
    String getNodeType();

    /**
     * 执行节点逻辑
     * @param node 节点配置（FlowGram.AI NodeJSON）
     * @param state 当前状态
     * @return 更新后的状态
     */
    WorkflowState execute(NodeJSON node, WorkflowState state);

    /**
     * 验证节点配置是否有效
     * @param node 节点配置
     * @return 是否有效
     */
    default boolean validate(NodeJSON node) {
        return true;
    }

    /**
     * 获取节点描述（用于日志和调试）
     * @param node 节点配置
     * @return 节点描述
     */
    default String getDescription(NodeJSON node) {
        return node.type() + " node: " + node.id();
    }
}