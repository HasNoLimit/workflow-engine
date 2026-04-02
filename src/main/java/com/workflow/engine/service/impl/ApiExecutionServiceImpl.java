package com.workflow.engine.service.impl;

import com.workflow.engine.dto.AsyncExecutionResponse;
import com.workflow.engine.dto.ExecutionRequest;
import com.workflow.engine.dto.ExecutionResponse;
import com.workflow.engine.engine.model.WorkflowJSON;
import com.workflow.engine.exception.*;
import com.workflow.engine.executor.ExecutionResult;
import com.workflow.engine.executor.WorkflowExecutor;
import com.workflow.engine.model.Agent;
import com.workflow.engine.model.WorkflowExecution;
import com.workflow.engine.model.WorkflowVersion;
import com.workflow.engine.service.AgentService;
import com.workflow.engine.service.ApiExecutionService;
import com.workflow.engine.service.WorkflowExecutionService;
import com.workflow.engine.service.WorkflowVersionService;
import com.workflow.engine.webhook.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * API 智能体执行服务实现
 * <p>
 * 实现 API 智能体的同步和异步执行功能，
 * 支持 API Key 鉴权和 Webhook 回调
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiExecutionServiceImpl implements ApiExecutionService {

    /** 智能体服务，用于查询和验证智能体 */
    private final AgentService agentService;

    /** 工作流版本服务，用于获取工作流定义 */
    private final WorkflowVersionService workflowVersionService;

    /** 工作流执行服务，用于管理执行记录 */
    private final WorkflowExecutionService workflowExecutionService;

    /** 工作流执行器，用于执行工作流逻辑 */
    private final WorkflowExecutor workflowExecutor;

    /** Webhook 服务，用于异步回调 */
    private final WebhookService webhookService;

    @Override
    @Transactional
    public ExecutionResponse executeSync(Long agentId, String apiKey, ExecutionRequest request) {
        log.info("开始同步执行 API 智能体: agentId={}", agentId);

        // 1. 验证智能体（类型、状态、API Key）
        Agent agent = validateAgent(agentId, apiKey);

        // 2. 获取工作流定义（画布 JSON）
        WorkflowJSON canvas = getWorkflowCanvas(agent);

        // 3. 创建执行记录
        WorkflowExecution execution = workflowExecutionService.createExecution(
                agent.getWorkflowId(), agent.getId(), request.input());

        // 4. 执行工作流
        long startTime = System.currentTimeMillis();
        ExecutionResult result = workflowExecutor.execute(
                agent.getWorkflowId(), canvas, request.input());
        long duration = System.currentTimeMillis() - startTime;

        // 5. 更新执行记录状态
        if (result.isSuccess()) {
            workflowExecutionService.markSuccess(execution.getId(), result.getOutput(), duration);
        } else {
            workflowExecutionService.markFailed(execution.getId(), result.getErrorMessage(), duration);
        }

        // 6. 重新获取更新后的执行记录
        WorkflowExecution updatedExecution = workflowExecutionService.getById(execution.getId());

        // 7. 构建并返回响应
        log.info("同步执行完成: agentId={}, executionId={}, success={}, durationMs={}",
                agentId, execution.getId(), result.isSuccess(), duration);

        return buildExecutionResponse(updatedExecution, result, duration);
    }

    @Override
    @Transactional
    public AsyncExecutionResponse executeAsync(Long agentId, String apiKey, ExecutionRequest request) {
        log.info("开始异步执行 API 智能体: agentId={}", agentId);

        // 1. 验证智能体
        Agent agent = validateAgent(agentId, apiKey);

        // 2. 创建执行记录（状态为 RUNNING）
        WorkflowExecution execution = workflowExecutionService.createExecution(
                agent.getWorkflowId(), agent.getId(), request.input());

        // 3. 异步执行工作流
        executeAsyncInternal(agent, execution, request.input());

        // 4. 立即返回执行ID
        log.info("异步执行已提交: agentId={}, executionId={}", agentId, execution.getId());

        return AsyncExecutionResponse.builder()
                .executionId(execution.getId())
                .status(WorkflowExecution.STATUS_RUNNING)
                .message("执行已提交，完成后将回调 Webhook")
                .build();
    }

    /**
     * 异步执行工作流内部方法
     * <p>
     * 使用 @Async 注解在独立线程中执行工作流，
     * 执行完成后更新执行记录并发送 Webhook 回调
     * </p>
     *
     * @param agent     智能体信息
     * @param execution 执行记录
     * @param input     输入参数
     */
    @Async
    protected void executeAsyncInternal(Agent agent, WorkflowExecution execution, Map<String, Object> input) {
        log.info("异步工作流开始执行: executionId={}", execution.getId());

        try {
            // 获取工作流定义
            WorkflowJSON canvas = getWorkflowCanvas(agent);

            // 执行工作流
            long startTime = System.currentTimeMillis();
            ExecutionResult result = workflowExecutor.execute(
                    agent.getWorkflowId(), canvas, input);
            long duration = System.currentTimeMillis() - startTime;

            // 更新执行记录
            if (result.isSuccess()) {
                workflowExecutionService.markSuccess(execution.getId(), result.getOutput(), duration);
            } else {
                workflowExecutionService.markFailed(execution.getId(), result.getErrorMessage(), duration);
            }

            // Webhook 回调（如果配置了 Webhook URL）
            if (agent.getWebhookUrl() != null && !agent.getWebhookUrl().isEmpty()) {
                // 更新执行记录获取最终状态
                WorkflowExecution updatedExecution = workflowExecutionService.getById(execution.getId());
                webhookService.notifyCompletion(agent, updatedExecution, result);
            }

            log.info("异步执行完成: executionId={}, success={}, durationMs={}",
                    execution.getId(), result.isSuccess(), duration);

        } catch (Exception e) {
            log.error("异步执行失败: executionId={}, error={}", execution.getId(), e.getMessage(), e);
            workflowExecutionService.markFailed(execution.getId(), e.getMessage(), 0L);
        }
    }

    @Override
    public ExecutionResponse getExecutionStatus(Long executionId) {
        log.info("查询执行状态: executionId={}", executionId);

        WorkflowExecution execution = workflowExecutionService.getExecutionById(executionId);

        return ExecutionResponse.builder()
                .executionId(execution.getId())
                .status(execution.getStatus())
                .output(execution.getOutput())
                .durationMs(execution.getDurationMs())
                .completedAt(execution.getCompletedAt())
                .build();
    }

    // ========== 私有辅助方法 ==========

    /**
     * 验证智能体
     * <p>
     * 检查智能体类型、状态和 API Key，确保智能体可以执行
     * </p>
     *
     * @param agentId 智能体ID
     * @param apiKey  API密钥
     * @return 验证通过的智能体
     */
    private Agent validateAgent(Long agentId, String apiKey) {
        // 获取智能体
        Agent agent = agentService.getAgentById(agentId);

        // 验证类型：必须是 API 类型
        if (!Agent.TYPE_API.equals(agent.getType())) {
            throw new InvalidAgentTypeException(agentId, Agent.TYPE_API, agent.getType());
        }

        // 验证状态：必须是激活状态
        if (!Agent.STATUS_ACTIVE.equals(agent.getStatus())) {
            throw new AgentNotActiveException(agentId, agent.getStatus());
        }

        // 验证 API Key
        if (!agent.getApiKey().equals(apiKey)) {
            throw new InvalidApiKeyException(agentId);
        }

        return agent;
    }

    /**
     * 获取工作流画布定义
     * <p>
     * 根据智能体配置的工作流ID和版本号获取工作流定义
     * </p>
     *
     * @param agent 智能体信息
     * @return 工作流画布 JSON
     */
    private WorkflowJSON getWorkflowCanvas(Agent agent) {
        WorkflowVersion version = workflowVersionService.getVersion(
                agent.getWorkflowId(), agent.getWorkflowVersion());
        if (version == null) {
            throw new WorkflowVersionNotFoundException(agent.getWorkflowId(), agent.getWorkflowVersion());
        }
        return version.getCanvas();
    }

    /**
     * 构建执行响应
     * <p>
     * 将执行记录和执行结果合并为响应对象
     * </p>
     *
     * @param execution 执行记录
     * @param result    执行结果
     * @param duration  执行耗时
     * @return 执行响应
     */
    private ExecutionResponse buildExecutionResponse(
            WorkflowExecution execution, ExecutionResult result, long duration) {
        return ExecutionResponse.builder()
                .executionId(execution.getId())
                .status(execution.getStatus())
                .output(result.getOutput())
                .executionPath(result.getExecutionPath())
                .durationMs(duration)
                .completedAt(execution.getCompletedAt())
                .errorMessage(result.getErrorMessage())
                .build();
    }
}