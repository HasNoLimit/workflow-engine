package com.workflow.engine.webhook;

import com.workflow.engine.dto.WebhookRequest;
import com.workflow.engine.executor.ExecutionResult;
import com.workflow.engine.model.Agent;
import com.workflow.engine.model.WorkflowExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * Webhook 服务
 * <p>
 * 负责在工作流执行完成后向配置的 URL 发送回调通知
 * 支持异步发送，避免阻塞主流程
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    /** HTTP 客户端，用于发送回调请求 */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 异步发送执行完成通知
     * <p>
     * 在工作流执行完成后，向智能体配置的 Webhook URL 发送回调请求
     * 包含执行ID、状态、输出结果等信息
     * </p>
     *
     * @param agent     智能体信息
     * @param execution 执行记录
     * @param result    执行结果
     */
    @Async
    public void notifyCompletion(Agent agent, WorkflowExecution execution, ExecutionResult result) {
        String webhookUrl = agent.getWebhookUrl();

        // 构建 Webhook 请求体
        WebhookRequest request = WebhookRequest.builder()
                .agentId(agent.getId())
                .agentName(agent.getName())
                .executionId(execution.getId())
                .status(result.isSuccess() ? "SUCCESS" : "FAILED")
                .output(result.getOutput())
                .errorMessage(result.getErrorMessage())
                .durationMs(execution.getDurationMs())
                .timestamp(LocalDateTime.now())
                .build();

        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Agent-Id", String.valueOf(agent.getId()));

            // 发送 POST 请求
            HttpEntity<WebhookRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    webhookUrl, entity, String.class);

            log.info("Webhook 回调成功: url={}, status={}, executionId={}",
                    webhookUrl, response.getStatusCode(), execution.getId());

        } catch (Exception e) {
            // 回调失败不影响主流程，仅记录日志
            log.error("Webhook 回调失败: url={}, executionId={}, error={}",
                    webhookUrl, execution.getId(), e.getMessage(), e);
        }
    }
}