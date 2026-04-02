package com.workflow.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.dto.WorkflowUpdateRequest;
import com.workflow.engine.model.Workflow;

import java.util.List;

/**
 * Service interface for workflow management operations.
 * Extends MyBatis-Plus IService for basic CRUD operations.
 */
public interface WorkflowService extends IService<Workflow> {

    /**
     * Creates a new workflow.
     *
     * @param request the workflow creation request
     * @return the created workflow
     */
    Workflow create(WorkflowCreateRequest request);

    /**
     * Updates an existing workflow.
     *
     * @param id      the workflow ID
     * @param request the workflow update request
     * @return the updated workflow
     * @throws com.workflow.engine.exception.WorkflowNotFoundException if workflow not found
     */
    Workflow update(Long id, WorkflowUpdateRequest request);

    /**
     * Archives a workflow (soft delete).
     *
     * @param id the workflow ID
     * @throws com.workflow.engine.exception.WorkflowNotFoundException if workflow not found
     */
    void delete(Long id);

    /**
     * Gets a workflow by ID.
     *
     * @param id the workflow ID
     * @return the workflow, or null if not found
     */
    Workflow getById(Long id);

    /**
     * Lists all workflows.
     *
     * @return list of all workflows
     */
    List<Workflow> listAll();
}