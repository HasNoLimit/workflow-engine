package com.workflow.engine.publish;

import com.workflow.engine.dto.AgentEndpoint;
import com.workflow.engine.dto.PublishResult;
import com.workflow.engine.model.Agent;
import com.workflow.engine.model.WorkflowVersion;
import com.workflow.engine.service.AgentService;
import com.workflow.engine.service.WorkflowVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * API 智能体发布器
 * <p>
 * 负责 API 智能体的发布和取消发布操作。
 * 发布后可通过 REST API 调用，支持同步/异步执行和 Webhook 回调。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAgentPublisher {

    private final AgentService agentService;
    private final WorkflowVersionService workflowVersionService;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * 发布 API 智能体
     * <p>
     * 流程：
     * 1. 验证智能体类型是否为 API
     * 2. 验证关联的工作流版本是否存在
     * 3. 检查 Webhook URL 配置（可选）
     * 4. 构建 REST API 端点地址
     * </p>
     *
     * @param agent 智能体对象
     * @return 发布结果
     */
    public PublishResult publish(Agent agent) {
        log.info("发布 API 智能体: agentId={}, name={}", agent.getId(), agent.getName());

        // 1. 验证智能体类型
        if (!Agent.TYPE_API.equals(agent.getType())) {
            return PublishResult.failure("智能体类型不是 API 类型");
        }

        // 2. 验证工作流版本存在
        WorkflowVersion version = workflowVersionService.getVersion(
            agent.getWorkflowId(), agent.getWorkflowVersion());
        if (version == null) {
            return PublishResult.failure("工作流版本不存在");
        }

        // 3. 验证 Webhook URL（如果配置了）
        if (agent.getWebhookUrl() == null || agent.getWebhookUrl().isEmpty()) {
            log.warn("API 智能体未配置 Webhook URL: agentId={}", agent.getId());
        }

        // 4. 构建发布信息 - REST API 端点
        String apiEndpoint = "/api/execute/" + agent.getId();

        log.info("API 智能体发布成功: agentId={}, endpoint={}", agent.getId(), apiEndpoint);

        return PublishResult.success()
            .agentId(agent.getId())
            .endpoint(apiEndpoint)
            .apiKey(agent.getApiKey())
            .webhookUrl(agent.getWebhookUrl())
            .message("API 智能体发布成功");
    }

    /**
     * 获取 API 智能体端点信息
     * <p>
     * 返回 REST API 地址、API 密钥、Webhook URL 和超时配置
     * </p>
     *
     * @param agent 智能体对象
     * @return 端点信息
     */
    public AgentEndpoint getEndpoint(Agent agent) {
        return AgentEndpoint.builder()
            .agentId(agent.getId())
            .type(Agent.TYPE_API)
            .endpoint("/api/execute/" + agent.getId())
            .apiKey(agent.getApiKey())
            .webhookUrl(agent.getWebhookUrl())
            .timeoutSeconds(agent.getTimeoutSeconds())
            .build();
    }
}