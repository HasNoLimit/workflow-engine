package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.WorkflowCheckpoint;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WorkflowCheckpointMapper extends BaseMapper<WorkflowCheckpoint> {

    default WorkflowCheckpoint findLatestByThreadId(String threadId) {
        return selectOne(new LambdaQueryWrapper<WorkflowCheckpoint>()
            .eq(WorkflowCheckpoint::getThreadId, threadId)
            .orderByDesc(WorkflowCheckpoint::getCreatedAt)
            .last("LIMIT 1"));
    }

    default List<WorkflowCheckpoint> findByThreadId(String threadId) {
        return selectList(new LambdaQueryWrapper<WorkflowCheckpoint>()
            .eq(WorkflowCheckpoint::getThreadId, threadId)
            .orderByDesc(WorkflowCheckpoint::getCreatedAt));
    }

    default List<WorkflowCheckpoint> findByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapper<WorkflowCheckpoint>()
            .eq(WorkflowCheckpoint::getExecutionId, executionId));
    }
}