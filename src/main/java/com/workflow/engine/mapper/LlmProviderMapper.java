package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.LlmProvider;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmProviderMapper extends BaseMapper<LlmProvider> {
}