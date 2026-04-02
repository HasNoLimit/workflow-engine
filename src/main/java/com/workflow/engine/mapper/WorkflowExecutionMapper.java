package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.WorkflowExecution;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WorkflowExecutionMapper extends BaseMapper<WorkflowExecution> {

    default List<WorkflowExecution> findByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapper<WorkflowExecution>()
            .eq(WorkflowExecution::getWorkflowId, workflowId)
            .orderByDesc(WorkflowExecution::getStartedAt));
    }

    default List<WorkflowExecution> findByAgentId(Long agentId) {
        return selectList(new LambdaQueryWrapper<WorkflowExecution>()
            .eq(WorkflowExecution::getAgentId, agentId)
            .orderByDesc(WorkflowExecution::getStartedAt));
    }
}