package com.workflow.engine.exception;

/**
 * 不支持的 LLM 提供商类型异常
 * <p>
 * 当尝试创建不支持的 LLM 提供商类型时抛出此异常
 * </p>
 */
public class UnsupportedProviderTypeException extends RuntimeException {

    /**
     * 构造函数
     *
     * @param providerType 不支持的提供商类型
     */
    public UnsupportedProviderTypeException(String providerType) {
        super("不支持的 LLM 提供商类型: " + providerType);
    }
}