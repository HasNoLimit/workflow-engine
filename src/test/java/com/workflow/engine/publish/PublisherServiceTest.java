package com.workflow.engine.publish;

import com.workflow.engine.dto.AgentCreateRequest;
import com.workflow.engine.dto.AgentEndpoint;
import com.workflow.engine.dto.PublishResult;
import com.workflow.engine.dto.VersionSaveRequest;
import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.engine.model.WorkflowJSON;
import com.workflow.engine.exception.AgentNotFoundException;
import com.workflow.engine.model.Agent;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.model.WorkflowVersion;
import com.workflow.engine.service.AgentService;
import com.workflow.engine.service.WorkflowService;
import com.workflow.engine.service.WorkflowVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PublisherService 集成测试
 * <p>
 * 测试智能体发布服务的核心功能，包括：
 * - 对话智能体发布
 * - API 智能体发布
 * - 取消发布
 * - 发布状态查询
 * - 端点信息获取
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PublisherServiceTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowVersionService workflowVersionService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private PublisherService publisherService;

    private Workflow testWorkflow;
    private WorkflowVersion testVersion;

    @BeforeEach
    void setUp() {
        // 创建测试工作流和版本
        WorkflowCreateRequest workflowRequest = new WorkflowCreateRequest("测试工作流", "用于发布测试");
        testWorkflow = workflowService.create(workflowRequest);

        WorkflowJSON canvas = createTestCanvas();
        VersionSaveRequest versionRequest = new VersionSaveRequest(canvas, "v1");
        testVersion = workflowVersionService.saveVersion(testWorkflow.getId(), versionRequest);
    }

    @Test
    void shouldPublishDialogAgent() {
        // Given: 创建对话智能体
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "对话智能体", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(createRequest);

        // When: 发布智能体
        PublishResult result = publisherService.publish(agent.getId());

        // Then: 验证发布成功
        assertTrue(result.isSuccess());
        assertEquals(agent.getId(), result.getAgentId());
        assertEquals("/ws/agent/" + agent.getId(), result.getEndpoint());
        assertNotNull(result.getApiKey());
        assertEquals("对话智能体发布成功", result.getMessage());

        // 验证智能体状态已激活
        Agent publishedAgent = agentService.getById(agent.getId());
        assertEquals(Agent.STATUS_ACTIVE, publishedAgent.getStatus());
    }

    @Test
    void shouldPublishApiAgent() {
        // Given: 创建 API 智能体
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "API智能体", Agent.TYPE_API);
        Agent agent = agentService.create(createRequest);

        // 设置 Webhook URL
        agent.setWebhookUrl("http://example.com/webhook");
        agentService.updateById(agent);

        // When: 发布智能体
        PublishResult result = publisherService.publish(agent.getId());

        // Then: 验证发布成功
        assertTrue(result.isSuccess());
        assertEquals(agent.getId(), result.getAgentId());
        assertEquals("/api/execute/" + agent.getId(), result.getEndpoint());
        assertNotNull(result.getApiKey());
        assertEquals("http://example.com/webhook", result.getWebhookUrl());
        assertEquals("API 智能体发布成功", result.getMessage());

        // 验证智能体状态已激活
        Agent publishedAgent = agentService.getById(agent.getId());
        assertEquals(Agent.STATUS_ACTIVE, publishedAgent.getStatus());
    }

    @Test
    void shouldFailPublishWithWrongType() {
        // Given: 创建对话智能体
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "对话智能体", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(createRequest);

        // 手动修改类型为无效值（模拟数据异常）
        agent.setType("INVALID");
        agentService.updateById(agent);

        // When: 尝试发布
        PublishResult result = publisherService.publish(agent.getId());

        // Then: 发布失败
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("未知的智能体类型"));
    }

    @Test
    void shouldUnpublishAgent() {
        // Given: 创建并发布智能体
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "测试智能体", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(createRequest);
        publisherService.publish(agent.getId());

        // 验证已激活
        assertEquals(Agent.STATUS_ACTIVE, agentService.getById(agent.getId()).getStatus());

        // When: 取消发布
        publisherService.unpublish(agent.getId());

        // Then: 验证已停用
        Agent unpublishedAgent = agentService.getById(agent.getId());
        assertEquals(Agent.STATUS_INACTIVE, unpublishedAgent.getStatus());
    }

    @Test
    void shouldThrowWhenPublishNonExistentAgent() {
        // When: 尝试发布不存在的智能体
        // Then: 应抛出异常
        assertThrows(AgentNotFoundException.class, () -> publisherService.publish(999L));
    }

    @Test
    void shouldThrowWhenUnpublishNonExistentAgent() {
        // When: 尝试取消发布不存在的智能体
        // Then: 应抛出异常
        assertThrows(AgentNotFoundException.class, () -> publisherService.unpublish(999L));
    }

    @Test
    void shouldCheckPublishStatus() {
        // Given: 创建智能体
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "测试智能体", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(createRequest);

        // When: 创建时未发布
        boolean beforePublish = publisherService.isPublished(agent.getId());
        assertFalse(beforePublish);

        // 发布后
        publisherService.publish(agent.getId());
        boolean afterPublish = publisherService.isPublished(agent.getId());
        assertTrue(afterPublish);

        // 取消发布后
        publisherService.unpublish(agent.getId());
        boolean afterUnpublish = publisherService.isPublished(agent.getId());
        assertFalse(afterUnpublish);
    }

    @Test
    void shouldThrowWhenCheckStatusOfNonExistentAgent() {
        // When: 检查不存在智能体的发布状态
        // Then: 应抛出异常
        assertThrows(AgentNotFoundException.class, () -> publisherService.isPublished(999L));
    }

    @Test
    void shouldGetDialogAgentEndpoint() {
        // Given: 创建并发布对话智能体
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "对话智能体", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(createRequest);

        // When: 获取端点信息
        AgentEndpoint endpoint = publisherService.getEndpoint(agent.getId());

        // Then: 验证端点信息
        assertEquals(agent.getId(), endpoint.getAgentId());
        assertEquals(Agent.TYPE_DIALOG, endpoint.getType());
        assertEquals("/ws/agent/" + agent.getId(), endpoint.getEndpoint());
        assertEquals(agent.getApiKey(), endpoint.getApiKey());
        assertNull(endpoint.getWebhookUrl());
    }

    @Test
    void shouldGetApiAgentEndpoint() {
        // Given: 创建 API 智能体并设置 Webhook
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "API智能体", Agent.TYPE_API);
        Agent agent = agentService.create(createRequest);
        agent.setWebhookUrl("http://example.com/webhook");
        agent.setTimeoutSeconds(300);
        agentService.updateById(agent);

        // When: 获取端点信息
        AgentEndpoint endpoint = publisherService.getEndpoint(agent.getId());

        // Then: 验证端点信息
        assertEquals(agent.getId(), endpoint.getAgentId());
        assertEquals(Agent.TYPE_API, endpoint.getType());
        assertEquals("/api/execute/" + agent.getId(), endpoint.getEndpoint());
        assertEquals(agent.getApiKey(), endpoint.getApiKey());
        assertEquals("http://example.com/webhook", endpoint.getWebhookUrl());
        assertEquals(300, endpoint.getTimeoutSeconds());
    }

    @Test
    void shouldThrowWhenGetEndpointOfNonExistentAgent() {
        // When: 获取不存在智能体的端点
        // Then: 应抛出异常
        assertThrows(AgentNotFoundException.class, () -> publisherService.getEndpoint(999L));
    }

    @Test
    void shouldThrowWhenGetEndpointOfUnknownType() {
        // Given: 创建智能体并设置为无效类型
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "测试智能体", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(createRequest);
        agent.setType("UNKNOWN");
        agentService.updateById(agent);

        // When: 获取端点信息
        // Then: 应抛出异常
        assertThrows(IllegalStateException.class, () -> publisherService.getEndpoint(agent.getId()));
    }

    @Test
    void shouldPublishMultipleAgentsIndependently() {
        // Given: 创建多个智能体
        AgentCreateRequest dialogRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "对话智能体1", Agent.TYPE_DIALOG);
        AgentCreateRequest apiRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "API智能体1", Agent.TYPE_API);

        Agent dialogAgent = agentService.create(dialogRequest);
        Agent apiAgent = agentService.create(apiRequest);
        apiAgent.setWebhookUrl("http://example.com/webhook");
        agentService.updateById(apiAgent);

        // When: 分别发布
        PublishResult dialogResult = publisherService.publish(dialogAgent.getId());
        PublishResult apiResult = publisherService.publish(apiAgent.getId());

        // Then: 都应成功
        assertTrue(dialogResult.isSuccess());
        assertTrue(apiResult.isSuccess());

        // 端点应不同
        assertNotEquals(dialogResult.getEndpoint(), apiResult.getEndpoint());
        assertTrue(dialogResult.getEndpoint().startsWith("/ws/"));
        assertTrue(apiResult.getEndpoint().startsWith("/api/"));
    }

    @Test
    void shouldPublishAlreadyPublishedAgent() {
        // Given: 创建并发布智能体
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "测试智能体", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(createRequest);
        publisherService.publish(agent.getId());

        // When: 再次发布（幂等操作）
        PublishResult result = publisherService.publish(agent.getId());

        // Then: 应返回成功（幂等）
        assertTrue(result.isSuccess());
        assertEquals(Agent.STATUS_ACTIVE, agentService.getById(agent.getId()).getStatus());
    }

    @Test
    void shouldUnpublishAlreadyUnpublishedAgent() {
        // Given: 创建智能体（未发布状态）
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "测试智能体", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(createRequest);

        // When: 取消发布（幂等操作）
        publisherService.unpublish(agent.getId());

        // Then: 状态应保持 INACTIVE
        assertEquals(Agent.STATUS_INACTIVE, agentService.getById(agent.getId()).getStatus());
    }

    /**
     * 创建测试用的画布数据
     */
    private WorkflowJSON createTestCanvas() {
        NodeJSON startNode = new NodeJSON("start_0", "start", Map.of(), Map.of("title", "开始"));
        NodeJSON endNode = new NodeJSON("end_0", "end", Map.of(), Map.of("title", "结束"));
        return new WorkflowJSON(List.of(startNode, endNode), List.of());
    }
}