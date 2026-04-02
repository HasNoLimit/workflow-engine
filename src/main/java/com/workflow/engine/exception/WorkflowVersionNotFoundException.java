package com.workflow.engine.exception;

/**
 * 工作流版本未找到异常
 * <p>
 * 当请求的工作流版本不存在时抛出
 * </p>
 */
public class WorkflowVersionNotFoundException extends RuntimeException {

    /**
     * 构造函数
     *
     * @param workflowId    工作流ID
     * @param versionNumber 版本号
     */
    public WorkflowVersionNotFoundException(Long workflowId, Integer versionNumber) {
        super("工作流版本未找到: workflowId=" + workflowId + ", version=" + versionNumber);
    }
}