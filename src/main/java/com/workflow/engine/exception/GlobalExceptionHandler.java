package com.workflow.engine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API endpoints.
 * Provides consistent error response format across all controllers.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles WorkflowNotFoundException and returns 404 response.
     *
     * @param e the WorkflowNotFoundException
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowNotFound(WorkflowNotFoundException e) {
        log.warn("Workflow not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "WORKFLOW_NOT_FOUND",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * 处理 WorkflowVersionNotFoundException 异常，返回 404 响应
     *
     * @param e WorkflowVersionNotFoundException 异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(WorkflowVersionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowVersionNotFound(WorkflowVersionNotFoundException e) {
        log.warn("工作流版本未找到: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "WORKFLOW_VERSION_NOT_FOUND",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * 处理 AgentNotFoundException 异常，返回 404 响应
     *
     * @param e AgentNotFoundException 异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(AgentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAgentNotFound(AgentNotFoundException e) {
        log.warn("智能体未找到: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "AGENT_NOT_FOUND",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * 处理 WorkflowExecutionException 异常，返回 500 响应
     * <p>
     * 用于工作流执行过程中的错误，如节点配置无效、执行超时等
     * </p>
     *
     * @param e WorkflowExecutionException 异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(WorkflowExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowExecution(WorkflowExecutionException e) {
        log.error("工作流执行错误: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "WORKFLOW_EXECUTION_ERROR",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * 处理 IllegalStateException 异常，返回 400 响应
     * <p>
     * 用于业务规则验证失败的情况，如尝试删除活跃状态的智能体
     * </p>
     *
     * @param e IllegalStateException 异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        log.warn("业务规则验证失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "INVALID_OPERATION",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * 处理 InvalidAgentTypeException 异常，返回 400 响应
     * <p>
     * 当智能体类型不符合操作要求时抛出
     * </p>
     *
     * @param e InvalidAgentTypeException 异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(InvalidAgentTypeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAgentType(InvalidAgentTypeException e) {
        log.warn("智能体类型无效: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "INVALID_AGENT_TYPE",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * 处理 AgentNotActiveException 异常，返回 400 响应
     * <p>
     * 当尝试执行未激活状态的智能体时抛出
     * </p>
     *
     * @param e AgentNotActiveException 异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(AgentNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleAgentNotActive(AgentNotActiveException e) {
        log.warn("智能体未激活: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "AGENT_NOT_ACTIVE",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * 处理 InvalidApiKeyException 异常，返回 401 响应
     * <p>
     * 当调用 API 智能体时提供的 API Key 无效时抛出
     * </p>
     *
     * @param e InvalidApiKeyException 异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(InvalidApiKeyException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidApiKey(InvalidApiKeyException e) {
        log.warn("API Key 无效: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "error", "INVALID_API_KEY",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * 处理 LlmProviderNotFoundException 异常，返回 404 响应
     * <p>
     * 当访问不存在的 LLM 提供商时抛出
     * </p>
     *
     * @param e LlmProviderNotFoundException 异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(LlmProviderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLlmProviderNotFound(LlmProviderNotFoundException e) {
        log.warn("LLM 提供商未找到: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "LLM_PROVIDER_NOT_FOUND",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * 处理 LlmModelNotFoundException 异常，返回 404 响应
     * <p>
     * 当访问不存在的 LLM 模型配置时抛出
     * </p>
     *
     * @param e LlmModelNotFoundException 异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(LlmModelNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLlmModelNotFound(LlmModelNotFoundException e) {
        log.warn("LLM 模型未找到: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "LLM_MODEL_NOT_FOUND",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * 处理 UnsupportedProviderTypeException 异常，返回 400 响应
     * <p>
     * 当尝试创建不支持的 LLM 提供商类型时抛出
     * </p>
     *
     * @param e UnsupportedProviderTypeException 异常
     * @return 包含错误信息的 ResponseEntity
     */
    @ExceptionHandler(UnsupportedProviderTypeException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedProviderType(UnsupportedProviderTypeException e) {
        log.warn("不支持的提供商类型: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "UNSUPPORTED_PROVIDER_TYPE",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * Handles validation errors and returns 400 response.
     *
     * @param e the MethodArgumentNotValidException
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "VALIDATION_ERROR",
                "message", errors,
                "timestamp", LocalDateTime.now()
            ));
    }

    /**
     * Handles generic exceptions and returns 500 response.
     *
     * @param e the Exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "An unexpected error occurred",
                "timestamp", LocalDateTime.now()
            ));
    }
}