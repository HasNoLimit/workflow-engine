package com.workflow.engine.publish;

import com.workflow.engine.dto.AgentEndpoint;
import com.workflow.engine.dto.PublishResult;
import com.workflow.engine.model.Agent;
import com.workflow.engine.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 智能体发布服务实现
 * <p>
 * 提供智能体发布的核心业务逻辑，根据智能体类型路由到对应的发布器。
 * 发布成功后自动激活智能体，取消发布后自动停用智能体。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {

    private final AgentService agentService;
    private final DialogAgentPublisher dialogPublisher;
    private final ApiAgentPublisher apiPublisher;

    /**
     * 发布智能体
     * <p>
     * 流程：
     * 1. 获取智能体信息
     * 2. 根据类型选择发布器（DIALOG 或 API）
     * 3. 执行发布验证和端点构建
     * 4. 发布成功则激活智能体
     * </p>
     *
     * @param agentId 智能体ID
     * @return 发布结果
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    @Override
    @Transactional
    public PublishResult publish(Long agentId) {
        log.info("开始发布智能体: agentId={}", agentId);

        // 1. 获取智能体
        Agent agent = agentService.getAgentById(agentId);

        // 2. 根据类型选择发布器
        PublishResult result;
        if (Agent.TYPE_DIALOG.equals(agent.getType())) {
            result = dialogPublisher.publish(agent);
        } else if (Agent.TYPE_API.equals(agent.getType())) {
            result = apiPublisher.publish(agent);
        } else {
            return PublishResult.failure("未知的智能体类型: " + agent.getType());
        }

        // 3. 发布成功则激活智能体
        if (result.isSuccess()) {
            agentService.activate(agentId);
            log.info("智能体发布并激活成功: agentId={}", agentId);
        }

        return result;
    }

    /**
     * 取消发布智能体
     * <p>
     * 流程：
     * 1. 获取智能体信息
     * 2. 停用智能体
     * </p>
     *
     * @param agentId 智能体ID
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    @Override
    @Transactional
    public void unpublish(Long agentId) {
        log.info("开始取消发布智能体: agentId={}", agentId);

        Agent agent = agentService.getAgentById(agentId);

        // 停用智能体
        agentService.deactivate(agentId);

        log.info("取消发布智能体成功: agentId={}, name={}", agentId, agent.getName());
    }

    /**
     * 检查智能体是否已发布
     * <p>
     * 通过检查智能体状态是否为 ACTIVE 来判断
     * </p>
     *
     * @param agentId 智能体ID
     * @return 是否已发布
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    @Override
    public boolean isPublished(Long agentId) {
        Agent agent = agentService.getAgentById(agentId);
        boolean published = Agent.STATUS_ACTIVE.equals(agent.getStatus());
        log.debug("检查智能体发布状态: agentId={}, published={}", agentId, published);
        return published;
    }

    /**
     * 获取智能体端点信息
     * <p>
     * 根据智能体类型获取对应的端点信息
     * </p>
     *
     * @param agentId 智能体ID
     * @return 端点信息
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     * @throws IllegalStateException                                     智能体类型未知
     */
    @Override
    public AgentEndpoint getEndpoint(Long agentId) {
        Agent agent = agentService.getAgentById(agentId);

        if (Agent.TYPE_DIALOG.equals(agent.getType())) {
            return dialogPublisher.getEndpoint(agent);
        } else if (Agent.TYPE_API.equals(agent.getType())) {
            return apiPublisher.getEndpoint(agent);
        }

        throw new IllegalStateException("未知的智能体类型: " + agent.getType());
    }
}