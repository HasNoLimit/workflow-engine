package com.workflow.engine.llm.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 模型信息 DTO
 * <p>
 * 用于描述 LLM 模型的详细信息和能力
 * </p>
 *
 * @param modelId      模型标识，如 qwen-max, gpt-4
 * @param modelName    模型显示名称
 * @param maxTokens    最大 Token 数
 * @param inputPrice   输入价格（每千 Token）
 * @param outputPrice  输出价格（每千 Token）
 * @param capabilities 能力标签
 */
public record ModelInfo(
    /** 模型标识 */
    String modelId,

    /** 模型名称 */
    String modelName,

    /** 最大 Token 数 */
    Integer maxTokens,

    /** 输入价格（每千Token） */
    BigDecimal inputPrice,

    /** 输出价格（每千Token） */
    BigDecimal outputPrice,

    /** 能力标签 */
    Map<String, Object> capabilities
) {
    /**
     * 创建基础模型信息
     *
     * @param modelId   模型标识
     * @param modelName 模型名称
     * @return ModelInfo 实例
     */
    public static ModelInfo of(String modelId, String modelName) {
        return new ModelInfo(modelId, modelName, null, null, null, null);
    }

    /**
     * 创建带最大 Token 数的模型信息
     *
     * @param modelId   模型标识
     * @param modelName 模型名称
     * @param maxTokens 最大 Token 数
     * @return ModelInfo 实例
     */
    public static ModelInfo of(String modelId, String modelName, Integer maxTokens) {
        return new ModelInfo(modelId, modelName, maxTokens, null, null, null);
    }
}