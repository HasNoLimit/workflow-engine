package com.workflow.engine.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 异步执行响应
 * <p>
 * 封装异步执行提交后的响应信息，立即返回执行ID供后续查询
 * </p>
 */
@Data
@Builder
public class AsyncExecutionResponse {

    /** 执行ID，用于追踪和查询执行状态 */
    private Long executionId;

    /** 当前状态，异步提交后为 RUNNING */
    private String status;

    /** 提示信息，说明执行已提交 */
    private String message;
}