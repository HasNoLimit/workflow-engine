package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.LlmModel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LlmModelMapper extends BaseMapper<LlmModel> {

    default LlmModel findByModelId(String modelId) {
        return selectOne(new LambdaQueryWrapper<LlmModel>()
            .eq(LlmModel::getModelId, modelId));
    }

    default List<LlmModel> findByProviderId(Long providerId) {
        return selectList(new LambdaQueryWrapper<LlmModel>()
            .eq(LlmModel::getProviderId, providerId));
    }
}