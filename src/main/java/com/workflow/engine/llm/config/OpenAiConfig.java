package com.workflow.engine.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenAI 配置属性
 * <p>
 * 用于配置 OpenAI 提供商的默认参数。
 * 可通过 application.yml 中的 workflow.llm.openai 配置。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "workflow.llm.openai")
public class OpenAiConfig {

    /** 默认基础 URL */
    private String baseUrl = "https://api.openai.com/v1";

    /** 默认模型 */
    private String defaultModel = "gpt-3.5-turbo";

    /** 默认最大 Token 数 */
    private int defaultMaxTokens = 4096;

    /** 默认温度参数 */
    private double defaultTemperature = 0.7;

    /** 默认超时时间（秒） */
    private int defaultTimeoutSeconds = 60;
}