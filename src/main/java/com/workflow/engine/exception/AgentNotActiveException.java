package com.workflow.engine.exception;

/**
 * 智能体未激活异常
 * <p>
 * 当尝试执行未激活（INACTIVE）状态的智能体时抛出
 * </p>
 */
public class AgentNotActiveException extends RuntimeException {

    public AgentNotActiveException(String message) {
        super(message);
    }

    public AgentNotActiveException(Long agentId, String status) {
        super(String.format("智能体未激活: agentId=%d, 当前状态=%s", agentId, status));
    }
}