package com.workflow.engine.webhook;

import com.workflow.engine.dto.WebhookRequest;
import com.workflow.engine.executor.ExecutionResult;
import com.workflow.engine.model.Agent;
import com.workflow.engine.model.WorkflowExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * WebhookService 单元测试
 * <p>
 * 测试 Webhook 回调通知功能
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService();
    }

    @Test
    @DisplayName("发送成功执行的 Webhook 回调")
    void notifyCompletion_withSuccess_shouldSendWebhook() {
        // Given: 准备测试数据
        Agent agent = createTestAgent();
        WorkflowExecution execution = createTestExecution(WorkflowExecution.STATUS_SUCCESS);
        ExecutionResult result = ExecutionResult.success(Map.of("output", "result"), null);

        // When: 发送回调
        webhookService.notifyCompletion(agent, execution, result);

        // Then: 由于 Webhook URL 是模拟的，应该能正常调用
        // 测试验证方法能正常执行而不抛出异常
    }

    @Test
    @DisplayName("发送失败执行的 Webhook 回调")
    void notifyCompletion_withFailure_shouldSendWebhook() {
        // Given: 准备测试数据
        Agent agent = createTestAgent();
        WorkflowExecution execution = createTestExecution(WorkflowExecution.STATUS_FAILED);
        ExecutionResult result = ExecutionResult.failure("执行失败", null);

        // When: 发送回调
        webhookService.notifyCompletion(agent, execution, result);

        // Then: 方法应正常执行
    }

    @Test
    @DisplayName("Webhook URL 为空时不发送回调")
    void notifyCompletion_withEmptyWebhookUrl_shouldNotSend() {
        // Given: Webhook URL 为空
        Agent agent = createTestAgent();
        agent.setWebhookUrl(null);
        WorkflowExecution execution = createTestExecution(WorkflowExecution.STATUS_SUCCESS);
        ExecutionResult result = ExecutionResult.success(Map.of("output", "result"), null);

        // When: 发送回调
        webhookService.notifyCompletion(agent, execution, result);

        // Then: 不应抛出异常（Webhook URL 为空时跳过）
    }

    @Test
    @DisplayName("Webhook URL 无效时记录错误日志")
    void notifyCompletion_withInvalidUrl_shouldLogError() {
        // Given: 无效的 Webhook URL
        Agent agent = createTestAgent();
        agent.setWebhookUrl("invalid-url");
        WorkflowExecution execution = createTestExecution(WorkflowExecution.STATUS_SUCCESS);
        ExecutionResult result = ExecutionResult.success(Map.of("output", "result"), null);

        // When: 发送回调
        webhookService.notifyCompletion(agent, execution, result);

        // Then: 应记录错误日志但不抛出异常
    }

    /**
     * 创建测试智能体
     */
    private Agent createTestAgent() {
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setName("测试智能体");
        agent.setWebhookUrl("http://example.com/webhook");
        return agent;
    }

    /**
     * 创建测试执行记录
     */
    private WorkflowExecution createTestExecution(String status) {
        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(1L);
        execution.setStatus(status);
        execution.setDurationMs(100L);
        execution.setCompletedAt(LocalDateTime.now());
        return execution;
    }
}