package com.workflow.engine.exception;

/**
 * LLM 提供商未找到异常
 * <p>
 * 当尝试访问不存在的 LLM 提供商时抛出此异常
 * </p>
 */
public class LlmProviderNotFoundException extends RuntimeException {

    /**
     * 构造函数
     *
     * @param providerId 未找到的提供商 ID
     */
    public LlmProviderNotFoundException(Long providerId) {
        super("LLM 提供商未找到: id=" + providerId);
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public LlmProviderNotFoundException(String message) {
        super(message);
    }
}