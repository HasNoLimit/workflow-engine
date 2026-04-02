package com.workflow.engine.executor;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行状态
 * <p>
 * 在整个工作流执行过程中传递和修改的状态对象。
 * 使用不可变模式，每次更新都返回新的实例。
 * </p>
 */
@Data
@Builder
public class WorkflowState {

    /** 执行状态常量：运行中 */
    public static final String STATUS_RUNNING = "RUNNING";

    /** 执行状态常量：成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /** 执行状态常量：失败 */
    public static final String STATUS_FAILED = "FAILED";

    /** 执行状态常量：暂停 */
    public static final String STATUS_PAUSED = "PAUSED";

    /** 工作流ID */
    private Long workflowId;

    /** 执行ID */
    private Long executionId;

    /** 当前节点ID */
    private String currentNodeId;

    /** 输入数据 */
    @Builder.Default
    private Map<String, Object> input = new HashMap<>();

    /** 输出数据 */
    @Builder.Default
    private Map<String, Object> output = new HashMap<>();

    /** 对话历史（用于 LLM 节点） */
    @Builder.Default
    private List<Map<String, String>> chatHistory = new ArrayList<>();

    /** 错误信息 */
    private String errorMessage;

    /** 执行状态：RUNNING, SUCCESS, FAILED, PAUSED */
    private String status;

    /** 节点执行路径 */
    @Builder.Default
    private List<String> executionPath = new ArrayList<>();

    /** 变量存储（用于节点间数据传递） */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 更新状态并返回新实例（不可变模式）
     * @param status 新状态
     * @return 新的状态实例
     */
    public WorkflowState withStatus(String status) {
        return WorkflowState.builder()
                .workflowId(this.workflowId)
                .executionId(this.executionId)
                .currentNodeId(this.currentNodeId)
                .input(this.input)
                .output(this.output)
                .chatHistory(this.chatHistory)
                .errorMessage(this.errorMessage)
                .status(status)
                .executionPath(this.executionPath)
                .variables(this.variables)
                .build();
    }

    /**
     * 设置当前节点并更新执行路径
     * @param nodeId 节点ID
     * @return 新的状态实例
     */
    public WorkflowState withCurrentNode(String nodeId) {
        List<String> newPath = new ArrayList<>(this.executionPath);
        newPath.add(nodeId);
        return WorkflowState.builder()
                .workflowId(this.workflowId)
                .executionId(this.executionId)
                .currentNodeId(nodeId)
                .input(this.input)
                .output(this.output)
                .chatHistory(this.chatHistory)
                .errorMessage(this.errorMessage)
                .status(this.status)
                .executionPath(newPath)
                .variables(this.variables)
                .build();
    }

    /**
     * 设置错误信息
     * @param errorMessage 错误信息
     * @return 新的状态实例
     */
    public WorkflowState withError(String errorMessage) {
        return WorkflowState.builder()
                .workflowId(this.workflowId)
                .executionId(this.executionId)
                .currentNodeId(this.currentNodeId)
                .input(this.input)
                .output(this.output)
                .chatHistory(this.chatHistory)
                .errorMessage(errorMessage)
                .status(STATUS_FAILED)
                .executionPath(this.executionPath)
                .variables(this.variables)
                .build();
    }

    /**
     * 设置输出数据
     * @param output 输出数据
     * @return 新的状态实例
     */
    public WorkflowState withOutput(Map<String, Object> output) {
        return WorkflowState.builder()
                .workflowId(this.workflowId)
                .executionId(this.executionId)
                .currentNodeId(this.currentNodeId)
                .input(this.input)
                .output(output)
                .chatHistory(this.chatHistory)
                .errorMessage(this.errorMessage)
                .status(this.status)
                .executionPath(this.executionPath)
                .variables(this.variables)
                .build();
    }

    /**
     * 添加对话消息
     * @param role 角色（user/assistant/system）
     * @param content 消息内容
     * @return 新的状态实例
     */
    public WorkflowState addChatMessage(String role, String content) {
        List<Map<String, String>> newHistory = new ArrayList<>(this.chatHistory);
        newHistory.add(Map.of("role", role, "content", content));
        return WorkflowState.builder()
                .workflowId(this.workflowId)
                .executionId(this.executionId)
                .currentNodeId(this.currentNodeId)
                .input(this.input)
                .output(this.output)
                .chatHistory(newHistory)
                .errorMessage(this.errorMessage)
                .status(this.status)
                .executionPath(this.executionPath)
                .variables(this.variables)
                .build();
    }

    /**
     * 设置变量
     * @param key 变量名
     * @param value 变量值
     * @return 新的状态实例
     */
    public WorkflowState setVariable(String key, Object value) {
        Map<String, Object> newVariables = new HashMap<>(this.variables);
        newVariables.put(key, value);
        return WorkflowState.builder()
                .workflowId(this.workflowId)
                .executionId(this.executionId)
                .currentNodeId(this.currentNodeId)
                .input(this.input)
                .output(this.output)
                .chatHistory(this.chatHistory)
                .errorMessage(this.errorMessage)
                .status(this.status)
                .executionPath(this.executionPath)
                .variables(newVariables)
                .build();
    }

    /**
     * 获取变量
     * @param key 变量名
     * @return 变量值
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        return (T) variables.get(key);
    }
}