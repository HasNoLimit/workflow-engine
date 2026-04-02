package com.workflow.engine.publish;

import com.workflow.engine.dto.AgentEndpoint;
import com.workflow.engine.dto.PublishResult;
import com.workflow.engine.model.Agent;
import com.workflow.engine.model.WorkflowVersion;
import com.workflow.engine.service.AgentService;
import com.workflow.engine.service.WorkflowVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 对话智能体发布器
 * <p>
 * 负责对话智能体的发布和取消发布操作。
 * 发布后可通过 WebSocket 端点进行交互式对话。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DialogAgentPublisher {

    private final AgentService agentService;
    private final WorkflowVersionService workflowVersionService;

    /**
     * 发布对话智能体
     * <p>
     * 流程：
     * 1. 验证智能体类型是否为 DIALOG
     * 2. 验证关联的工作流版本是否存在
     * 3. 构建 WebSocket 端点地址
     * </p>
     *
     * @param agent 智能体对象
     * @return 发布结果
     */
    public PublishResult publish(Agent agent) {
        log.info("发布对话智能体: agentId={}, name={}", agent.getId(), agent.getName());

        // 1. 验证智能体类型
        if (!Agent.TYPE_DIALOG.equals(agent.getType())) {
            return PublishResult.failure("智能体类型不是对话类型");
        }

        // 2. 验证工作流版本存在
        WorkflowVersion version = workflowVersionService.getVersion(
            agent.getWorkflowId(), agent.getWorkflowVersion());
        if (version == null) {
            return PublishResult.failure("工作流版本不存在");
        }

        // 3. 构建发布信息 - WebSocket 端点
        String wsEndpoint = "/ws/agent/" + agent.getId();

        log.info("对话智能体发布成功: agentId={}, endpoint={}", agent.getId(), wsEndpoint);

        return PublishResult.success()
            .agentId(agent.getId())
            .endpoint(wsEndpoint)
            .apiKey(agent.getApiKey())
            .message("对话智能体发布成功");
    }

    /**
     * 获取对话智能体端点信息
     * <p>
     * 返回 WebSocket 连接地址和 API 密钥
     * </p>
     *
     * @param agent 智能体对象
     * @return 端点信息
     */
    public AgentEndpoint getEndpoint(Agent agent) {
        return AgentEndpoint.builder()
            .agentId(agent.getId())
            .type(Agent.TYPE_DIALOG)
            .endpoint("/ws/agent/" + agent.getId())
            .apiKey(agent.getApiKey())
            .build();
    }
}