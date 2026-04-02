package com.workflow.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workflow.engine.exception.WorkflowExecutionException;
import com.workflow.engine.mapper.WorkflowExecutionMapper;
import com.workflow.engine.model.WorkflowExecution;
import com.workflow.engine.service.WorkflowExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流执行服务实现
 * <p>
 * 继承 MyBatis-Plus ServiceImpl，提供执行记录的持久化操作
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionServiceImpl extends ServiceImpl<WorkflowExecutionMapper, WorkflowExecution>
        implements WorkflowExecutionService {

    @Override
    @Transactional
    public WorkflowExecution createExecution(Long workflowId, Long agentId, Map<String, Object> input) {
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setAgentId(agentId);
        execution.setStatus(WorkflowExecution.STATUS_RUNNING);
        execution.setInput(input);
        execution.setStartedAt(LocalDateTime.now());
        save(execution);

        log.info("创建执行记录: executionId={}, workflowId={}, agentId={}",
                execution.getId(), workflowId, agentId);

        return execution;
    }

    @Override
    @Transactional
    public void markSuccess(Long executionId, Map<String, Object> output, Long durationMs) {
        WorkflowExecution execution = getById(executionId);
        if (execution == null) {
            throw new WorkflowExecutionException("执行记录不存在: " + executionId);
        }

        execution.setStatus(WorkflowExecution.STATUS_SUCCESS);
        execution.setOutput(output);
        execution.setDurationMs(durationMs);
        execution.setCompletedAt(LocalDateTime.now());
        updateById(execution);

        log.info("执行成功: executionId={}, durationMs={}", executionId, durationMs);
    }

    @Override
    @Transactional
    public void markFailed(Long executionId, String errorMessage, Long durationMs) {
        WorkflowExecution execution = getById(executionId);
        if (execution == null) {
            throw new WorkflowExecutionException("执行记录不存在: " + executionId);
        }

        execution.setStatus(WorkflowExecution.STATUS_FAILED);
        execution.setDurationMs(durationMs);
        execution.setCompletedAt(LocalDateTime.now());
        // 将错误信息存储到 output 中
        execution.setOutput(Map.of("error", errorMessage));
        updateById(execution);

        log.error("执行失败: executionId={}, error={}", executionId, errorMessage);
    }

    @Override
    public WorkflowExecution getExecutionById(Long executionId) {
        WorkflowExecution execution = getById(executionId);
        if (execution == null) {
            throw new WorkflowExecutionException("执行记录不存在: " + executionId);
        }
        return execution;
    }
}