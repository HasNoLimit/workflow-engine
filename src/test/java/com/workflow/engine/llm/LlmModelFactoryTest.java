package com.workflow.engine.llm;

import com.workflow.engine.exception.UnsupportedProviderTypeException;
import com.workflow.engine.model.LlmProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmModelFactory 单元测试
 * <p>
 * 测试 ChatModel 工厂类的创建和验证功能
 * </p>
 */
class LlmModelFactoryTest {

    private final LlmModelFactory modelFactory = new LlmModelFactory();

    @Test
    void shouldValidateSupportedProviderTypes() {
        // Then: 验证支持的类型
        assertTrue(modelFactory.isSupportedProviderType("qwen"));
        assertTrue(modelFactory.isSupportedProviderType("openai"));
        assertTrue(modelFactory.isSupportedProviderType("ollama"));

        // 大小写不敏感
        assertTrue(modelFactory.isSupportedProviderType("QWEN"));
        assertTrue(modelFactory.isSupportedProviderType("OpenAI"));
        assertTrue(modelFactory.isSupportedProviderType("OLLAMA"));
    }

    @Test
    void shouldValidateUnsupportedProviderTypes() {
        // Then: 验证不支持的类型
        assertFalse(modelFactory.isSupportedProviderType("claude"));
        assertFalse(modelFactory.isSupportedProviderType("gemini"));
        assertFalse(modelFactory.isSupportedProviderType("unsupported"));
        assertFalse(modelFactory.isSupportedProviderType(""));
        assertFalse(modelFactory.isSupportedProviderType(null));
    }

    @Test
    void shouldGetSupportedProviderTypesList() {
        // When: 获取支持的类型列表
        String[] types = modelFactory.getSupportedProviderTypes();

        // Then: 验证列表内容
        assertEquals(3, types.length);
        assertArrayEquals(new String[]{"qwen", "openai", "ollama"}, types);
    }

    @Test
    void shouldThrowForUnsupportedProviderType() {
        // Given: 不支持的提供商类型
        LlmProvider provider = new LlmProvider();
        provider.setProviderType("unsupported");
        provider.setApiKey("test-key");
        provider.setBaseUrl("http://test.com");

        // Then: 应抛出异常
        assertThrows(UnsupportedProviderTypeException.class,
            () -> modelFactory.createChatModel(provider, "test-model", null, null));
    }

    @Test
    void shouldCreateQwenChatModel() {
        // Given: 通义千问提供商
        LlmProvider provider = new LlmProvider();
        provider.setProviderType("qwen");
        provider.setApiKey("test-api-key");
        provider.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        provider.setTimeoutSeconds(60);

        // When: 创建 ChatModel
        var chatModel = modelFactory.createChatModel(provider, "qwen-max", 4096, 0.7);

        // Then: 验证创建成功
        assertNotNull(chatModel);
    }

    @Test
    void shouldCreateOpenAiChatModel() {
        // Given: OpenAI 提供商
        LlmProvider provider = new LlmProvider();
        provider.setProviderType("openai");
        provider.setApiKey("test-api-key");
        provider.setBaseUrl("https://api.openai.com/v1");
        provider.setTimeoutSeconds(60);

        // When: 创建 ChatModel
        var chatModel = modelFactory.createChatModel(provider, "gpt-4", 4096, 0.7);

        // Then: 验证创建成功
        assertNotNull(chatModel);
    }

    @Test
    void shouldCreateOllamaChatModel() {
        // Given: Ollama 提供商（不需要 API Key）
        LlmProvider provider = new LlmProvider();
        provider.setProviderType("ollama");
        provider.setApiKey(null); // Ollama 不需要 API Key
        provider.setBaseUrl("http://localhost:11434");
        provider.setTimeoutSeconds(120);

        // When: 创建 ChatModel
        var chatModel = modelFactory.createChatModel(provider, "llama2", null, 0.7);

        // Then: 验证创建成功
        assertNotNull(chatModel);
    }

    @Test
    void shouldCreateChatModelWithDefaultParameters() {
        // Given: 提供商（无自定义参数）
        LlmProvider provider = new LlmProvider();
        provider.setProviderType("openai");
        provider.setApiKey("test-key");
        provider.setBaseUrl("http://localhost:8080");
        provider.setTimeoutSeconds(null); // 使用默认超时

        // When: 创建 ChatModel（使用 null 参数）
        var chatModel = modelFactory.createChatModel(provider, "test-model", null, null);

        // Then: 验证创建成功（使用默认值）
        assertNotNull(chatModel);
    }

    @Test
    void shouldHandleCaseInsensitiveProviderType() {
        // Given: 大写提供商类型
        LlmProvider provider = new LlmProvider();
        provider.setProviderType("QWEN"); // 大写
        provider.setApiKey("test-key");
        provider.setBaseUrl("http://test.com");
        provider.setTimeoutSeconds(60);

        // When: 创建 ChatModel
        var chatModel = modelFactory.createChatModel(provider, "qwen-max", null, null);

        // Then: 验证创建成功（大小写不敏感）
        assertNotNull(chatModel);
    }
}