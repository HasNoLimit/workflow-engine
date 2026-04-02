package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.Agent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentMapper extends BaseMapper<Agent> {

    default Agent findByApiKey(String apiKey) {
        return selectOne(new LambdaQueryWrapper<Agent>()
            .eq(Agent::getApiKey, apiKey));
    }

    default List<Agent> findByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapper<Agent>()
            .eq(Agent::getWorkflowId, workflowId)
            .orderByDesc(Agent::getCreatedAt));
    }

    default List<Agent> findByType(String type) {
        return selectList(new LambdaQueryWrapper<Agent>()
            .eq(Agent::getType, type)
            .eq(Agent::getStatus, Agent.STATUS_ACTIVE)
            .orderByDesc(Agent::getCreatedAt));
    }
}