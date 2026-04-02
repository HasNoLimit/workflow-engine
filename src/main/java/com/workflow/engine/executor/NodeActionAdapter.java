package com.workflow.engine.executor;

import org.bsc.langgraph4j.action.NodeAction;
import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.node.NodeHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点处理器适配器
 * <p>
 * 将 NodeHandler 适配为 LangGraph4j NodeAction，
 * 使现有的节点处理器可以在 StateGraph 中使用。
 * </p>
 */
@Slf4j
public class NodeActionAdapter implements NodeAction<WorkflowAgentState> {

    /** 原始节点处理器 */
    private final NodeHandler handler;

    /** 节点配置 */
    private final NodeJSON node;

    /**
     * 构造适配器
     * @param handler 节点处理器
     * @param node 节点配置
     */
    public NodeActionAdapter(NodeHandler handler, NodeJSON node) {
        this.handler = handler;
        this.node = node;
    }

    /**
     * 执行节点动作
     * <p>
     * 将 LangGraph4j 的 WorkflowAgentState 转换为旧的 WorkflowState，
     * 执行节点处理器，再将结果转换回 WorkflowAgentState。
     * </p>
     * @param state 当前状态
     * @return 更新后的状态数据（Map 形式）
     */
    @Override
    public Map<String, Object> apply(WorkflowAgentState state) throws Exception {
        log.info("执行节点: nodeId={}, type={}, workflowId={}",
                node.id(), node.type(), state.workflowId().orElse(null));

        try {
            // 1. 将 WorkflowAgentState 转换为 WorkflowState（兼容现有处理器）
            WorkflowState legacyState = convertToLegacyState(state);

            // 2. 执行节点处理器
            WorkflowState resultState = handler.execute(node, legacyState);

            // 3. 将结果转换回 WorkflowAgentState 更新数据
            Map<String, Object> updates = convertToUpdates(resultState, node.id());

            log.info("节点执行完成: nodeId={}, type={}", node.id(), node.type());
            return updates;

        } catch (Exception e) {
            log.error("节点执行失败: nodeId={}, type={}, error={}",
                    node.id(), node.type(), e.getMessage(), e);
            // 返回错误状态更新
            Map<String, Object> errorUpdates = new HashMap<>();
            errorUpdates.put(WorkflowAgentState.ERROR, e.getMessage());
            errorUpdates.put(WorkflowAgentState.STATUS, WorkflowAgentState.STATUS_FAILED);
            return errorUpdates;
        }
    }

    /**
     * 将 WorkflowAgentState 转换为 WorkflowState
     * <p>
     * 用于兼容现有的 NodeHandler 实现
     * </p>
     * @param agentState LangGraph4j 状态
     * @return 旧的 WorkflowState
     */
    private WorkflowState convertToLegacyState(WorkflowAgentState agentState) {
        return WorkflowState.builder()
                .workflowId(agentState.workflowId().orElse(null))
                .executionId(agentState.executionId().orElse(null))
                .currentNodeId(agentState.currentNode().orElse(null))
                .input(agentState.input().orElse(new HashMap<>()))
                .output(agentState.output().orElse(new HashMap<>()))
                .chatHistory(agentState.chatHistory().orElse(new java.util.ArrayList<>()))
                .status(agentState.status().orElse(WorkflowAgentState.STATUS_RUNNING))
                .executionPath(agentState.executionPath().orElse(new java.util.ArrayList<>()))
                .variables(agentState.variables().orElse(new HashMap<>()))
                .errorMessage(agentState.error().orElse(null))
                .build();
    }

    /**
     * 将 WorkflowState 结果转换为状态更新数据
     * <p>
     * 提取需要更新的字段，返回 Map 形式供 LangGraph4j 合并
     * </p>
     * @param legacyState 旧状态结果
     * @param nodeId 当前节点ID
     * @return 状态更新数据
     */
    private Map<String, Object> convertToUpdates(WorkflowState legacyState, String nodeId) {
        Map<String, Object> updates = new HashMap<>();

        // 更新当前节点
        updates.put(WorkflowAgentState.CURRENT_NODE, nodeId);

        // 更新执行路径（追加当前节点）
        java.util.List<String> newPath = new java.util.ArrayList<>(legacyState.getExecutionPath());
        if (!newPath.contains(nodeId)) {
            newPath.add(nodeId);
        }
        updates.put(WorkflowAgentState.EXECUTION_PATH, newPath);

        // 更新状态
        updates.put(WorkflowAgentState.STATUS, legacyState.getStatus());

        // 更新输出数据
        if (legacyState.getOutput() != null && !legacyState.getOutput().isEmpty()) {
            updates.put(WorkflowAgentState.OUTPUT, legacyState.getOutput());
        }

        // 更新对话历史
        if (legacyState.getChatHistory() != null && !legacyState.getChatHistory().isEmpty()) {
            updates.put(WorkflowAgentState.CHAT_HISTORY, legacyState.getChatHistory());
        }

        // 更新变量存储
        if (legacyState.getVariables() != null && !legacyState.getVariables().isEmpty()) {
            updates.put(WorkflowAgentState.VARIABLES, legacyState.getVariables());
        }

        // 更新错误信息
        if (legacyState.getErrorMessage() != null) {
            updates.put(WorkflowAgentState.ERROR, legacyState.getErrorMessage());
        }

        // 存储条件分支结果（用于条件节点）
        Object branchValue = legacyState.getVariable("condition_branch_" + nodeId);
        if (branchValue != null) {
            updates.put(WorkflowAgentState.CONDITION_BRANCH, branchValue);
        }

        return updates;
    }

    /**
     * 获取原始节点处理器
     * @return 节点处理器
     */
    public NodeHandler getHandler() {
        return handler;
    }

    /**
     * 获取节点配置
     * @return 节点配置
     */
    public NodeJSON getNode() {
        return node;
    }
}