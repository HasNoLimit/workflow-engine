package com.workflow.engine.llm;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmProviderService 集成测试
 * <p>
 * 测试 LLM 提供商和模型的 CRUD 操作
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LlmProviderServiceTest {

    @Autowired
    private LlmProviderService providerService;

    @Autowired
    private LlmProviderMapper providerMapper;

    @Autowired
    private LlmModelMapper modelMapper;

    @Autowired
    private LlmModelFactory modelFactory;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        modelMapper.delete(null);
        providerMapper.delete(null);
    }

    @Test
    void shouldCreateQwenProvider() {
        // Given: 通义千问配置
        ProviderConfig config = new ProviderConfig(
            "阿里云通义千问",
            "qwen",
            "test-api-key-123",
            "https://dashscope.aliyuncs.com/compatible-mode/v1",
            60,
            null
        );

        // When: 创建提供商
        Long providerId = providerService.createProvider(config);

        // Then: 验证创建结果
        assertNotNull(providerId);
        LlmProvider provider = providerService.getProvider(providerId);
        assertEquals("阿里云通义千问", provider.getName());
        assertEquals("qwen", provider.getProviderType());
        assertEquals("test-api-key-123", provider.getApiKey());
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", provider.getBaseUrl());
        assertEquals(60, provider.getTimeoutSeconds());
    }

    @Test
    void shouldCreateOpenAiProvider() {
        // Given: OpenAI 配置
        ProviderConfig config = new ProviderConfig(
            "OpenAI GPT",
            "openai",
            "sk-test-key-456",
            "https://api.openai.com/v1",
            30,
            null
        );

        // When: 创建提供商
        Long providerId = providerService.createProvider(config);

        // Then: 验证创建结果
        assertNotNull(providerId);
        LlmProvider provider = providerService.getProvider(providerId);
        assertEquals("OpenAI GPT", provider.getName());
        assertEquals("openai", provider.getProviderType());
    }

    @Test
    void shouldCreateOllamaProvider() {
        // Given: Ollama 配置（不需要 API Key）
        ProviderConfig config = new ProviderConfig(
            "本地 Ollama",
            "ollama",
            null,
            "http://localhost:11434",
            120,
            null
        );

        // When: 创建提供商
        Long providerId = providerService.createProvider(config);

        // Then: 验证创建结果
        assertNotNull(providerId);
        LlmProvider provider = providerService.getProvider(providerId);
        assertEquals("本地 Ollama", provider.getName());
        assertEquals("ollama", provider.getProviderType());
        assertNull(provider.getApiKey());
    }

    @Test
    void shouldThrowWhenUnsupportedProviderType() {
        // Given: 不支持的提供商类型
        ProviderConfig config = new ProviderConfig(
            "测试提供商",
            "unsupported",
            "test-key",
            "http://test.com",
            60,
            null
        );

        // Then: 应抛出异常
        assertThrows(UnsupportedProviderTypeException.class,
            () -> providerService.createProvider(config));
    }

    @Test
    void shouldUpdateProvider() {
        // Given: 创建提供商
        ProviderConfig createConfig = new ProviderConfig(
            "原名称",
            "qwen",
            "old-key",
            "http://old-url.com",
            30,
            null
        );
        Long providerId = providerService.createProvider(createConfig);

        // When: 更新提供商
        ProviderConfig updateConfig = new ProviderConfig(
            "新名称",
            "openai",
            "new-key",
            "http://new-url.com",
            60,
            null
        );
        providerService.updateProvider(providerId, updateConfig);

        // Then: 验证更新结果
        LlmProvider provider = providerService.getProvider(providerId);
        assertEquals("新名称", provider.getName());
        assertEquals("openai", provider.getProviderType());
        assertEquals("new-key", provider.getApiKey());
        assertEquals("http://new-url.com", provider.getBaseUrl());
        assertEquals(60, provider.getTimeoutSeconds());
    }

    @Test
    void shouldThrowWhenUpdateNonExistentProvider() {
        // Given: 不存在的提供商 ID
        ProviderConfig config = new ProviderConfig(
            "测试",
            "qwen",
            "key",
            "http://test.com",
            60,
            null
        );

        // Then: 应抛出异常
        assertThrows(LlmProviderNotFoundException.class,
            () -> providerService.updateProvider(999L, config));
    }

    @Test
    void shouldDeleteProvider() {
        // Given: 创建提供商
        ProviderConfig config = new ProviderConfig(
            "待删除",
            "qwen",
            "key",
            "http://test.com",
            60,
            null
        );
        Long providerId = providerService.createProvider(config);

        // When: 删除提供商
        providerService.deleteProvider(providerId);

        // Then: 应抛出异常（已不存在）
        assertThrows(LlmProviderNotFoundException.class,
            () -> providerService.getProvider(providerId));
    }

    @Test
    void shouldDeleteProviderWithModels() {
        // Given: 创建提供商和模型
        ProviderConfig config = new ProviderConfig(
            "测试提供商",
            "qwen",
            "key",
            "http://test.com",
            60,
            null
        );
        Long providerId = providerService.createProvider(config);

        ModelInfo modelInfo = ModelInfo.of("qwen-max", "通义千问 MAX", 8192);
        Long modelId = providerService.addModel(providerId, modelInfo);

        // When: 删除提供商
        providerService.deleteProvider(providerId);

        // Then: 模型也应被删除
        LlmModel model = modelMapper.selectById(modelId);
        assertNull(model);
    }

    @Test
    void shouldListProviders() {
        // Given: 创建多个提供商
        providerService.createProvider(new ProviderConfig("Qwen", "qwen", "key1", "http://qwen.com", 60, null));
        providerService.createProvider(new ProviderConfig("OpenAI", "openai", "key2", "http://openai.com", 30, null));
        providerService.createProvider(new ProviderConfig("Ollama", "ollama", null, "http://localhost:11434", 120, null));

        // When: 获取所有提供商
        List<LlmProvider> providers = providerService.listProviders();

        // Then: 验证列表
        assertEquals(3, providers.size());
    }

    @Test
    void shouldListProvidersByType() {
        // Given: 创建多个不同类型的提供商
        providerService.createProvider(new ProviderConfig("Qwen1", "qwen", "key1", "http://qwen.com", 60, null));
        providerService.createProvider(new ProviderConfig("Qwen2", "qwen", "key2", "http://qwen.com", 60, null));
        providerService.createProvider(new ProviderConfig("OpenAI", "openai", "key3", "http://openai.com", 30, null));

        // When: 按类型查询
        List<LlmProvider> qwenProviders = providerService.listProvidersByType("qwen");
        List<LlmProvider> openaiProviders = providerService.listProvidersByType("openai");

        // Then: 验证查询结果
        assertEquals(2, qwenProviders.size());
        assertEquals(1, openaiProviders.size());
    }

    @Test
    void shouldAddModel() {
        // Given: 创建提供商
        ProviderConfig config = new ProviderConfig(
            "测试",
            "qwen",
            "key",
            "http://test.com",
            60,
            null
        );
        Long providerId = providerService.createProvider(config);

        ModelInfo modelInfo = new ModelInfo(
            "qwen-max",
            "通义千问 MAX",
            8192,
            new BigDecimal("0.12"),
            new BigDecimal("0.12"),
            Map.of("reasoning", true)
        );

        // When: 添加模型
        Long modelId = providerService.addModel(providerId, modelInfo);

        // Then: 验证添加结果
        assertNotNull(modelId);
        List<ModelInfo> models = providerService.listModels(providerId);
        assertEquals(1, models.size());
        ModelInfo addedModel = models.get(0);
        assertEquals("qwen-max", addedModel.modelId());
        assertEquals("通义千问 MAX", addedModel.modelName());
        assertEquals(8192, addedModel.maxTokens());
        assertEquals(new BigDecimal("0.1200"), addedModel.inputPrice());
        assertEquals(new BigDecimal("0.1200"), addedModel.outputPrice());
    }

    @Test
    void shouldThrowWhenAddModelToNonExistentProvider() {
        // Given: 不存在的提供商 ID
        ModelInfo modelInfo = ModelInfo.of("test-model", "测试模型");

        // Then: 应抛出异常
        assertThrows(LlmProviderNotFoundException.class,
            () -> providerService.addModel(999L, modelInfo));
    }

    @Test
    void shouldUpdateModel() {
        // Given: 创建提供商和模型
        ProviderConfig config = new ProviderConfig("测试", "qwen", "key", "http://test.com", 60, null);
        Long providerId = providerService.createProvider(config);

        ModelInfo original = ModelInfo.of("qwen-max", "原模型", 4096);
        Long modelId = providerService.addModel(providerId, original);

        // When: 更新模型
        ModelInfo updated = new ModelInfo("qwen-max", "更新模型", 8192, null, null, null);
        providerService.updateModel(modelId, updated);

        // Then: 验证更新结果
        List<ModelInfo> models = providerService.listModels(providerId);
        assertEquals("更新模型", models.get(0).modelName());
        assertEquals(8192, models.get(0).maxTokens());
    }

    @Test
    void shouldDeleteModel() {
        // Given: 创建提供商和模型
        ProviderConfig config = new ProviderConfig("测试", "qwen", "key", "http://test.com", 60, null);
        Long providerId = providerService.createProvider(config);

        ModelInfo modelInfo = ModelInfo.of("qwen-max", "测试模型");
        Long modelId = providerService.addModel(providerId, modelInfo);

        // When: 删除模型
        providerService.deleteModel(modelId);

        // Then: 模型已不存在
        List<ModelInfo> models = providerService.listModels(providerId);
        assertEquals(0, models.size());
    }

    @Test
    void shouldThrowWhenDeleteNonExistentModel() {
        // Then: 应抛出异常
        assertThrows(LlmModelNotFoundException.class,
            () -> providerService.deleteModel(999L));
    }

    @Test
    void shouldThrowWhenGetNonExistentProvider() {
        // Then: 应抛出异常
        assertThrows(LlmProviderNotFoundException.class,
            () -> providerService.getProvider(999L));
    }

    @Test
    void shouldGetChatModelWithRegisteredModel() {
        // Given: 创建提供商和模型
        ProviderConfig config = new ProviderConfig(
            "测试",
            "openai",
            "test-key",
            "http://localhost:8080",  // 使用模拟 URL
            60,
            null
        );
        Long providerId = providerService.createProvider(config);

        ModelInfo modelInfo = ModelInfo.of("gpt-4", "GPT-4", 8192);
        providerService.addModel(providerId, modelInfo);

        // When: 获取 ChatModel
        ChatModel chatModel = providerService.getChatModel(providerId, "gpt-4");

        // Then: 验证 ChatModel 创建成功
        assertNotNull(chatModel);
    }

    @Test
    void shouldGetChatModelWithUnregisteredModel() {
        // Given: 创建提供商（没有注册模型）
        ProviderConfig config = new ProviderConfig(
            "测试",
            "openai",
            "test-key",
            "http://localhost:8080",
            60,
            null
        );
        Long providerId = providerService.createProvider(config);

        // When: 获取 ChatModel（使用未注册的模型 ID）
        ChatModel chatModel = providerService.getChatModel(providerId, "gpt-3.5-turbo");

        // Then: 验证 ChatModel 创建成功（使用默认参数）
        assertNotNull(chatModel);
    }

    @Test
    void shouldGetChatModelWithCustomParameters() {
        // Given: 创建提供商
        ProviderConfig config = new ProviderConfig(
            "测试",
            "ollama",
            null,
            "http://localhost:11434",
            60,
            null
        );
        Long providerId = providerService.createProvider(config);

        // When: 获取 ChatModel（自定义参数）
        ChatModel chatModel = providerService.getChatModel(
            providerId, "llama2", 2048, 0.5);

        // Then: 验证 ChatModel 创建成功
        assertNotNull(chatModel);
    }

    @Test
    void shouldValidateProviderTypeSupport() {
        // Then: 验证支持的类型
        assertTrue(modelFactory.isSupportedProviderType("qwen"));
        assertTrue(modelFactory.isSupportedProviderType("openai"));
        assertTrue(modelFactory.isSupportedProviderType("ollama"));
        assertTrue(modelFactory.isSupportedProviderType("QWEN")); // 大小写不敏感
        assertFalse(modelFactory.isSupportedProviderType("unsupported"));
        assertFalse(modelFactory.isSupportedProviderType(null));
    }

    @Test
    void shouldGetSupportedProviderTypes() {
        // When: 获取支持的提供商类型列表
        String[] types = modelFactory.getSupportedProviderTypes();

        // Then: 验证列表
        assertEquals(3, types.length);
    }

    @Test
    void shouldCreateProviderWithOptions() {
        // Given: 带 JSON options 的配置
        ProviderConfig config = new ProviderConfig(
            "测试",
            "qwen",
            "key",
            "http://test.com",
            60,
            "{\"enableSearch\": true, \"searchCount\": 5}"
        );

        // When: 创建提供商
        Long providerId = providerService.createProvider(config);

        // Then: 验证 options 解析
        LlmProvider provider = providerService.getProvider(providerId);
        assertNotNull(provider.getOptions());
        assertEquals(true, provider.getOptions().get("enableSearch"));
        assertEquals(5, provider.getOptions().get("searchCount"));
    }
}