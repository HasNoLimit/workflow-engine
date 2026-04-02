package com.workflow.engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建智能体请求
 * <p>
 * 用于创建新的智能体，支持 DIALOG（对话智能体）和 API（API智能体）两种类型
 * </p>
 *
 * @param workflowId      工作流ID（必填）
 * @param workflowVersion 工作流版本号（必填）
 * @param name            智能体名称（必填）
 * @param type            智能体类型：DIALOG 或 API（必填）
 */
public record AgentCreateRequest(
    /** 工作流ID */
    @NotNull(message = "工作流ID不能为空")
    Long workflowId,

    /** 工作流版本号 */
    @NotNull(message = "工作流版本号不能为空")
    Integer workflowVersion,

    /** 智能体名称 */
    @NotBlank(message = "智能体名称不能为空")
    String name,

    /** 智能体类型：DIALOG 或 API */
    @NotBlank(message = "智能体类型不能为空")
    String type
) {}