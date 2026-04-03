package com.workflow.engine.llm;

import com.workflow.engine.exception.UnsupportedProviderTypeException;
import com.workflow.engine.model.LlmProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * LLM 模型工厂
 * <p>
 * 根据提供商类型创建对应的 ChatModel 实例。
 * 支持的提供商类型：
 * <ul>
 *     <li>qwen - 通义千问（阿里云 DashScope）</li>
 *     <li>openai - OpenAI GPT 系列</li>
 *     <li>ollama - Ollama 本地模型</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class LlmModelFactory {

    /** 通义千问提供商类型 */
    private static final String TYPE_QWEN = "qwen";

    /** OpenAI 提供商类型 */
    private static final String TYPE_OPENAI = "openai";

    /** Ollama 提供商类型 */
    private static final String TYPE_OLLAMA = "ollama";

    /** 默认最大 Token 数 */
    private static final int DEFAULT_MAX_TOKENS = 4096;

    /** 默认温度参数 */
    private static final double DEFAULT_TEMPERATURE = 0.7;

    /** 默认超时时间（秒） */
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    /**
     * 创建 ChatModel 实例
     *
     * @param provider    提供商配置
     * @param modelId     模型标识
     * @param maxTokens   最大 Token 数（可为 null，使用默认值）
     * @param temperature 温度参数（可为 null，使用默认值）
     * @return ChatModel 实例
     * @throws UnsupportedProviderTypeException 当提供商类型不支持时抛出
     */
    public ChatModel createChatModel(LlmProvider provider, String modelId,
                                              Integer maxTokens, Double temperature) {
        String providerType = provider.getProviderType().toLowerCase();
        log.debug("创建 ChatModel: providerType={}, modelId={}, maxTokens={}, temperature={}",
            providerType, modelId, maxTokens, temperature);

        return switch (providerType) {
            case TYPE_QWEN -> createQwenModel(provider, modelId, maxTokens, temperature);
            case TYPE_OPENAI -> createOpenAiModel(provider, modelId, maxTokens, temperature);
            case TYPE_OLLAMA -> createOllamaModel(provider, modelId, maxTokens, temperature);
            default -> throw new UnsupportedProviderTypeException(provider.getProviderType());
        };
    }

    /**
     * 创建通义千问 ChatModel
     * <p>
     * 使用阿里云 DashScope API
     * </p>
     *
     * @param provider    提供商配置
     * @param modelId     模型标识
     * @param maxTokens   最大 Token 数
     * @param temperature 温度参数
     * @return QwenChatModel 实例
     */
    private ChatModel createQwenModel(LlmProvider provider, String modelId,
                                               Integer maxTokens, Double temperature) {
        log.info("创建通义千问模型: modelId={}", modelId);

        QwenChatModel.QwenChatModelBuilder builder = QwenChatModel.builder()
            .apiKey(provider.getApiKey())
            .modelName(modelId);

        // 设置基础 URL（如果有自定义）
        if (provider.getBaseUrl() != null && !provider.getBaseUrl().isBlank()) {
            builder.baseUrl(provider.getBaseUrl());
        }

        // 设置最大 Token 数
        builder.maxTokens(maxTokens != null ? maxTokens : DEFAULT_MAX_TOKENS);

        // 设置温度参数（Qwen 需要 Float 类型）
        Float tempValue = temperature != null ? temperature.floatValue() : (float) DEFAULT_TEMPERATURE;
        builder.temperature(tempValue);

        return builder.build();
    }

    /**
     * 创建 OpenAI ChatModel
     *
     * @param provider    提供商配置
     * @param modelId     模型标识
     * @param maxTokens   最大 Token 数
     * @param temperature 温度参数
     * @return OpenAiChatModel 实例
     */
    private ChatModel createOpenAiModel(LlmProvider provider, String modelId,
                                                 Integer maxTokens, Double temperature) {
        log.info("创建 OpenAI 模型: modelId={}", modelId);

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
            .apiKey(provider.getApiKey())
            .modelName(modelId);

        // 设置基础 URL（支持自定义 OpenAI 兼容 API）
        if (provider.getBaseUrl() != null && !provider.getBaseUrl().isBlank()) {
            builder.baseUrl(provider.getBaseUrl());
        }

        // 设置最大 Token 数
        builder.maxTokens(maxTokens != null ? maxTokens : DEFAULT_MAX_TOKENS);

        // 设置温度参数
        builder.temperature(temperature != null ? temperature : DEFAULT_TEMPERATURE);

        // 设置超时时间
        int timeoutSeconds = provider.getTimeoutSeconds() != null
            ? provider.getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
        builder.timeout(Duration.ofSeconds(timeoutSeconds));

        return builder.build();
    }

    /**
     * 创建 Ollama ChatModel
     * <p>
     * Ollama 不需要 API Key，仅需要基础 URL
     * </p>
     *
     * @param provider    提供商配置
     * @param modelId     模型标识
     * @param maxTokens   最大 Token 数
     * @param temperature 温度参数
     * @return OllamaChatModel 实例
     */
    private ChatModel createOllamaModel(LlmProvider provider, String modelId,
                                                 Integer maxTokens, Double temperature) {
        log.info("创建 Ollama 模型: modelId={}", modelId);

        OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
            .modelName(modelId);

        // 设置基础 URL
        if (provider.getBaseUrl() != null && !provider.getBaseUrl().isBlank()) {
            builder.baseUrl(provider.getBaseUrl());
        }

        // 设置温度参数
        builder.temperature(temperature != null ? temperature : DEFAULT_TEMPERATURE);

        // 设置超时时间
        int timeoutSeconds = provider.getTimeoutSeconds() != null
            ? provider.getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
        builder.timeout(Duration.ofSeconds(timeoutSeconds));

        return builder.build();
    }

    /**
     * 验证提供商类型是否支持
     *
     * @param providerType 提供商类型
     * @return 是否支持
     */
    public boolean isSupportedProviderType(String providerType) {
        if (providerType == null) {
            return false;
        }
        String type = providerType.toLowerCase();
        return TYPE_QWEN.equals(type) || TYPE_OPENAI.equals(type) || TYPE_OLLAMA.equals(type);
    }

    /**
     * 获取支持的提供商类型列表
     *
     * @return 支持的提供商类型数组
     */
    public String[] getSupportedProviderTypes() {
        return new String[]{TYPE_QWEN, TYPE_OPENAI, TYPE_OLLAMA};
    }
}