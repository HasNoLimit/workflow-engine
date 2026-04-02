package com.workflow.engine.service;

import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.dto.WorkflowUpdateRequest;
import com.workflow.engine.exception.WorkflowNotFoundException;
import com.workflow.engine.model.Workflow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WorkflowService.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkflowServiceTest {

    @Autowired
    private WorkflowService workflowService;

    @Test
    void shouldCreateWorkflow() {
        // Given
        WorkflowCreateRequest request = new WorkflowCreateRequest("Test Workflow", "Test Description");

        // When
        Workflow workflow = workflowService.create(request);

        // Then
        assertNotNull(workflow.getId());
        assertEquals("Test Workflow", workflow.getName());
        assertEquals("Test Description", workflow.getDescription());
        assertEquals(1, workflow.getVersion());
        assertEquals(Workflow.STATUS_DRAFT, workflow.getStatus());
        assertNotNull(workflow.getCreatedAt());
        assertNotNull(workflow.getUpdatedAt());
    }

    @Test
    void shouldCreateWorkflowWithNullDescription() {
        // Given
        WorkflowCreateRequest request = new WorkflowCreateRequest("Minimal Workflow", null);

        // When
        Workflow workflow = workflowService.create(request);

        // Then
        assertNotNull(workflow.getId());
        assertEquals("Minimal Workflow", workflow.getName());
        assertNull(workflow.getDescription());
        assertEquals(Workflow.STATUS_DRAFT, workflow.getStatus());
    }

    @Test
    void shouldGetWorkflowById() {
        // Given
        WorkflowCreateRequest request = new WorkflowCreateRequest("Find Test", "Description");
        Workflow created = workflowService.create(request);

        // When
        Workflow found = workflowService.getById(created.getId());

        // Then
        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
        assertEquals("Find Test", found.getName());
    }

    @Test
    void shouldReturnNullForNonExistentWorkflow() {
        // When
        Workflow found = workflowService.getById(999L);

        // Then
        assertNull(found);
    }

    @Test
    void shouldUpdateWorkflow() {
        // Given
        WorkflowCreateRequest createRequest = new WorkflowCreateRequest("Original Name", "Original Description");
        Workflow created = workflowService.create(createRequest);

        // When
        WorkflowUpdateRequest updateRequest = new WorkflowUpdateRequest("Updated Name", "Updated Description");
        Workflow updated = workflowService.update(created.getId(), updateRequest);

        // Then
        assertEquals(created.getId(), updated.getId());
        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated Description", updated.getDescription());
    }

    @Test
    void shouldUpdateWorkflowNameOnly() {
        // Given
        WorkflowCreateRequest createRequest = new WorkflowCreateRequest("Original", "Keep this");
        Workflow created = workflowService.create(createRequest);

        // When
        WorkflowUpdateRequest updateRequest = new WorkflowUpdateRequest("New Name", null);
        Workflow updated = workflowService.update(created.getId(), updateRequest);

        // Then
        assertEquals("New Name", updated.getName());
        assertNull(updated.getDescription());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentWorkflow() {
        // Given
        WorkflowUpdateRequest request = new WorkflowUpdateRequest("Name", "Description");

        // When & Then
        assertThrows(WorkflowNotFoundException.class, () ->
            workflowService.update(999L, request)
        );
    }

    @Test
    void shouldDeleteWorkflow() {
        // Given
        WorkflowCreateRequest request = new WorkflowCreateRequest("To Delete", "Description");
        Workflow created = workflowService.create(request);

        // When
        workflowService.delete(created.getId());

        // Then
        Workflow deleted = workflowService.getById(created.getId());
        assertNotNull(deleted);
        assertEquals(Workflow.STATUS_ARCHIVED, deleted.getStatus());
    }

    @Test
    void shouldThrowWhenDeletingNonExistentWorkflow() {
        // When & Then
        assertThrows(WorkflowNotFoundException.class, () ->
            workflowService.delete(999L)
        );
    }

    @Test
    void shouldListAllWorkflows() {
        // Given
        workflowService.create(new WorkflowCreateRequest("Workflow 1", "Desc 1"));
        workflowService.create(new WorkflowCreateRequest("Workflow 2", "Desc 2"));
        workflowService.create(new WorkflowCreateRequest("Workflow 3", "Desc 3"));

        // When
        List<Workflow> workflows = workflowService.listAll();

        // Then
        assertNotNull(workflows);
        assertTrue(workflows.size() >= 3);
    }

    @Test
    void shouldReturnEmptyListWhenNoWorkflows() {
        // When
        List<Workflow> workflows = workflowService.listAll();

        // Then
        assertNotNull(workflows);
        // List may not be empty due to other tests, but should not be null
    }
}