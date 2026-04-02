package com.workflow.engine.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating an existing workflow.
 *
 * @param name        the new name of the workflow (required)
 * @param description the new description of the workflow (optional)
 */
public record WorkflowUpdateRequest(
    @NotBlank(message = "Workflow name is required")
    String name,
    String description
) {}