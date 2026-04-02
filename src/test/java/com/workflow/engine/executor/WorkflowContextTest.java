package com.workflow.engine.executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WorkflowContext 单元测试
 */
class WorkflowContextTest {

    @Test
    @DisplayName("创建 WorkflowContext")
    void builder_shouldCreateContext() {
        WorkflowContext context = WorkflowContext.builder()
                .workflowId(1L)
                .versionId(2L)
                .executionId(100L)
                .executionMode("ASYNC")
                .debugMode(true)
                .build();

        assertThat(context.getWorkflowId()).isEqualTo(1L);
        assertThat(context.getVersionId()).isEqualTo(2L);
        assertThat(context.getExecutionId()).isEqualTo(100L);
        assertThat(context.getExecutionMode()).isEqualTo("ASYNC");
        assertThat(context.isDebugMode()).isTrue();
    }

    @Test
    @DisplayName("getOrCreateThreadId 创建线程ID")
    void getOrCreateThreadId_shouldCreateThreadId() {
        WorkflowContext context = WorkflowContext.builder()
                .workflowId(1L)
                .build();

        String threadId = context.getOrCreateThreadId();

        assertThat(threadId).isNotNull();
        assertThat(threadId).startsWith("thread-1-");
    }

    @Test
    @DisplayName("getOrCreateThreadId 返回已设置的线程ID")
    void getOrCreateThreadId_shouldReturnExistingThreadId() {
        WorkflowContext context = WorkflowContext.builder()
                .threadId("existing-thread-id")
                .build();

        String threadId = context.getOrCreateThreadId();

        assertThat(threadId).isEqualTo("existing-thread-id");
    }

    @Test
    @DisplayName("计算执行时长")
    void calculateDuration_shouldCalculateCorrectly() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusSeconds(5);

        WorkflowContext context = WorkflowContext.builder()
                .startedAt(start)
                .completedAt(end)
                .build();

        Long duration = context.calculateDuration();

        assertThat(duration).isNotNull();
        assertThat(duration).isEqualTo(5000L);
    }

    @Test
    @DisplayName("计算执行时长 - 未结束返回 null")
    void calculateDuration_whenNotCompleted_shouldReturnNull() {
        WorkflowContext context = WorkflowContext.builder()
                .startedAt(LocalDateTime.now())
                .build();

        Long duration = context.calculateDuration();

        assertThat(duration).isNull();
    }

    @Test
    @DisplayName("默认值测试")
    void defaults_shouldBeSet() {
        WorkflowContext context = WorkflowContext.builder().build();

        assertThat(context.getExecutionMode()).isEqualTo("SYNC");
        assertThat(context.isDebugMode()).isFalse();
        assertThat(context.isCheckpointEnabled()).isTrue();
    }
}