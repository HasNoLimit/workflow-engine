package com.workflow.engine.controller;

import com.workflow.engine.llm.LlmProviderService;
import com.workflow.engine.llm.dto.ModelInfo;
import com.workflow.engine.llm.dto.ProviderConfig;
import com.workflow.engine.model.LlmProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LLM 提供商管理控制器
 * <p>
 * 提供 LLM 提供商和模型的 CRUD API，支持：
 * <ul>
 *     <li>提供商创建、查询、更新、删除</li>
 *     <li>模型配置管理</li>
 *     <li>连接测试</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/llm/providers")
@RequiredArgsConstructor
public class LlmProviderController {

    private final LlmProviderService providerService;

    /**
     * 创建 LLM 提供商
     *
     * @param config 提供商配置
     * @return 创建的提供商 ID
     */
    @PostMapping
    public ResponseEntity<Long> createProvider(@Valid @RequestBody ProviderConfig config) {
        Long id = providerService.createProvider(config);
        return ResponseEntity.ok(id);
    }

    /**
     * 获取所有提供商列表
     *
     * @return 提供商列表
     */
    @GetMapping
    public ResponseEntity<List<LlmProvider>> listProviders() {
        return ResponseEntity.ok(providerService.listProviders());
    }

    /**
     * 按类型获取提供商列表
     *
     * @param providerType 提供商类型 (qwen, openai, ollama)
     * @return 匹配的提供商列表
     */
    @GetMapping("/type/{providerType}")
    public ResponseEntity<List<LlmProvider>> listProvidersByType(@PathVariable String providerType) {
        return ResponseEntity.ok(providerService.listProvidersByType(providerType));
    }

    /**
     * 获取单个提供商详情
     *
     * @param providerId 提供商 ID
     * @return 提供商信息
     */
    @GetMapping("/{providerId}")
    public ResponseEntity<LlmProvider> getProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(providerService.getProvider(providerId));
    }

    /**
     * 更新提供商配置
     *
     * @param providerId 提供商 ID
     * @param config      新的配置
     * @return 无内容响应
     */
    @PutMapping("/{providerId}")
    public ResponseEntity<Void> updateProvider(@PathVariable Long providerId,
                                                @Valid @RequestBody ProviderConfig config) {
        providerService.updateProvider(providerId, config);
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除提供商
     *
     * @param providerId 提供商 ID
     * @return 无内容响应
     */
    @DeleteMapping("/{providerId}")
    public ResponseEntity<Void> deleteProvider(@PathVariable Long providerId) {
        providerService.deleteProvider(providerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取提供商的模型列表
     *
     * @param providerId 提供商 ID
     * @return 模型信息列表
     */
    @GetMapping("/{providerId}/models")
    public ResponseEntity<List<ModelInfo>> listModels(@PathVariable Long providerId) {
        return ResponseEntity.ok(providerService.listModels(providerId));
    }

    /**
     * 添加模型到提供商
     *
     * @param providerId 提供商 ID
     * @param model       模型信息
     * @return 创建的模型 ID
     */
    @PostMapping("/{providerId}/models")
    public ResponseEntity<Long> addModel(@PathVariable Long providerId,
                                         @Valid @RequestBody ModelInfo model) {
        Long id = providerService.addModel(providerId, model);
        return ResponseEntity.ok(id);
    }

    /**
     * 更新模型配置
     *
     * @param modelId 模型 ID（数据库 ID）
     * @param model   新的模型信息
     * @return 无内容响应
     */
    @PutMapping("/{providerId}/models/{modelId}")
    public ResponseEntity<Void> updateModel(@PathVariable Long modelId,
                                            @Valid @RequestBody ModelInfo model) {
        providerService.updateModel(modelId, model);
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除模型
     *
     * @param modelId 模型 ID（数据库 ID）
     * @return 无内容响应
     */
    @DeleteMapping("/{providerId}/models/{modelId}")
    public ResponseEntity<Void> deleteModel(@PathVariable Long modelId) {
        providerService.deleteModel(modelId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 测试提供商连接
     *
     * @param providerId 提供商 ID
     * @return 连接是否成功
     */
    @PostMapping("/{providerId}/test")
    public ResponseEntity<Boolean> testConnection(@PathVariable Long providerId) {
        boolean success = providerService.testConnection(providerId);
        return ResponseEntity.ok(success);
    }

    /**
     * 测试特定模型连接
     *
     * @param providerId 提供商 ID
     * @param modelId    模型标识
     * @return 连接是否成功
     */
    @PostMapping("/{providerId}/test/{modelId}")
    public ResponseEntity<Boolean> testModelConnection(@PathVariable Long providerId,
                                                       @PathVariable String modelId) {
        boolean success = providerService.testModelConnection(providerId, modelId);
        return ResponseEntity.ok(success);
    }
}