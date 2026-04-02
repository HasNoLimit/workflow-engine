package com.workflow.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workflow.engine.dto.AgentCreateRequest;
import com.workflow.engine.dto.AgentPublishRequest;
import com.workflow.engine.exception.AgentNotFoundException;
import com.workflow.engine.exception.WorkflowNotFoundException;
import com.workflow.engine.exception.WorkflowVersionNotFoundException;
import com.workflow.engine.mapper.AgentMapper;
import com.workflow.engine.model.Agent;
import com.workflow.engine.service.AgentService;
import com.workflow.engine.service.WorkflowService;
import com.workflow.engine.service.WorkflowVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 智能体服务实现
 * <p>
 * 提供智能体的创建、发布、激活/停用等核心功能实现
 * 继承 MyBatis-Plus ServiceImpl，复用基础 CRUD 操作
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    private final WorkflowService workflowService;
    private final WorkflowVersionService workflowVersionService;

    /**
     * 创建智能体
     * <p>
     * 流程：
     * 1. 验证工作流是否存在
     * 2. 验证工作流版本是否存在
     * 3. 验证智能体类型是否合法
     * 4. 创建智能体并生成 API Key
     * </p>
     */
    @Override
    @Transactional
    public Agent create(AgentCreateRequest request) {
        log.info("创建智能体: workflowId={}, version={}, name={}, type={}",
            request.workflowId(), request.workflowVersion(), request.name(), request.type());

        // 1. 验证工作流是否存在
        if (workflowService.getById(request.workflowId()) == null) {
            throw new WorkflowNotFoundException(request.workflowId());
        }

        // 2. 验证工作流版本是否存在
        if (workflowVersionService.getVersion(request.workflowId(), request.workflowVersion()) == null) {
            throw new WorkflowVersionNotFoundException(request.workflowId(), request.workflowVersion());
        }

        // 3. 验证类型是否合法
        validateAgentType(request.type());

        // 4. 创建智能体
        Agent agent = new Agent();
        agent.setWorkflowId(request.workflowId());
        agent.setWorkflowVersion(request.workflowVersion());
        agent.setName(request.name());
        agent.setType(request.type());
        agent.setStatus(Agent.STATUS_INACTIVE);  // 创建时默认停用
        agent.setApiKey(generateApiKey());       // 生成唯一API密钥

        save(agent);
        log.info("创建智能体成功: id={}, name={}, type={}", agent.getId(), agent.getName(), agent.getType());

        return agent;
    }

    /**
     * 发布智能体
     * <p>
     * 流程：
     * 1. 获取智能体
     * 2. 设置配置（API 类型需设置 Webhook）
     * 3. 激活智能体
     * </p>
     */
    @Override
    @Transactional
    public Agent publish(Long agentId, AgentPublishRequest request) {
        log.info("发布智能体: agentId={}, timeout={}s", agentId, request.timeoutSeconds());

        // 1. 获取智能体
        Agent agent = getById(agentId);
        if (agent == null) {
            throw new AgentNotFoundException(agentId);
        }

        // 2. 设置配置
        if (Agent.TYPE_API.equals(agent.getType())) {
            // API 类型需要设置 Webhook
            agent.setWebhookUrl(request.webhookUrl());
        }
        agent.setTimeoutSeconds(request.timeoutSeconds());

        // 3. 激活智能体
        agent.setStatus(Agent.STATUS_ACTIVE);

        updateById(agent);
        log.info("发布智能体成功: id={}, status={}", agentId, Agent.STATUS_ACTIVE);

        return agent;
    }

    /**
     * 停用智能体
     * <p>
     * 将状态改为 INACTIVE，停止对外服务
     * </p>
     */
    @Override
    @Transactional
    public void deactivate(Long agentId) {
        log.info("停用智能体: agentId={}", agentId);

        Agent agent = getById(agentId);
        if (agent == null) {
            throw new AgentNotFoundException(agentId);
        }

        agent.setStatus(Agent.STATUS_INACTIVE);
        updateById(agent);
        log.info("停用智能体成功: id={}", agentId);
    }

    /**
     * 激活智能体
     * <p>
     * 将状态改为 ACTIVE，恢复对外服务
     * </p>
     */
    @Override
    @Transactional
    public void activate(Long agentId) {
        log.info("激活智能体: agentId={}", agentId);

        Agent agent = getById(agentId);
        if (agent == null) {
            throw new AgentNotFoundException(agentId);
        }

        agent.setStatus(Agent.STATUS_ACTIVE);
        updateById(agent);
        log.info("激活智能体成功: id={}", agentId);
    }

    /**
     * 按 API Key 查询智能体
     * <p>
     * 使用 Mapper 中定义的方法查询
     * </p>
     */
    @Override
    public Agent findByApiKey(String apiKey) {
        return baseMapper.findByApiKey(apiKey);
    }

    /**
     * 按工作流查询智能体列表
     * <p>
     * 使用 Mapper 中定义的方法查询，按创建时间倒序
     * </p>
     */
    @Override
    public List<Agent> findByWorkflowId(Long workflowId) {
        return baseMapper.findByWorkflowId(workflowId);
    }

    /**
     * 按类型查询智能体列表
     * <p>
     * 使用 Mapper 中定义的方法查询，只返回 ACTIVE 状态的智能体
     * </p>
     */
    @Override
    public List<Agent> findByType(String type) {
        return baseMapper.findByType(type);
    }

    /**
     * 验证智能体类型是否合法
     *
     * @param type 智能体类型
     * @throws IllegalArgumentException 类型无效时抛出
     */
    private void validateAgentType(String type) {
        if (!Agent.TYPE_DIALOG.equals(type) && !Agent.TYPE_API.equals(type)) {
            throw new IllegalArgumentException("无效的智能体类型: " + type + "，仅支持 DIALOG 或 API");
        }
    }

    /**
     * 生成唯一 API Key
     * <p>
     * 使用 UUID 生成 32 位唯一标识符
     * </p>
     *
     * @return 32 位 API Key
     */
    private String generateApiKey() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}