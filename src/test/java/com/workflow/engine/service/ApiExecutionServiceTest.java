package com.workflow.engine.service;

import com.workflow.engine.dto.AgentCreateRequest;
import com.workflow.engine.dto.AgentPublishRequest;
import com.workflow.engine.dto.AsyncExecutionResponse;
import com.workflow.engine.dto.ExecutionRequest;
import com.workflow.engine.dto.ExecutionResponse;
import com.workflow.engine.dto.VersionSaveRequest;
import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.engine.model.WorkflowJSON;
import com.workflow.engine.exception.AgentNotFoundException;
import com.workflow.engine.exception.AgentNotActiveException;
import com.workflow.engine.exception.InvalidAgentTypeException;
import com.workflow.engine.exception.InvalidApiKeyException;
import com.workflow.engine.exception.WorkflowExecutionException;
import com.workflow.engine.model.Agent;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.model.WorkflowExecution;
import com.workflow.engine.model.WorkflowVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiExecutionService 集成测试
 * <p>
 * 测试 API 智能体的同步和异步执行功能
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiExecutionServiceTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowVersionService workflowVersionService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ApiExecutionService apiExecutionService;

    @Autowired
    private WorkflowExecutionService workflowExecutionService;

    private Workflow testWorkflow;
    private WorkflowVersion testVersion;
    private Agent testApiAgent;

    @BeforeEach
    void setUp() {
        // 创建测试工作流和版本
        WorkflowCreateRequest workflowRequest = new WorkflowCreateRequest("测试工作流", "用于API执行测试");
        testWorkflow = workflowService.create(workflowRequest);

        WorkflowJSON canvas = createTestCanvas();
        VersionSaveRequest versionRequest = new VersionSaveRequest(canvas, "v1");
        testVersion = workflowVersionService.saveVersion(testWorkflow.getId(), versionRequest);

        // 创建并发布 API 智能体
        AgentCreateRequest agentRequest = new AgentCreateRequest(
                testWorkflow.getId(), testVersion.getVersion(), "API测试智能体", Agent.TYPE_API);
        testApiAgent = agentService.create(agentRequest);

        // 发布智能体（设置 Webhook URL）
        AgentPublishRequest publishRequest = new AgentPublishRequest("http://example.com/webhook", 300);
        testApiAgent = agentService.publish(testApiAgent.getId(), publishRequest);
    }

    @Test
    @DisplayName("同步执行 API 智能体 - 成功")
    void executeSync_withValidAgent_shouldSucceed() {
        // Given: 准备执行请求
        ExecutionRequest request = new ExecutionRequest(Map.of("inputKey", "inputValue"));
        String apiKey = testApiAgent.getApiKey();

        // When: 同步执行
        ExecutionResponse response = apiExecutionService.executeSync(testApiAgent.getId(), apiKey, request);

        // Then: 验证执行结果
        assertNotNull(response);
        assertNotNull(response.getExecutionId());
        assertEquals(WorkflowExecution.STATUS_SUCCESS, response.getStatus());
        assertNotNull(response.getOutput());
        assertNotNull(response.getDurationMs());
        assertNotNull(response.getCompletedAt());

        // 验证执行记录已保存
        WorkflowExecution execution = workflowExecutionService.getById(response.getExecutionId());
        assertNotNull(execution);
        assertEquals(testWorkflow.getId(), execution.getWorkflowId());
        assertEquals(testApiAgent.getId(), execution.getAgentId());
    }

    @Test
    @DisplayName("同步执行 - 智能体不存在")
    void executeSync_withNonExistentAgent_shouldThrowAgentNotFoundException() {
        // Given: 不存在的智能体ID
        ExecutionRequest request = new ExecutionRequest(Map.of("key", "value"));

        // Then: 应抛出 AgentNotFoundException
        assertThrows(AgentNotFoundException.class, () ->
                apiExecutionService.executeSync(999L, "any-api-key", request));
    }

    @Test
    @DisplayName("同步执行 - 智能体类型不是 API")
    void executeSync_withDialogAgent_shouldThrowInvalidAgentTypeException() {
        // Given: 创建对话智能体
        AgentCreateRequest dialogRequest = new AgentCreateRequest(
                testWorkflow.getId(), testVersion.getVersion(), "对话智能体", Agent.TYPE_DIALOG);
        Agent dialogAgent = agentService.create(dialogRequest);
        agentService.publish(dialogAgent.getId(), new AgentPublishRequest(null, 300));

        ExecutionRequest request = new ExecutionRequest(Map.of("key", "value"));

        // Then: 应抛出 InvalidAgentTypeException
        assertThrows(InvalidAgentTypeException.class, () ->
                apiExecutionService.executeSync(dialogAgent.getId(), dialogAgent.getApiKey(), request));
    }

    @Test
    @DisplayName("同步执行 - 智能体未激活")
    void executeSync_withInactiveAgent_shouldThrowAgentNotActiveException() {
        // Given: 创建但不发布智能体（INACTIVE 状态）
        AgentCreateRequest inactiveRequest = new AgentCreateRequest(
                testWorkflow.getId(), testVersion.getVersion(), "未激活智能体", Agent.TYPE_API);
        Agent inactiveAgent = agentService.create(inactiveRequest);

        ExecutionRequest request = new ExecutionRequest(Map.of("key", "value"));

        // Then: 应抛出 AgentNotActiveException
        assertThrows(AgentNotActiveException.class, () ->
                apiExecutionService.executeSync(inactiveAgent.getId(), inactiveAgent.getApiKey(), request));
    }

    @Test
    @DisplayName("同步执行 - API Key 无效")
    void executeSync_withInvalidApiKey_shouldThrowInvalidApiKeyException() {
        // Given: 错误的 API Key
        ExecutionRequest request = new ExecutionRequest(Map.of("key", "value"));
        String wrongApiKey = "wrong-api-key-12345";

        // Then: 应抛出 InvalidApiKeyException
        assertThrows(InvalidApiKeyException.class, () ->
                apiExecutionService.executeSync(testApiAgent.getId(), wrongApiKey, request));
    }

    @Test
    @DisplayName("异步执行 API 智能体 - 成功")
    void executeAsync_withValidAgent_shouldReturnExecutionId() {
        // Given: 准备执行请求
        ExecutionRequest request = new ExecutionRequest(Map.of("inputKey", "inputValue"));
        String apiKey = testApiAgent.getApiKey();

        // When: 异步执行
        AsyncExecutionResponse response = apiExecutionService.executeAsync(testApiAgent.getId(), apiKey, request);

        // Then: 验证立即返回执行ID
        assertNotNull(response);
        assertNotNull(response.getExecutionId());
        // 注意：在测试环境中，@Async 可能使用同步执行器，导致立即执行完成
        // 所以状态可能是 RUNNING 或 SUCCESS
        assertTrue(response.getStatus().equals(WorkflowExecution.STATUS_RUNNING)
                || response.getStatus().equals(WorkflowExecution.STATUS_SUCCESS));
        assertTrue(response.getMessage().contains("已提交"));

        // 验证执行记录已创建
        WorkflowExecution execution = workflowExecutionService.getById(response.getExecutionId());
        assertNotNull(execution);
        // 状态可能是 RUNNING（刚创建）或 SUCCESS（已执行完成）
        assertTrue(execution.getStatus().equals(WorkflowExecution.STATUS_RUNNING)
                || execution.getStatus().equals(WorkflowExecution.STATUS_SUCCESS)
                || execution.getStatus().equals(WorkflowExecution.STATUS_FAILED));
    }

    @Test
    @DisplayName("异步执行 - 智能体不存在")
    void executeAsync_withNonExistentAgent_shouldThrowAgentNotFoundException() {
        // Given: 不存在的智能体ID
        ExecutionRequest request = new ExecutionRequest(Map.of("key", "value"));

        // Then: 应抛出 AgentNotFoundException
        assertThrows(AgentNotFoundException.class, () ->
                apiExecutionService.executeAsync(999L, "any-api-key", request));
    }

    @Test
    @DisplayName("异步执行 - API Key 无效")
    void executeAsync_withInvalidApiKey_shouldThrowInvalidApiKeyException() {
        // Given: 错误的 API Key
        ExecutionRequest request = new ExecutionRequest(Map.of("key", "value"));
        String wrongApiKey = "wrong-api-key-12345";

        // Then: 应抛出 InvalidApiKeyException
        assertThrows(InvalidApiKeyException.class, () ->
                apiExecutionService.executeAsync(testApiAgent.getId(), wrongApiKey, request));
    }

    @Test
    @DisplayName("查询执行状态 - 成功")
    void getExecutionStatus_withValidExecutionId_shouldReturnStatus() {
        // Given: 先执行一个工作流
        ExecutionRequest request = new ExecutionRequest(Map.of("key", "value"));
        ExecutionResponse execResponse = apiExecutionService.executeSync(
                testApiAgent.getId(), testApiAgent.getApiKey(), request);

        // When: 查询执行状态
        ExecutionResponse statusResponse = apiExecutionService.getExecutionStatus(execResponse.getExecutionId());

        // Then: 验证状态信息
        assertNotNull(statusResponse);
        assertEquals(execResponse.getExecutionId(), statusResponse.getExecutionId());
        assertEquals(WorkflowExecution.STATUS_SUCCESS, statusResponse.getStatus());
    }

    @Test
    @DisplayName("查询执行状态 - 执行不存在")
    void getExecutionStatus_withNonExistentExecution_shouldThrowWorkflowExecutionException() {
        // Then: 应抛出 WorkflowExecutionException
        assertThrows(WorkflowExecutionException.class, () ->
                apiExecutionService.getExecutionStatus(999L));
    }

    @Test
    @DisplayName("输入参数为空 Map - 正常执行")
    void executeSync_withEmptyInput_shouldSucceed() {
        // Given: 输入参数为空 Map（不是 null）
        ExecutionRequest request = new ExecutionRequest(Map.of());
        String apiKey = testApiAgent.getApiKey();

        // When: 同步执行
        ExecutionResponse response = apiExecutionService.executeSync(testApiAgent.getId(), apiKey, request);

        // Then: 应正常执行
        assertNotNull(response);
        assertEquals(WorkflowExecution.STATUS_SUCCESS, response.getStatus());
    }

    /**
     * 创建测试用的画布数据
     * <p>
     * 包含 start 和 end 节点的简单工作流
     * </p>
     */
    private WorkflowJSON createTestCanvas() {
        NodeJSON startNode = new NodeJSON("start_0", "start", Map.of(), Map.of("title", "开始"));
        NodeJSON endNode = new NodeJSON("end_0", "end", Map.of(), Map.of("title", "结束"));
        return new WorkflowJSON(List.of(startNode, endNode), List.of());
    }
}