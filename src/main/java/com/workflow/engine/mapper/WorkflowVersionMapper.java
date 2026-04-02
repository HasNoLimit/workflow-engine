package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.WorkflowVersion;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WorkflowVersionMapper extends BaseMapper<WorkflowVersion> {

    default WorkflowVersion findByWorkflowIdAndVersion(Long workflowId, Integer version) {
        return selectOne(new LambdaQueryWrapper<WorkflowVersion>()
            .eq(WorkflowVersion::getWorkflowId, workflowId)
            .eq(WorkflowVersion::getVersion, version));
    }

    default List<WorkflowVersion> findByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapper<WorkflowVersion>()
            .eq(WorkflowVersion::getWorkflowId, workflowId)
            .orderByDesc(WorkflowVersion::getVersion));
    }
}