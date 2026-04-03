package com.workflow.engine.llm;

import com.workflow.engine.llm.dto.ModelInfo;
import com.workflow.engine.llm.dto.ProviderConfig;
import com.workflow.engine.model.LlmProvider;
import dev.langchain4j.model.chat.ChatModel;

import java.util.List;

/**
 * LLM 提供商服务接口
 * <p>
 * 提供 LLM 提供商的注册、配置、模型管理功能。
 * 支持通义千问 (Qwen)、OpenAI、Ollama 三种提供商类型。
 * </p>
 */
public interface LlmProviderService {

    /**
     * 创建 LLM 提供商
     *
     * @param config 提供商配置
     * @return 创建的提供商 ID
     */
    Long createProvider(ProviderConfig config);

    /**
     * 更新提供商配置
     *
     * @param providerId 提供商 ID
     * @param config      新的配置
     */
    void updateProvider(Long providerId, ProviderConfig config);

    /**
     * 删除提供商
     * <p>
     * 删除提供商时会同时删除关联的模型配置
     * </p>
     *
     * @param providerId 提供商 ID
     */
    void deleteProvider(Long providerId);

    /**
     * 获取所有提供商
     *
     * @return 提供商列表
     */
    List<LlmProvider> listProviders();

    /**
     * 按类型获取提供商
     *
     * @param providerType 提供商类型 (qwen, openai, ollama)
     * @return 匹配的提供商列表
     */
    List<LlmProvider> listProvidersByType(String providerType);

    /**
     * 根据 ID 获取提供商
     *
     * @param providerId 提供商 ID
     * @return 提供商信息
     */
    LlmProvider getProvider(Long providerId);

    /**
     * 获取提供商的模型列表
     *
     * @param providerId 提供商 ID
     * @return 模型信息列表
     */
    List<ModelInfo> listModels(Long providerId);

    /**
     * 添加模型到提供商
     *
     * @param providerId 提供商 ID
     * @param model       模型信息
     * @return 创建的模型 ID
     */
    Long addModel(Long providerId, ModelInfo model);

    /**
     * 更新模型配置
     *
     * @param modelId 模型 ID（数据库 ID）
     * @param model   新的模型信息
     */
    void updateModel(Long modelId, ModelInfo model);

    /**
     * 删除模型
     *
     * @param modelId 模型 ID（数据库 ID）
     */
    void deleteModel(Long modelId);

    /**
     * 获取 ChatModel 实例
     * <p>
     * 根据提供商和模型标识创建对应的 ChatModel 实例
     * </p>
     *
     * @param providerId 提供商 ID
     * @param modelId    模型标识（如 qwen-max, gpt-4）
     * @return ChatModel 实例
     */
    ChatModel getChatModel(Long providerId, String modelId);

    /**
     * 获取 ChatModel 实例（带自定义参数）
     *
     * @param providerId  提供商 ID
     * @param modelId     模型标识
     * @param maxTokens   最大 Token 数
     * @param temperature 温度参数
     * @return ChatModel 实例
     */
    ChatModel getChatModel(Long providerId, String modelId, Integer maxTokens, Double temperature);

    /**
     * 测试提供商连接
     * <p>
     * 发送一个简单的测试请求，验证提供商配置是否正确
     * </p>
     *
     * @param providerId 提供商 ID
     * @return 连接是否成功
     */
    boolean testConnection(Long providerId);

    /**
     * 测试模型连接
     *
     * @param providerId 提供商 ID
     * @param modelId    模型标识
     * @return 连接是否成功
     */
    boolean testModelConnection(Long providerId, String modelId);
}