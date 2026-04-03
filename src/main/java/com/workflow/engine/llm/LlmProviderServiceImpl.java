package com.workflow.engine.llm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.engine.exception.LlmModelNotFoundException;
import com.workflow.engine.exception.LlmProviderNotFoundException;
import com.workflow.engine.exception.UnsupportedProviderTypeException;
import com.workflow.engine.llm.dto.ModelInfo;
import com.workflow.engine.llm.dto.ProviderConfig;
import com.workflow.engine.mapper.LlmModelMapper;
import com.workflow.engine.mapper.LlmProviderMapper;
import com.workflow.engine.model.LlmModel;
import com.workflow.engine.model.LlmProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * LLM 提供商服务实现
 * <p>
 * 实现 LLM 提供商的注册、配置、模型管理和 ChatModel 创建功能。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmProviderServiceImpl implements LlmProviderService {

    private final LlmProviderMapper providerMapper;
    private final LlmModelMapper modelMapper;
    private final LlmModelFactory modelFactory;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long createProvider(ProviderConfig config) {
        log.info("创建 LLM 提供商: name={}, type={}", config.name(), config.providerType());

        // 验证提供商类型
        if (!modelFactory.isSupportedProviderType(config.providerType())) {
            throw new UnsupportedProviderTypeException(config.providerType());
        }

        LlmProvider provider = new LlmProvider();
        provider.setName(config.name());
        provider.setProviderType(config.providerType().toLowerCase());
        provider.setApiKey(config.apiKey());
        provider.setBaseUrl(config.baseUrl());
        provider.setTimeoutSeconds(config.timeoutSeconds());

        // 解析 options JSON
        if (config.options() != null && !config.options().isBlank()) {
            try {
                Map<String, Object> options = objectMapper.readValue(
                    config.options(), new TypeReference<>() {});
                provider.setOptions(options);
            } catch (JsonProcessingException e) {
                log.warn("解析 options JSON 失败: {}", e.getMessage());
            }
        }

        providerMapper.insert(provider);
        log.info("LLM 提供商创建成功: id={}, name={}", provider.getId(), provider.getName());

        return provider.getId();
    }

    @Override
    @Transactional
    public void updateProvider(Long providerId, ProviderConfig config) {
        log.info("更新 LLM 提供商: id={}", providerId);

        LlmProvider provider = getProviderOrThrow(providerId);

        // 更新字段
        provider.setName(config.name());
        provider.setProviderType(config.providerType().toLowerCase());
        provider.setApiKey(config.apiKey());
        provider.setBaseUrl(config.baseUrl());
        provider.setTimeoutSeconds(config.timeoutSeconds());

        // 解析 options JSON
        if (config.options() != null && !config.options().isBlank()) {
            try {
                Map<String, Object> options = objectMapper.readValue(
                    config.options(), new TypeReference<>() {});
                provider.setOptions(options);
            } catch (JsonProcessingException e) {
                log.warn("解析 options JSON 失败: {}", e.getMessage());
            }
        }

        providerMapper.updateById(provider);
        log.info("LLM 提供商更新成功: id={}", providerId);
    }

    @Override
    @Transactional
    public void deleteProvider(Long providerId) {
        log.info("删除 LLM 提供商: id={}", providerId);

        // 验证提供商存在
        getProviderOrThrow(providerId);

        // 删除关联的模型
        modelMapper.delete(new LambdaQueryWrapper<LlmModel>()
            .eq(LlmModel::getProviderId, providerId));

        // 删除提供商
        providerMapper.deleteById(providerId);

        log.info("LLM 提供商删除成功: id={}", providerId);
    }

    @Override
    public List<LlmProvider> listProviders() {
        return providerMapper.selectList(new LambdaQueryWrapper<LlmProvider>()
            .orderByDesc(LlmProvider::getCreatedAt));
    }

    @Override
    public List<LlmProvider> listProvidersByType(String providerType) {
        return providerMapper.selectList(new LambdaQueryWrapper<LlmProvider>()
            .eq(LlmProvider::getProviderType, providerType.toLowerCase())
            .orderByDesc(LlmProvider::getCreatedAt));
    }

    @Override
    public LlmProvider getProvider(Long providerId) {
        return getProviderOrThrow(providerId);
    }

    @Override
    public List<ModelInfo> listModels(Long providerId) {
        // 验证提供商存在
        getProviderOrThrow(providerId);

        List<LlmModel> models = modelMapper.findByProviderId(providerId);
        return models.stream()
            .map(this::convertToModelInfo)
            .toList();
    }

    @Override
    @Transactional
    public Long addModel(Long providerId, ModelInfo model) {
        log.info("添加模型到提供商: providerId={}, modelId={}", providerId, model.modelId());

        // 验证提供商存在
        getProviderOrThrow(providerId);

        LlmModel entity = new LlmModel();
        entity.setProviderId(providerId);
        entity.setModelId(model.modelId());
        entity.setModelName(model.modelName());
        entity.setMaxTokens(model.maxTokens());
        entity.setInputPrice(model.inputPrice());
        entity.setOutputPrice(model.outputPrice());
        entity.setCapabilities(model.capabilities());
        entity.setCreatedAt(LocalDateTime.now());

        modelMapper.insert(entity);
        log.info("模型添加成功: id={}, modelId={}", entity.getId(), model.modelId());

        return entity.getId();
    }

    @Override
    @Transactional
    public void updateModel(Long modelId, ModelInfo model) {
        log.info("更新模型: id={}", modelId);

        LlmModel entity = modelMapper.selectById(modelId);
        if (entity == null) {
            throw new LlmModelNotFoundException(modelId.toString());
        }

        entity.setModelId(model.modelId());
        entity.setModelName(model.modelName());
        entity.setMaxTokens(model.maxTokens());
        entity.setInputPrice(model.inputPrice());
        entity.setOutputPrice(model.outputPrice());
        entity.setCapabilities(model.capabilities());

        modelMapper.updateById(entity);
        log.info("模型更新成功: id={}", modelId);
    }

    @Override
    @Transactional
    public void deleteModel(Long modelId) {
        log.info("删除模型: id={}", modelId);

        int deleted = modelMapper.deleteById(modelId);
        if (deleted == 0) {
            throw new LlmModelNotFoundException(modelId.toString());
        }

        log.info("模型删除成功: id={}", modelId);
    }

    @Override
    public ChatModel getChatModel(Long providerId, String modelId) {
        return getChatModel(providerId, modelId, null, null);
    }

    @Override
    public ChatModel getChatModel(Long providerId, String modelId, Integer maxTokens, Double temperature) {
        log.debug("获取 ChatModel: providerId={}, modelId={}", providerId, modelId);

        LlmProvider provider = getProviderOrThrow(providerId);

        // 查找模型配置
        LlmModel model = modelMapper.selectOne(new LambdaQueryWrapper<LlmModel>()
            .eq(LlmModel::getProviderId, providerId)
            .eq(LlmModel::getModelId, modelId));

        // 如果找到模型配置，使用配置中的参数
        if (model != null) {
            Integer effectiveMaxTokens = maxTokens != null ? maxTokens : model.getMaxTokens();
            return modelFactory.createChatModel(provider, modelId, effectiveMaxTokens, temperature);
        }

        // 没有模型配置，使用默认参数创建
        return modelFactory.createChatModel(provider, modelId, maxTokens, temperature);
    }

    @Override
    public boolean testConnection(Long providerId) {
        log.info("测试提供商连接: id={}", providerId);

        try {
            LlmProvider provider = getProviderOrThrow(providerId);

            // 获取第一个可用模型进行测试
            List<LlmModel> models = modelMapper.findByProviderId(providerId);
            String testModelId = models.isEmpty() ? getDefaultModelId(provider.getProviderType()) : models.get(0).getModelId();

            ChatModel model = modelFactory.createChatModel(provider, testModelId, 100, 0.0);
            String response = model.chat("Hello");
            log.info("提供商连接测试成功: id={}, response={}", providerId, response);
            return true;
        } catch (Exception e) {
            log.error("提供商连接测试失败: id={}, error={}", providerId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean testModelConnection(Long providerId, String modelId) {
        log.info("测试模型连接: providerId={}, modelId={}", providerId, modelId);

        try {
            LlmProvider provider = getProviderOrThrow(providerId);
            ChatModel model = modelFactory.createChatModel(provider, modelId, 100, 0.0);
            String response = model.chat("Hello");
            log.info("模型连接测试成功: providerId={}, modelId={}", providerId, modelId);
            return true;
        } catch (Exception e) {
            log.error("模型连接测试失败: providerId={}, modelId={}, error={}",
                providerId, modelId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取提供商，不存在则抛出异常
     *
     * @param providerId 提供商 ID
     * @return 提供商实体
     * @throws LlmProviderNotFoundException 提供商不存在时抛出
     */
    private LlmProvider getProviderOrThrow(Long providerId) {
        LlmProvider provider = providerMapper.selectById(providerId);
        if (provider == null) {
            throw new LlmProviderNotFoundException(providerId);
        }
        return provider;
    }

    /**
     * 将实体转换为 DTO
     *
     * @param entity 模型实体
     * @return ModelInfo DTO
     */
    private ModelInfo convertToModelInfo(LlmModel entity) {
        return new ModelInfo(
            entity.getModelId(),
            entity.getModelName(),
            entity.getMaxTokens(),
            entity.getInputPrice(),
            entity.getOutputPrice(),
            entity.getCapabilities()
        );
    }

    /**
     * 获取提供商类型的默认模型 ID
     *
     * @param providerType 提供商类型
     * @return 默认模型 ID
     */
    private String getDefaultModelId(String providerType) {
        return switch (providerType.toLowerCase()) {
            case "qwen" -> "qwen-max";
            case "openai" -> "gpt-3.5-turbo";
            case "ollama" -> "llama2";
            default -> "unknown";
        };
    }
}