package com.workflow.engine.service;

import com.workflow.engine.dto.AsyncExecutionResponse;
import com.workflow.engine.dto.ExecutionRequest;
import com.workflow.engine.dto.ExecutionResponse;

/**
 * API 智能体执行服务接口
 * <p>
 * 提供外部系统调用智能体的执行功能，支持同步和异步两种模式
 * </p>
 */
public interface ApiExecutionService {

    /**
     * 同步执行智能体
     * <p>
     * 等待工作流执行完成后返回结果，适用于执行时间较短的工作流
     * </p>
     *
     * @param agentId 智能体ID
     * @param apiKey  API密钥，用于鉴权
     * @param request 执行请求，包含输入参数
     * @return 执行结果
     * @throws com.workflow.engine.exception.AgentNotFoundException      智能体不存在
     * @throws com.workflow.engine.exception.InvalidAgentTypeException   智能体类型不是 API 类型
     * @throws com.workflow.engine.exception.AgentNotActiveException     智能体未激活
     * @throws com.workflow.engine.exception.InvalidApiKeyException      API Key 无效
     */
    ExecutionResponse executeSync(Long agentId, String apiKey, ExecutionRequest request);

    /**
     * 异步执行智能体
     * <p>
     * 立即返回执行ID，执行完成后通过 Webhook 回调
     * 适用于执行时间较长的工作流
     * </p>
     *
     * @param agentId 智能体ID
     * @param apiKey  API密钥，用于鉴权
     * @param request 执行请求，包含输入参数
     * @return 异步执行响应，包含执行ID和当前状态
     * @throws com.workflow.engine.exception.AgentNotFoundException      智能体不存在
     * @throws com.workflow.engine.exception.InvalidAgentTypeException   智能体类型不是 API 类型
     * @throws com.workflow.engine.exception.AgentNotActiveException     智能体未激活
     * @throws com.workflow.engine.exception.InvalidApiKeyException      API Key 无效
     */
    AsyncExecutionResponse executeAsync(Long agentId, String apiKey, ExecutionRequest request);

    /**
     * 查询执行状态
     * <p>
     * 根据执行ID查询执行记录的当前状态和结果
     * </p>
     *
     * @param executionId 执行ID
     * @return 执行状态信息
     * @throws com.workflow.engine.exception.WorkflowExecutionException 执行记录不存在
     */
    ExecutionResponse getExecutionStatus(Long executionId);
}