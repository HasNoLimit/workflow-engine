package com.workflow.engine.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 智能体端点信息
 * <p>
 * 封装已发布智能体的端点访问信息，用于外部调用者获取连接参数
 * </p>
 */
@Data
@Builder
public class AgentEndpoint {

    /** 智能体ID */
    private Long agentId;

    /** 智能体类型（DIALOG 或 API） */
    private String type;

    /** 端点地址 */
    private String endpoint;

    /** API密钥 */
    private String apiKey;

    /** Webhook URL（仅 API 类型） */
    private String webhookUrl;

    /** 超时时间（秒） */
    private Integer timeoutSeconds;
}