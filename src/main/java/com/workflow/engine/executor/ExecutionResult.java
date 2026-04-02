package com.workflow.engine.executor;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流执行结果
 * <p>
 * 封装工作流执行完成后的结果信息
 * </p>
 */
@Data
@Builder
public class ExecutionResult {

    /** 是否成功 */
    private boolean success;

    /** 执行状态 */
    private String status;

    /** 输出数据 */
    private Map<String, Object> output;

    /** 执行路径（经过的节点ID列表） */
    private List<String> executionPath;

    /** 错误信息 */
    private String errorMessage;

    /** 执行ID */
    private Long executionId;

    /** 执行时长（毫秒） */
    private Long durationMs;

    /**
     * 创建成功结果
     * @param output 输出数据
     * @param executionPath 执行路径
     * @return 成功的执行结果
     */
    public static ExecutionResult success(Map<String, Object> output, List<String> executionPath) {
        return ExecutionResult.builder()
                .success(true)
                .status(WorkflowAgentState.STATUS_SUCCESS)
                .output(output)
                .executionPath(executionPath)
                .build();
    }

    /**
     * 创建失败结果
     * @param errorMessage 错误信息
     * @param executionPath 执行路径
     * @return 失败的执行结果
     */
    public static ExecutionResult failure(String errorMessage, List<String> executionPath) {
        return ExecutionResult.builder()
                .success(false)
                .status(WorkflowAgentState.STATUS_FAILED)
                .errorMessage(errorMessage)
                .executionPath(executionPath)
                .build();
    }
}