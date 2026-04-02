package com.workflow.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workflow.engine.dto.VersionSaveRequest;
import com.workflow.engine.exception.WorkflowNotFoundException;
import com.workflow.engine.exception.WorkflowVersionNotFoundException;
import com.workflow.engine.mapper.WorkflowVersionMapper;
import com.workflow.engine.model.WorkflowVersion;
import com.workflow.engine.service.WorkflowService;
import com.workflow.engine.service.WorkflowVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 工作流版本服务实现
 * <p>
 * 实现工作流版本管理功能，继承 MyBatis-Plus ServiceImpl
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowVersionServiceImpl extends ServiceImpl<WorkflowVersionMapper, WorkflowVersion> implements WorkflowVersionService {

    private final WorkflowService workflowService;

    /**
     * 保存新版本
     * <p>
     * 自动递增版本号，保存画布数据
     * </p>
     *
     * @param workflowId 工作流ID
     * @param request    保存请求
     * @return 新创建的版本
     * @throws WorkflowNotFoundException 如果工作流不存在
     */
    @Override
    @Transactional
    public WorkflowVersion saveVersion(Long workflowId, VersionSaveRequest request) {
        log.info("保存工作流版本: workflowId={}", workflowId);

        // 1. 验证工作流是否存在
        validateWorkflowExists(workflowId);

        // 2. 获取最新版本号，自动递增
        WorkflowVersion latestVersion = baseMapper.findLatestByWorkflowId(workflowId).orElse(null);
        int newVersionNumber = latestVersion == null ? 1 : latestVersion.getVersion() + 1;

        // 3. 创建新版本记录
        WorkflowVersion version = new WorkflowVersion();
        version.setWorkflowId(workflowId);
        version.setVersion(newVersionNumber);
        version.setCanvas(request.canvas());
        version.setChangeNote(request.changeNote());

        // 4. 保存版本
        save(version);
        log.info("保存工作流版本成功: workflowId={}, version={}", workflowId, newVersionNumber);

        return version;
    }

    /**
     * 获取版本历史列表
     *
     * @param workflowId 工作流ID
     * @return 版本列表（按版本号倒序）
     * @throws WorkflowNotFoundException 如果工作流不存在
     */
    @Override
    public List<WorkflowVersion> listVersions(Long workflowId) {
        log.info("获取工作流版本列表: workflowId={}", workflowId);

        // 验证工作流是否存在
        validateWorkflowExists(workflowId);

        // 查询版本列表（已按版本号倒序）
        return baseMapper.findByWorkflowId(workflowId);
    }

    /**
     * 获取特定版本
     *
     * @param workflowId    工作流ID
     * @param versionNumber 版本号
     * @return 版本详情
     * @throws WorkflowNotFoundException        如果工作流不存在
     * @throws WorkflowVersionNotFoundException 如果版本不存在
     */
    @Override
    public WorkflowVersion getVersion(Long workflowId, Integer versionNumber) {
        log.info("获取工作流特定版本: workflowId={}, version={}", workflowId, versionNumber);

        // 验证工作流是否存在
        validateWorkflowExists(workflowId);

        // 查询特定版本
        WorkflowVersion version = baseMapper.findByWorkflowIdAndVersion(workflowId, versionNumber);
        if (version == null) {
            throw new WorkflowVersionNotFoundException(workflowId, versionNumber);
        }

        return version;
    }

    /**
     * 获取最新版本
     *
     * @param workflowId 工作流ID
     * @return 最新版本，如果没有任何版本则返回 null
     * @throws WorkflowNotFoundException 如果工作流不存在
     */
    @Override
    public WorkflowVersion getLatestVersion(Long workflowId) {
        log.info("获取工作流最新版本: workflowId={}", workflowId);

        // 验证工作流是否存在
        validateWorkflowExists(workflowId);

        // 查询最新版本
        return baseMapper.findLatestByWorkflowId(workflowId).orElse(null);
    }

    /**
     * 验证工作流是否存在
     *
     * @param workflowId 工作流ID
     * @throws WorkflowNotFoundException 如果工作流不存在
     */
    private void validateWorkflowExists(Long workflowId) {
        if (workflowService.getById(workflowId) == null) {
            throw new WorkflowNotFoundException(workflowId);
        }
    }
}