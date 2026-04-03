package com.workflow.engine.exception;

/**
 * LLM 模型未找到异常
 * <p>
 * 当尝试访问不存在的 LLM 模型配置时抛出此异常
 * </p>
 */
public class LlmModelNotFoundException extends RuntimeException {

    /**
     * 构造函数
     *
     * @param providerId 提供商 ID
     * @param modelId    模型 ID
     */
    public LlmModelNotFoundException(Long providerId, String modelId) {
        super("LLM 模型未找到: providerId=" + providerId + ", modelId=" + modelId);
    }

    /**
     * 构造函数
     *
     * @param modelId 模型 ID
     */
    public LlmModelNotFoundException(String modelId) {
        super("LLM 模型未找到: modelId=" + modelId);
    }
}