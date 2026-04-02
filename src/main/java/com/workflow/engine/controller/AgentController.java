package com.workflow.engine.controller;

import com.workflow.engine.dto.AgentCreateRequest;
import com.workflow.engine.dto.AgentPublishRequest;
import com.workflow.engine.model.Agent;
import com.workflow.engine.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能体管理控制器
 * <p>
 * 提供智能体的创建、发布、激活/停用 REST API
 * </p>
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 创建智能体
     * <p>
     * 创建新的智能体，支持 DIALOG 和 API 两种类型
     * </p>
     *
     * @param request 创建请求
     * @return 创建的智能体（201 Created）
     */
    @PostMapping
    public ResponseEntity<Agent> create(@Valid @RequestBody AgentCreateRequest request) {
        Agent agent = agentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(agent);
    }

    /**
     * 发布智能体
     * <p>
     * 配置智能体参数并激活，使其可以对外提供服务
     * </p>
     *
     * @param agentId 智能体ID
     * @param request 发布请求
     * @return 发布后的智能体
     */
    @PostMapping("/{agentId}/publish")
    public ResponseEntity<Agent> publish(
        @PathVariable Long agentId,
        @Valid @RequestBody AgentPublishRequest request) {
        Agent agent = agentService.publish(agentId, request);
        return ResponseEntity.ok(agent);
    }

    /**
     * 获取智能体详情
     *
     * @param agentId 智能体ID
     * @return 智能体详情
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<Agent> getById(@PathVariable Long agentId) {
        Agent agent = agentService.getAgentById(agentId);
        return ResponseEntity.ok(agent);
    }

    /**
     * 获取智能体列表
     * <p>
     * 返回所有智能体列表
     * </p>
     *
     * @return 智能体列表
     */
    @GetMapping
    public ResponseEntity<List<Agent>> list() {
        return ResponseEntity.ok(agentService.list());
    }

    /**
     * 按工作流查询智能体
     * <p>
     * 获取指定工作流关联的所有智能体
     * </p>
     *
     * @param workflowId 工作流ID
     * @return 智能体列表
     */
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<List<Agent>> findByWorkflowId(@PathVariable Long workflowId) {
        return ResponseEntity.ok(agentService.findByWorkflowId(workflowId));
    }

    /**
     * 按类型查询智能体
     * <p>
     * 筛选指定类型的智能体（DIALOG 或 API）
     * </p>
     *
     * @param type 类型（DIALOG 或 API）
     * @return 智能体列表
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Agent>> findByType(@PathVariable String type) {
        return ResponseEntity.ok(agentService.findByType(type));
    }

    /**
     * 停用智能体
     * <p>
     * 将智能体状态改为 INACTIVE，停止对外服务
     * </p>
     *
     * @param agentId 智能体ID
     * @return 200 OK
     */
    @PostMapping("/{agentId}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long agentId) {
        agentService.deactivate(agentId);
        return ResponseEntity.ok().build();
    }

    /**
     * 激活智能体
     * <p>
     * 将智能体状态改为 ACTIVE，恢复对外服务
     * </p>
     *
     * @param agentId 智能体ID
     * @return 200 OK
     */
    @PostMapping("/{agentId}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long agentId) {
        agentService.activate(agentId);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除智能体
     * <p>
     * 仅支持删除 INACTIVE 状态的智能体，活跃状态的智能体需先停用
     * </p>
     *
     * @param agentId 智能体ID
     * @return 204 No Content
     * @throws com.workflow.engine.exception.AgentNotFoundException 智能体不存在
     * @throws IllegalStateException 智能体处于活跃状态
     */
    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> delete(@PathVariable Long agentId) {
        agentService.delete(agentId);
        return ResponseEntity.noContent().build();
    }
}