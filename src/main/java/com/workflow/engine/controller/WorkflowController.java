package com.workflow.engine.controller;

import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.dto.WorkflowUpdateRequest;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for workflow management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * Creates a new workflow.
     *
     * @param request the workflow creation request
     * @return the created workflow with 201 status
     */
    @PostMapping
    public ResponseEntity<Workflow> create(@Valid @RequestBody WorkflowCreateRequest request) {
        log.info("Received request to create workflow: {}", request.name());
        Workflow workflow = workflowService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflow);
    }

    /**
     * Gets a workflow by ID.
     *
     * @param id the workflow ID
     * @return the workflow
     */
    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getById(@PathVariable Long id) {
        log.info("Received request to get workflow by id: {}", id);
        Workflow workflow = workflowService.getById(id);
        return ResponseEntity.ok(workflow);
    }

    /**
     * Lists all workflows.
     *
     * @return list of all workflows
     */
    @GetMapping
    public ResponseEntity<List<Workflow>> list() {
        log.info("Received request to list all workflows");
        List<Workflow> workflows = workflowService.listAll();
        return ResponseEntity.ok(workflows);
    }

    /**
     * Updates an existing workflow.
     *
     * @param id      the workflow ID
     * @param request the workflow update request
     * @return the updated workflow
     */
    @PutMapping("/{id}")
    public ResponseEntity<Workflow> update(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowUpdateRequest request) {
        log.info("Received request to update workflow id: {}", id);
        Workflow workflow = workflowService.update(id, request);
        return ResponseEntity.ok(workflow);
    }

    /**
     * Archives a workflow (soft delete).
     *
     * @param id the workflow ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Received request to delete workflow id: {}", id);
        workflowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}