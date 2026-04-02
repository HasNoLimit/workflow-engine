package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.LlmProvider;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface LlmProviderMapper extends BaseMapper<LlmProvider> {

    default Optional<LlmProvider> findByName(String name) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<LlmProvider>()
            .eq(LlmProvider::getName, name)));
    }
}