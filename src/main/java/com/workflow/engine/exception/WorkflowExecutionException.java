package com.workflow.engine.exception;

/**
 * 工作流执行异常
 * <p>
 * 当工作流执行过程中出现错误时抛出此异常
 * </p>
 */
public class WorkflowExecutionException extends RuntimeException {

    /**
     * 构造工作流执行异常
     * @param message 异常信息
     */
    public WorkflowExecutionException(String message) {
        super(message);
    }

    /**
     * 构造工作流执行异常（带原因）
     * @param message 异常信息
     * @param cause 原始异常
     */
    public WorkflowExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}