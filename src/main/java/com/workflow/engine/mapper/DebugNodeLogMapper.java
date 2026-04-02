package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.DebugNodeLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DebugNodeLogMapper extends BaseMapper<DebugNodeLog> {

    default List<DebugNodeLog> findByDebugExecutionId(Long debugExecutionId) {
        return selectList(new LambdaQueryWrapper<DebugNodeLog>()
            .eq(DebugNodeLog::getDebugExecutionId, debugExecutionId)
            .orderByAsc(DebugNodeLog::getStepIndex));
    }
}