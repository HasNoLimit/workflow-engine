package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.DebugExecution;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DebugExecutionMapper extends BaseMapper<DebugExecution> {

    default List<DebugExecution> findByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapper<DebugExecution>()
            .eq(DebugExecution::getWorkflowId, workflowId));
    }
}