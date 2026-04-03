package com.workflow.engine.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通义千问（Qwen）配置属性
 * <p>
 * 用于配置通义千问提供商的默认参数。
 * 可通过 application.yml 中的 workflow.llm.qwen 配置。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "workflow.llm.qwen")
public class QwenConfig {

    /** 默认基础 URL */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 默认模型 */
    private String defaultModel = "qwen-max";

    /** 默认最大 Token 数 */
    private int defaultMaxTokens = 4096;

    /** 默认温度参数 */
    private double defaultTemperature = 0.7;

    /** 默认超时时间（秒） */
    private int defaultTimeoutSeconds = 60;
}