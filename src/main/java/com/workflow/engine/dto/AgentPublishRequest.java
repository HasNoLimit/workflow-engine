package com.workflow.engine.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 发布智能体请求
 * <p>
 * 用于配置并发布智能体，设置 Webhook 回调地址和超时时间
 * </p>
 *
 * @param webhookUrl     Webhook 回调地址（仅 API 类型需要，必须是有效的 URL 格式）
 * @param timeoutSeconds 超时时间（秒，必填）
 */
public record AgentPublishRequest(
    /** Webhook 回调地址（仅 API 类型需要，必须是有效的 URL 格式） */
    @Pattern(regexp = "^https?://[\\w.-]+(:\\d+)?(/.*)?$", message = "Webhook URL 格式无效") String webhookUrl,

    /** 超时时间（秒） */
    @NotNull(message = "超时时间不能为空")
    Integer timeoutSeconds
) {}