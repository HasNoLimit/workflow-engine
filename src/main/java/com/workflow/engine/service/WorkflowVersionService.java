package com.workflow.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.workflow.engine.dto.VersionSaveRequest;
import com.workflow.engine.model.WorkflowVersion;

import java.util.List;

/**
 * 工作流版本服务接口
 * <p>
 * 提供工作流版本管理功能，包括保存版本、查询版本历史等
 * </p>
 */
public interface WorkflowVersionService extends IService<WorkflowVersion> {

    /**
     * 保存新版本
     * <p>
     * 自动递增版本号，保存画布数据
     * </p>
     *
     * @param workflowId 工作流ID
     * @param request    保存请求
     * @return 新创建的版本
     * @throws com.workflow.engine.exception.WorkflowNotFoundException 如果工作流不存在
     */
    WorkflowVersion saveVersion(Long workflowId, VersionSaveRequest request);

    /**
     * 获取版本历史列表
     *
     * @param workflowId 工作流ID
     * @return 版本列表（按版本号倒序）
     * @throws com.workflow.engine.exception.WorkflowNotFoundException 如果工作流不存在
     */
    List<WorkflowVersion> listVersions(Long workflowId);

    /**
     * 获取特定版本
     *
     * @param workflowId    工作流ID
     * @param versionNumber 版本号
     * @return 版本详情
     * @throws com.workflow.engine.exception.WorkflowNotFoundException        如果工作流不存在
     * @throws com.workflow.engine.exception.WorkflowVersionNotFoundException 如果版本不存在
     */
    WorkflowVersion getVersion(Long workflowId, Integer versionNumber);

    /**
     * 获取最新版本
     *
     * @param workflowId 工作流ID
     * @return 最新版本，如果没有任何版本则返回 null
     * @throws com.workflow.engine.exception.WorkflowNotFoundException 如果工作流不存在
     */
    WorkflowVersion getLatestVersion(Long workflowId);
}