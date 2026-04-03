package com.workflow.engine.llm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 提供商配置 DTO
 * <p>
 * 用于创建和更新 LLM 提供商的配置信息
 * </p>
 *
 * @param name           提供商名称，不能为空
 * @param providerType   提供商类型 (qwen, openai, ollama)，不能为空
 * @param apiKey         API 密钥，可选（Ollama 可能不需要）
 * @param baseUrl        基础 URL，不能为空
 * @param timeoutSeconds 超时时间（秒），可选
 * @param options        扩展选项（JSON 格式），可选
 */
public record ProviderConfig(
    /** 提供商名称 */
    @NotBlank(message = "提供商名称不能为空")
    String name,

    /** 提供商类型: qwen, openai, ollama */
    @NotBlank(message = "提供商类型不能为空")
    String providerType,

    /** API Key */
    String apiKey,

    /** 基础 URL */
    @NotBlank(message = "基础URL不能为空")
    String baseUrl,

    /** 超时时间（秒） */
    Integer timeoutSeconds,

    /** 扩展选项（JSON） */
    String options
) {
}