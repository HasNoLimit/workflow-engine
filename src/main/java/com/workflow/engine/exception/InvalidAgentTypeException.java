package com.workflow.engine.exception;

/**
 * 智能体类型无效异常
 * <p>
 * 当智能体类型不符合操作要求时抛出，例如尝试用对话智能体执行 API 调用
 * </p>
 */
public class InvalidAgentTypeException extends RuntimeException {

    public InvalidAgentTypeException(String message) {
        super(message);
    }

    public InvalidAgentTypeException(Long agentId, String expectedType, String actualType) {
        super(String.format("智能体类型无效: agentId=%d, 期望类型=%s, 实际类型=%s",
                agentId, expectedType, actualType));
    }
}