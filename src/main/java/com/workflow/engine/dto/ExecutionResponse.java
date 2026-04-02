package com.workflow.engine.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 执行响应（同步模式）
 * <p>
 * 封装同步执行完成后的响应信息，包含执行结果、状态、耗时等
 * </p>
 */
@Data
@Builder
public class ExecutionResponse {

    /** 执行ID，用于追踪和查询 */
    private Long executionId;

    /** 执行状态：RUNNING、SUCCESS、FAILED、TIMEOUT */
    private String status;

    /** 输出结果，由工作流最终节点产生 */
    private Map<String, Object> output;

    /** 执行路径，记录经过的节点ID列表 */
    private List<String> executionPath;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 错误信息，执行失败时记录错误原因 */
    private String errorMessage;
}