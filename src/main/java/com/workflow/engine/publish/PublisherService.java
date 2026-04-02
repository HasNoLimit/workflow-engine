package com.workflow.engine.publish;

import com.workflow.engine.dto.AgentEndpoint;
import com.workflow.engine.dto.PublishResult;

/**
 * 智能体发布服务接口
 * <p>
 * 提供智能体的发布、取消发布、状态查询和端点信息获取功能
 * 支持对话智能体（DIALOG）和 API 智能体（API）两种发布模式
 * </p>
 */
public interface PublisherService {

    /**
     * 发布智能体
     * <p>
     * 根据智能体类型选择对应的发布器进行发布：
     * - DIALOG 类型：生成 WebSocket 端点
     * - API 类型：生成 REST API 端点
     * </p>
     *
     * @param agentId 智能体ID
     * @return 发布结果，包含端点地址、API密钥等信息
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    PublishResult publish(Long agentId);

    /**
     * 取消发布智能体
     * <p>
     * 将智能体状态改为 INACTIVE，停止对外服务
     * </p>
     *
     * @param agentId 智能体ID
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    void unpublish(Long agentId);

    /**
     * 检查智能体是否已发布
     * <p>
     * 通过检查智能体状态是否为 ACTIVE 来判断发布状态
     * </p>
     *
     * @param agentId 智能体ID
     * @return 是否已发布（ACTIVE 状态为已发布）
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    boolean isPublished(Long agentId);

    /**
     * 获取智能体端点信息
     * <p>
     * 返回已发布智能体的访问端点、API密钥、Webhook URL等信息
     * </p>
     *
     * @param agentId 智能体ID
     * @return 端点信息对象
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     * @throws IllegalStateException                                     智能体类型未知
     */
    AgentEndpoint getEndpoint(Long agentId);
}