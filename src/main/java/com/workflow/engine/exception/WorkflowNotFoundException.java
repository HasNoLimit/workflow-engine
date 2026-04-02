package com.workflow.engine.exception;

/**
 * Exception thrown when a workflow is not found.
 */
public class WorkflowNotFoundException extends RuntimeException {

    public WorkflowNotFoundException(Long id) {
        super("Workflow not found: " + id);
    }
}