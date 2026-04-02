package com.workflow.engine.controller;

import com.workflow.engine.dto.VersionSaveRequest;
import com.workflow.engine.model.WorkflowVersion;
import com.workflow.engine.service.WorkflowVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流版本管理控制器
 * <p>
 * 提供工作流版本的 REST API，包括保存版本、查询版本历史等
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/{workflowId}/versions")
@RequiredArgsConstructor
public class WorkflowVersionController {

    private final WorkflowVersionService workflowVersionService;

    /**
     * 保存新版本
     * <p>
     * 自动递增版本号，保存画布数据
     * </p>
     *
     * @param workflowId 工作流ID
     * @param request     保存请求
     * @return 新创建的版本（HTTP 201）
     */
    @PostMapping
    public ResponseEntity<WorkflowVersion> saveVersion(
            @PathVariable Long workflowId,
            @Valid @RequestBody VersionSaveRequest request) {
        log.info("接收保存版本请求: workflowId={}", workflowId);
        WorkflowVersion version = workflowVersionService.saveVersion(workflowId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }

    /**
     * 获取版本历史列表
     * <p>
     * 返回指定工作流的所有版本，按版本号倒序排列
     * </p>
     *
     * @param workflowId 工作流ID
     * @return 版本列表
     */
    @GetMapping
    public ResponseEntity<List<WorkflowVersion>> listVersions(@PathVariable Long workflowId) {
        log.info("接收获取版本列表请求: workflowId={}", workflowId);
        List<WorkflowVersion> versions = workflowVersionService.listVersions(workflowId);
        return ResponseEntity.ok(versions);
    }

    /**
     * 获取特定版本
     *
     * @param workflowId    工作流ID
     * @param versionNumber 版本号
     * @return 版本详情
     */
    @GetMapping("/{versionNumber}")
    public ResponseEntity<WorkflowVersion> getVersion(
            @PathVariable Long workflowId,
            @PathVariable Integer versionNumber) {
        log.info("接收获取特定版本请求: workflowId={}, version={}", workflowId, versionNumber);
        WorkflowVersion version = workflowVersionService.getVersion(workflowId, versionNumber);
        return ResponseEntity.ok(version);
    }

    /**
     * 获取最新版本
     *
     * @param workflowId 工作流ID
     * @return 最新版本，如果没有版本则返回 null
     */
    @GetMapping("/latest")
    public ResponseEntity<WorkflowVersion> getLatestVersion(@PathVariable Long workflowId) {
        log.info("接收获取最新版本请求: workflowId={}", workflowId);
        WorkflowVersion version = workflowVersionService.getLatestVersion(workflowId);
        return ResponseEntity.ok(version);
    }
}