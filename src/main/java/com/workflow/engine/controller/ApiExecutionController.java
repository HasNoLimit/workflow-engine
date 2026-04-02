package com.workflow.engine.controller;

import com.workflow.engine.dto.AsyncExecutionResponse;
import com.workflow.engine.dto.ExecutionRequest;
import com.workflow.engine.dto.ExecutionResponse;
import com.workflow.engine.service.ApiExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API 智能体执行控制器
 * <p>
 * 提供外部系统调用智能体的 REST API 端点
 * 支持 API Key 鉴权、同步/异步执行模式
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
public class ApiExecutionController {

    /** API 执行服务 */
    private final ApiExecutionService apiExecutionService;

    /**
     * 同步执行智能体
     * <p>
     * 等待工作流执行完成后返回结果
     * 适用于执行时间较短的工作流
     * </p>
     *
     * @param agentId     智能体ID
     * @param request     执行请求，包含输入参数
     * @param httpRequest HTTP请求，用于提取 API Key
     * @return 执行结果
     */
    @PostMapping("/{agentId}/sync")
    public ResponseEntity<ExecutionResponse> executeSync(
            @PathVariable Long agentId,
            @Valid @RequestBody ExecutionRequest request,
            HttpServletRequest httpRequest) {

        // 从请求头提取 API Key
        String apiKey = extractApiKey(httpRequest);
        log.info("同步执行 API 智能体: agentId={}", agentId);

        ExecutionResponse response = apiExecutionService.executeSync(agentId, apiKey, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 异步执行智能体
     * <p>
     * 立即返回执行ID，执行完成后通过 Webhook 回调
     * 适用于执行时间较长的工作流
     * </p>
     *
     * @param agentId     智能体ID
     * @param request     执行请求，包含输入参数
     * @param httpRequest HTTP请求，用于提取 API Key
     * @return 异步执行响应，包含执行ID
     */
    @PostMapping("/{agentId}/async")
    public ResponseEntity<AsyncExecutionResponse> executeAsync(
            @PathVariable Long agentId,
            @Valid @RequestBody ExecutionRequest request,
            HttpServletRequest httpRequest) {

        // 从请求头提取 API Key
        String apiKey = extractApiKey(httpRequest);
        log.info("异步执行 API 智能体: agentId={}", agentId);

        AsyncExecutionResponse response = apiExecutionService.executeAsync(agentId, apiKey, request);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * 查询执行状态
     * <p>
     * 根据执行ID查询执行记录的当前状态和结果
     * </p>
     *
     * @param executionId 执行ID
     * @return 执行状态信息
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ExecutionResponse> getExecutionStatus(@PathVariable Long executionId) {
        log.info("查询执行状态: executionId={}", executionId);

        ExecutionResponse response = apiExecutionService.getExecutionStatus(executionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 从请求头提取 API Key
     * <p>
     * 支持两种方式：
     * 1. Authorization: Bearer {apiKey}
     * 2. X-API-Key: {apiKey}
     * </p>
     *
     * @param request HTTP请求
     * @return API Key，如果不存在则返回 null
     */
    private String extractApiKey(HttpServletRequest request) {
        // 优先从 Authorization 头提取（Bearer Token 格式）
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 从 X-API-Key 头提取
        return request.getHeader("X-API-Key");
    }
}