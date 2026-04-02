package com.workflow.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.workflow.engine.model.WorkflowExecution;

/**
 * 工作流执行服务接口
 * <p>
 * 提供工作流执行记录的管理功能，包括创建、查询、更新执行状态等
 * </p>
 */
public interface WorkflowExecutionService extends IService<WorkflowExecution> {

    /**
     * 创建执行记录
     * <p>
     * 创建一个新的工作流执行记录，状态初始化为 RUNNING
     * </p>
     *
     * @param workflowId 工作流ID
     * @param agentId    智能体ID
     * @param input      输入参数
     * @return 创建的执行记录
     */
    WorkflowExecution createExecution(Long workflowId, Long agentId, java.util.Map<String, Object> input);

    /**
     * 标记执行成功
     *
     * @param executionId 执行ID
     * @param output      输出结果
     * @param durationMs  执行耗时（毫秒）
     */
    void markSuccess(Long executionId, java.util.Map<String, Object> output, Long durationMs);

    /**
     * 标记执行失败
     *
     * @param executionId 执行ID
     * @param errorMessage 错误信息
     * @param durationMs   执行耗时（毫秒）
     */
    void markFailed(Long executionId, String errorMessage, Long durationMs);

    /**
     * 获取执行记录（不存在时抛出异常）
     *
     * @param executionId 执行ID
     * @return 执行记录
     * @throws com.workflow.engine.exception.WorkflowExecutionException 执行记录不存在
     */
    WorkflowExecution getExecutionById(Long executionId);
}