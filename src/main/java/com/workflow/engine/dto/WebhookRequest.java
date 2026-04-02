package com.workflow.engine.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Webhook 回调请求体
 * <p>
 * 封装工作流执行完成后发送给配置 URL 的回调数据
 * </p>
 */
@Data
@Builder
public class WebhookRequest {

    /** 智能体ID */
    private Long agentId;

    /** 智能体名称 */
    private String agentName;

    /** 执行ID */
    private Long executionId;

    /** 执行状态：SUCCESS 或 FAILED */
    private String status;

    /** 输出结果 */
    private Map<String, Object> output;

    /** 错误信息，执行失败时记录错误原因 */
    private String errorMessage;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 回调时间戳 */
    private LocalDateTime timestamp;
}