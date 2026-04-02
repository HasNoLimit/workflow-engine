package com.workflow.engine.controller;

import com.workflow.engine.dto.AgentEndpoint;
import com.workflow.engine.dto.PublishResult;
import com.workflow.engine.publish.PublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 智能体发布管理控制器
 * <p>
 * 提供智能体发布、取消发布、状态查询和端点信息获取的 REST API。
 * 与 AgentController 中的 publish 接口区分：
 * - AgentController.publish: 配置参数并激活
 * - PublishController: 管理发布状态和获取端点信息
 * </p>
 */
@RestController
@RequestMapping("/api/agents/{agentId}/publication")
@RequiredArgsConstructor
public class PublishController {

    private final PublisherService publisherService;

    /**
     * 发布智能体
     * <p>
     * 执行发布流程，激活智能体并返回端点信息
     * </p>
     *
     * @param agentId 智能体ID
     * @return 发布结果
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    @PostMapping
    public ResponseEntity<PublishResult> publish(@PathVariable Long agentId) {
        PublishResult result = publisherService.publish(agentId);
        return ResponseEntity.ok(result);
    }

    /**
     * 取消发布智能体
     * <p>
     * 停用智能体，停止对外服务
     * </p>
     *
     * @param agentId 智能体ID
     * @return 204 No Content
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    @DeleteMapping
    public ResponseEntity<Void> unpublish(@PathVariable Long agentId) {
        publisherService.unpublish(agentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取发布状态
     * <p>
     * 返回智能体是否处于已发布（ACTIVE）状态
     * </p>
     *
     * @param agentId 智能体ID
     * @return 发布状态
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    @GetMapping("/status")
    public ResponseEntity<PublishStatus> getPublishStatus(@PathVariable Long agentId) {
        boolean published = publisherService.isPublished(agentId);
        return ResponseEntity.ok(new PublishStatus(published));
    }

    /**
     * 获取端点信息
     * <p>
     * 返回智能体的访问端点、API密钥、Webhook URL等信息
     * </p>
     *
     * @param agentId 智能体ID
     * @return 端点信息
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    @GetMapping("/endpoint")
    public ResponseEntity<AgentEndpoint> getEndpoint(@PathVariable Long agentId) {
        AgentEndpoint endpoint = publisherService.getEndpoint(agentId);
        return ResponseEntity.ok(endpoint);
    }

    /**
     * 发布状态 DTO
     * <p>
     * 封装智能体的发布状态信息
     * </p>
     *
     * @param published 是否已发布
     */
    public record PublishStatus(boolean published) {}
}