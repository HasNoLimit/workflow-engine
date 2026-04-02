package com.workflow.engine.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 智能体执行请求
 * <p>
 * 封装外部系统调用智能体时的请求参数
 * </p>
 *
 * @param input 输入参数，不能为空
 */
public record ExecutionRequest(
        /** 输入参数，将被传递给工作流的起始节点 */
        @NotNull(message = "输入参数不能为空") Map<String, Object> input
) {
}