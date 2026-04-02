package com.workflow.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.workflow.engine.dto.AgentCreateRequest;
import com.workflow.engine.dto.AgentPublishRequest;
import com.workflow.engine.model.Agent;

import java.util.List;

/**
 * 智能体服务接口
 * <p>
 * 提供智能体的创建、发布、激活/停用管理功能
 * 继承 MyBatis-Plus IService 接口，获得基础 CRUD 能力
 * </p>
 */
public interface AgentService extends IService<Agent> {

    /**
     * 创建智能体
     * <p>
     * 创建新的智能体，自动生成 API Key，初始状态为 INACTIVE
     * </p>
     *
     * @param request 创建请求
     * @return 新创建的智能体
     * @throws com.workflow.engine.exception.WorkflowNotFoundException        工作流不存在
     * @throws com.workflow.engine.exception.WorkflowVersionNotFoundException 工作流版本不存在
     * @throws IllegalArgumentException                                        智能体类型无效
     */
    Agent create(AgentCreateRequest request);

    /**
     * 发布智能体
     * <p>
     * 配置智能体参数并激活，API 类型需设置 Webhook URL
     * </p>
     *
     * @param agentId 智能体ID
     * @param request 发布请求
     * @return 发布后的智能体
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    Agent publish(Long agentId, AgentPublishRequest request);

    /**
     * 停用智能体
     * <p>
     * 将智能体状态改为 INACTIVE，停止对外服务
     * </p>
     *
     * @param agentId 智能体ID
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    void deactivate(Long agentId);

    /**
     * 激活智能体
     * <p>
     * 将智能体状态改为 ACTIVE，恢复对外服务
     * </p>
     *
     * @param agentId 智能体ID
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    void activate(Long agentId);

    /**
     * 按 API Key 查询智能体
     * <p>
     * 用于 API 智能体鉴权，根据请求中的 API Key 查找对应智能体
     * </p>
     *
     * @param apiKey API密钥
     * @return 智能体，如果不存在返回 null
     */
    Agent findByApiKey(String apiKey);

    /**
     * 按工作流查询智能体列表
     * <p>
     * 获取指定工作流关联的所有智能体
     * </p>
     *
     * @param workflowId 工作流ID
     * @return 智能体列表
     */
    List<Agent> findByWorkflowId(Long workflowId);

    /**
     * 按类型查询智能体列表
     * <p>
     * 筛选指定类型的智能体（DIALOG 或 API）
     * </p>
     *
     * @param type 类型（DIALOG 或 API）
     * @return 智能体列表
     */
    List<Agent> findByType(String type);
}