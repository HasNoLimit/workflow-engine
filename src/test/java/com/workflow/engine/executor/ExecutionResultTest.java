package com.workflow.engine.executor;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExecutionResult 单元测试
 */
class ExecutionResultTest {

    @Test
    @DisplayName("创建成功的执行结果")
    void success_shouldCreateSuccessResult() {
        Map<String, Object> output = new HashMap<>();
        output.put("result", "success");
        var path = java.util.List.of("node-1", "node-2");

        ExecutionResult result = ExecutionResult.success(output, path);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_SUCCESS);
        assertThat(result.getOutput()).isEqualTo(output);
        assertThat(result.getExecutionPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("创建失败的执行结果")
    void failure_shouldCreateFailureResult() {
        var path = java.util.List.of("node-1");

        ExecutionResult result = ExecutionResult.failure("执行出错", path);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(WorkflowState.STATUS_FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("执行出错");
        assertThat(result.getExecutionPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("构建器创建完整执行结果")
    void builder_shouldCreateFullResult() {
        ExecutionResult result = ExecutionResult.builder()
                .success(true)
                .status(WorkflowState.STATUS_SUCCESS)
                .output(java.util.Map.of("key", "value"))
                .executionPath(java.util.List.of("start", "end"))
                .executionId(100L)
                .durationMs(500L)
                .build();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExecutionId()).isEqualTo(100L);
        assertThat(result.getDurationMs()).isEqualTo(500L);
    }
}