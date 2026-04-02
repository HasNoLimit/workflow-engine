package com.workflow.engine.exception;

/**
 * 智能体未找到异常
 * <p>
 * 当请求的智能体不存在时抛出
 * </p>
 */
public class AgentNotFoundException extends RuntimeException {

    /**
     * 构造函数
     *
     * @param agentId 智能体ID
     */
    public AgentNotFoundException(Long agentId) {
        super("智能体未找到: id=" + agentId);
    }
}