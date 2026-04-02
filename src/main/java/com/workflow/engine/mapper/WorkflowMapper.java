package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.Workflow;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface WorkflowMapper extends BaseMapper<Workflow> {

    default Optional<Workflow> findByName(String name) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<Workflow>()
            .eq(Workflow::getName, name)));
    }
}