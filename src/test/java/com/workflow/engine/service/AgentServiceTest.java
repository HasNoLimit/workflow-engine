package com.workflow.engine.service;

import com.workflow.engine.dto.AgentCreateRequest;
import com.workflow.engine.dto.AgentPublishRequest;
import com.workflow.engine.dto.VersionSaveRequest;
import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.engine.model.WorkflowJSON;
import com.workflow.engine.exception.AgentNotFoundException;
import com.workflow.engine.exception.WorkflowNotFoundException;
import com.workflow.engine.model.Agent;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.model.WorkflowVersion;
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
 * AgentService 集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentServiceTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowVersionService workflowVersionService;

    @Autowired
    private AgentService agentService;

    private Workflow testWorkflow;
    private WorkflowVersion testVersion;

    @BeforeEach
    void setUp() {
        // 创建测试工作流和版本
        WorkflowCreateRequest workflowRequest = new WorkflowCreateRequest("测试工作流", "用于智能体测试");
        testWorkflow = workflowService.create(workflowRequest);

        WorkflowJSON canvas = createTestCanvas();
        VersionSaveRequest versionRequest = new VersionSaveRequest(canvas, "v1");
        testVersion = workflowVersionService.saveVersion(testWorkflow.getId(), versionRequest);
    }

    @Test
    void shouldCreateDialogAgent() {
        // Given: 对话智能体请求
        AgentCreateRequest request = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "对话智能体", Agent.TYPE_DIALOG);

        // When: 创建智能体
        Agent agent = agentService.create(request);

        // Then: 验证智能体信息
        assertNotNull(agent.getId());
        assertEquals("对话智能体", agent.getName());
        assertEquals(Agent.TYPE_DIALOG, agent.getType());
        assertEquals(Agent.STATUS_INACTIVE, agent.getStatus());
        assertNotNull(agent.getApiKey());
        assertEquals(32, agent.getApiKey().length());
        assertEquals(testWorkflow.getId(), agent.getWorkflowId());
        assertEquals(testVersion.getVersion(), agent.getWorkflowVersion());
    }

    @Test
    void shouldCreateApiAgent() {
        // Given: API智能体请求
        AgentCreateRequest request = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "API智能体", Agent.TYPE_API);

        // When: 创建智能体
        Agent agent = agentService.create(request);

        // Then: 验证智能体类型
        assertEquals(Agent.TYPE_API, agent.getType());
    }

    @Test
    void shouldThrowWhenInvalidType() {
        // Given: 无效类型
        AgentCreateRequest request = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "测试", "INVALID");

        // Then: 应抛出异常
        assertThrows(IllegalArgumentException.class, () -> agentService.create(request));
    }

    @Test
    void shouldThrowWhenWorkflowNotFound() {
        // Given: 不存在的工作流ID
        AgentCreateRequest request = new AgentCreateRequest(999L, 1, "测试", Agent.TYPE_DIALOG);

        // Then: 应抛出异常
        assertThrows(WorkflowNotFoundException.class, () -> agentService.create(request));
    }

    @Test
    void shouldPublishAgent() {
        // Given: 创建智能体
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "API智能体", Agent.TYPE_API);
        Agent agent = agentService.create(createRequest);

        // When: 发布智能体
        AgentPublishRequest publishRequest = new AgentPublishRequest("http://example.com/webhook", 300);
        Agent published = agentService.publish(agent.getId(), publishRequest);

        // Then: 验证发布状态
        assertEquals(Agent.STATUS_ACTIVE, published.getStatus());
        assertEquals("http://example.com/webhook", published.getWebhookUrl());
        assertEquals(300, published.getTimeoutSeconds());
    }

    @Test
    void shouldPublishDialogAgentWithoutWebhook() {
        // Given: 创建对话智能体
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "对话智能体", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(createRequest);

        // When: 发布智能体
        AgentPublishRequest publishRequest = new AgentPublishRequest(null, 60);
        Agent published = agentService.publish(agent.getId(), publishRequest);

        // Then: 验证发布状态（对话类型不需要 webhook）
        assertEquals(Agent.STATUS_ACTIVE, published.getStatus());
        assertNull(published.getWebhookUrl());
        assertEquals(60, published.getTimeoutSeconds());
    }

    @Test
    void shouldThrowWhenPublishNonExistentAgent() {
        // Given: 不存在的智能体ID
        AgentPublishRequest publishRequest = new AgentPublishRequest("http://example.com/webhook", 300);

        // Then: 应抛出异常
        assertThrows(AgentNotFoundException.class, () -> agentService.publish(999L, publishRequest));
    }

    @Test
    void shouldActivateAndDeactivate() {
        // Given: 创建并发布智能体
        AgentCreateRequest createRequest = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "测试", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(createRequest);
        agentService.publish(agent.getId(), new AgentPublishRequest(null, 300));

        // When: 停用
        agentService.deactivate(agent.getId());
        Agent deactivated = agentService.getById(agent.getId());
        assertEquals(Agent.STATUS_INACTIVE, deactivated.getStatus());

        // When: 激活
        agentService.activate(agent.getId());
        Agent activated = agentService.getById(agent.getId());
        assertEquals(Agent.STATUS_ACTIVE, activated.getStatus());
    }

    @Test
    void shouldThrowWhenDeactivateNonExistentAgent() {
        // Then: 应抛出异常
        assertThrows(AgentNotFoundException.class, () -> agentService.deactivate(999L));
    }

    @Test
    void shouldThrowWhenActivateNonExistentAgent() {
        // Then: 应抛出异常
        assertThrows(AgentNotFoundException.class, () -> agentService.activate(999L));
    }

    @Test
    void shouldFindByApiKey() {
        // Given: 创建智能体
        AgentCreateRequest request = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "测试", Agent.TYPE_API);
        Agent agent = agentService.create(request);

        // When: 按API Key查询
        Agent found = agentService.findByApiKey(agent.getApiKey());

        // Then: 验证查询结果
        assertNotNull(found);
        assertEquals(agent.getId(), found.getId());
        assertEquals(agent.getApiKey(), found.getApiKey());
    }

    @Test
    void shouldReturnNullWhenApiKeyNotFound() {
        // When: 按不存在的API Key查询
        Agent found = agentService.findByApiKey("nonexistent-api-key-12345");

        // Then: 返回 null
        assertNull(found);
    }

    @Test
    void shouldFindByWorkflowId() {
        // Given: 创建多个智能体
        for (int i = 1; i <= 2; i++) {
            agentService.create(new AgentCreateRequest(
                testWorkflow.getId(), testVersion.getVersion(), "智能体" + i, Agent.TYPE_DIALOG));
        }

        // When: 按工作流查询
        List<Agent> agents = agentService.findByWorkflowId(testWorkflow.getId());

        // Then: 验证查询结果
        assertEquals(2, agents.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoAgentsForWorkflow() {
        // When: 按没有智能体的工作流查询
        List<Agent> agents = agentService.findByWorkflowId(999L);

        // Then: 返回空列表
        assertNotNull(agents);
        assertTrue(agents.isEmpty());
    }

    @Test
    void shouldFindByType() {
        // Given: 创建不同类型智能体
        agentService.create(new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "对话1", Agent.TYPE_DIALOG));
        agentService.create(new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "API1", Agent.TYPE_API));

        // 发布智能体（findByType 只返回 ACTIVE 状态）
        List<Agent> allAgents = agentService.list();
        for (Agent agent : allAgents) {
            if (agent.getName().equals("对话1") || agent.getName().equals("API1")) {
                agentService.publish(agent.getId(), new AgentPublishRequest(null, 300));
            }
        }

        // When: 按类型查询
        List<Agent> dialogAgents = agentService.findByType(Agent.TYPE_DIALOG);
        List<Agent> apiAgents = agentService.findByType(Agent.TYPE_API);

        // Then: 验证查询结果（findByType 只返回 ACTIVE 状态的智能体）
        assertTrue(dialogAgents.stream().allMatch(a -> a.getType().equals(Agent.TYPE_DIALOG)));
        assertTrue(apiAgents.stream().allMatch(a -> a.getType().equals(Agent.TYPE_API)));
    }

    @Test
    void shouldListAllAgents() {
        // Given: 创建多个智能体
        agentService.create(new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "智能体1", Agent.TYPE_DIALOG));
        agentService.create(new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "智能体2", Agent.TYPE_API));

        // When: 获取列表
        List<Agent> agents = agentService.list();

        // Then: 验证列表包含创建的智能体
        assertTrue(agents.size() >= 2);
    }

    @Test
    void shouldDeleteAgent() {
        // Given: 创建智能体
        AgentCreateRequest request = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "待删除", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(request);

        // When: 删除智能体
        agentService.removeById(agent.getId());

        // Then: 智能体已不存在
        Agent deleted = agentService.getById(agent.getId());
        assertNull(deleted);
    }

    @Test
    void shouldGenerateUniqueApiKeys() {
        // Given: 创建两个智能体
        AgentCreateRequest request1 = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "智能体1", Agent.TYPE_DIALOG);
        AgentCreateRequest request2 = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "智能体2", Agent.TYPE_DIALOG);

        // When: 创建智能体
        Agent agent1 = agentService.create(request1);
        Agent agent2 = agentService.create(request2);

        // Then: API Key 应唯一
        assertNotNull(agent1.getApiKey());
        assertNotNull(agent2.getApiKey());
        assertNotEquals(agent1.getApiKey(), agent2.getApiKey());
    }

    @Test
    void shouldGetAgentById() {
        // Given: 创建智能体
        AgentCreateRequest request = new AgentCreateRequest(
            testWorkflow.getId(), testVersion.getVersion(), "查询测试", Agent.TYPE_DIALOG);
        Agent agent = agentService.create(request);

        // When: 按ID查询
        Agent found = agentService.getById(agent.getId());

        // Then: 返回正确智能体
        assertNotNull(found);
        assertEquals(agent.getId(), found.getId());
        assertEquals("查询测试", found.getName());
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