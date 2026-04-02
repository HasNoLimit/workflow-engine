package com.workflow.engine.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new workflow.
 *
 * @param name        the name of the workflow (required)
 * @param description the description of the workflow (optional)
 */
public record WorkflowCreateRequest(
    @NotBlank(message = "Workflow name is required")
    String name,
    String description
) {}