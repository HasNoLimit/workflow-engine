package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.NodeExecutionLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NodeExecutionLogMapper extends BaseMapper<NodeExecutionLog> {

    default List<NodeExecutionLog> findByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapper<NodeExecutionLog>()
            .eq(NodeExecutionLog::getExecutionId, executionId)
            .orderByAsc(NodeExecutionLog::getId));
    }
}