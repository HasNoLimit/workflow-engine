package com.workflow.engine.exception;

/**
 * API Key 无效异常
 * <p>
 * 当调用 API 智能体时提供的 API Key 与智能体配置不匹配时抛出
 * </p>
 */
public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException(String message) {
        super(message);
    }

    public InvalidApiKeyException(Long agentId) {
        super(String.format("API Key 无效: agentId=%d", agentId));
    }
}