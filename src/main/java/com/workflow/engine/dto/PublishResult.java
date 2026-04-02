package com.workflow.engine.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 发布结果
 * <p>
 * 封装智能体发布操作的结果信息，包括是否成功、端点地址、API密钥等
 * </p>
 */
@Data
@Builder
public class PublishResult {

    /** 是否成功 */
    private boolean success;

    /** 智能体ID */
    private Long agentId;

    /** 端点地址 */
    private String endpoint;

    /** API密钥 */
    private String apiKey;

    /** Webhook URL（仅API类型） */
    private String webhookUrl;

    /** 消息 */
    private String message;

    /**
     * 创建成功结果
     *
     * @return 成功的发布结果
     */
    public static PublishResult success() {
        return PublishResult.builder().success(true).build();
    }

    /**
     * 创建失败结果
     *
     * @param message 失败消息
     * @return 失败的发布结果
     */
    public static PublishResult failure(String message) {
        return PublishResult.builder().success(false).message(message).build();
    }

    /**
     * 设置智能体ID（链式调用）
     *
     * @param agentId 智能体ID
     * @return 当前对象
     */
    public PublishResult agentId(Long agentId) {
        this.agentId = agentId;
        return this;
    }

    /**
     * 设置端点地址（链式调用）
     *
     * @param endpoint 端点地址
     * @return 当前对象
     */
    public PublishResult endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * 设置API密钥（链式调用）
     *
     * @param apiKey API密钥
     * @return 当前对象
     */
    public PublishResult apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /**
     * 设置Webhook URL（链式调用）
     *
     * @param webhookUrl Webhook URL
     * @return 当前对象
     */
    public PublishResult webhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        return this;
    }

    /**
     * 设置消息（链式调用）
     *
     * @param message 消息
     * @return 当前对象
     */
    public PublishResult message(String message) {
        this.message = message;
        return this;
    }
}