package com.workflow.engine.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ollama 配置属性
 * <p>
 * 用于配置 Ollama 提供商的默认参数。
 * 可通过 application.yml 中的 workflow.llm.ollama 配置。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "workflow.llm.ollama")
public class OllamaConfig {

    /** 默认基础 URL */
    private String baseUrl = "http://localhost:11434";

    /** 默认模型 */
    private String defaultModel = "llama2";

    /** 默认温度参数 */
    private double defaultTemperature = 0.7;

    /** 默认超时时间（秒） */
    private int defaultTimeoutSeconds = 60;
}