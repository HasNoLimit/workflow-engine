package com.workflow.engine.service;

import com.workflow.engine.executor.ExecutionResult;
import com.workflow.engine.executor.WorkflowContext;
import com.workflow.engine.executor.WorkflowExecutor;
import com.workflow.engine.model.Agent;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.model.WorkflowVersion;
import com.workflow.engine.websocket.dto.ChatMessage;
import com.workflow.engine.websocket.dto.ChatResponse;
import dev.langchain4j.memory.ChatMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * DialogSessionService 单元测试
 * <p>
 * 测试对话会话服务的各项功能
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class DialogSessionServiceTest {

    @Mock
    private AgentService agentService;

    @Mock
    private WorkflowExecutor workflowExecutor;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowVersionService workflowVersionService;

    private DialogSessionService dialogSessionService;

    @BeforeEach
    void setUp() {
        dialogSessionService = new DialogSessionService(
            agentService,
            workflowExecutor,
            workflowService,
            workflowVersionService
        );
    }

    @Test
    @DisplayName("处理聊天消息_智能体不存在_应该返回错误响应")
    void chat_agentNotExists_shouldReturnError() {
        // Given
        Long agentId = 100L;
        String sessionId = "session-1";
        String message = "你好";
        when(agentService.getAgentById(agentId)).thenReturn(null);

        // When
        ChatResponse response = dialogSessionService.chat(agentId, sessionId, message);

        // Then
        assertEquals("error", response.getType());
        assertEquals("智能体不存在", response.getError());
        assertEquals(sessionId, response.getSessionId());
    }

    @Test
    @DisplayName("处理聊天消息_智能体未激活_应该返回错误响应")
    void chat_agentInactive_shouldReturnError() {
        // Given
        Long agentId = 100L;
        String sessionId = "session-1";
        String message = "你好";
        Agent agent = createInactiveAgent(agentId);
        when(agentService.getAgentById(agentId)).thenReturn(agent);

        // When
        ChatResponse response = dialogSessionService.chat(agentId, sessionId, message);

        // Then
        assertEquals("error", response.getType());
        assertTrue(response.getError().contains("未激活"));
    }

    @Test
    @DisplayName("处理聊天消息_正常情况_应该返回消息响应")
    void chat_normalCase_shouldReturnMessageResponse() {
        // Given
        Long agentId = 100L;
        String sessionId = "session-1";
        String message = "你好";
        Agent agent = createActiveAgent(agentId);

        when(agentService.getAgentById(agentId)).thenReturn(agent);
        when(workflowService.getById(anyLong())).thenReturn(null);

        // When
        ChatResponse response = dialogSessionService.chat(agentId, sessionId, message);

        // Then
        assertEquals("message", response.getType());
        assertEquals(sessionId, response.getSessionId());
        assertEquals(agentId, response.getAgentId());
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains(agent.getName()));
    }

    @Test
    @DisplayName("处理聊天消息_多次调用_应该维护聊天记忆")
    void chat_multipleCalls_shouldMaintainChatMemory() {
        // Given
        Long agentId = 100L;
        String sessionId = "session-1";
        Agent agent = createActiveAgent(agentId);

        when(agentService.getAgentById(agentId)).thenReturn(agent);
        when(workflowService.getById(anyLong())).thenReturn(null);

        // When - 第一次对话
        dialogSessionService.chat(agentId, sessionId, "第一条消息");
        int countAfterFirst = dialogSessionService.getMessageCount(sessionId);

        // When - 第二次对话
        dialogSessionService.chat(agentId, sessionId, "第二条消息");
        int countAfterSecond = dialogSessionService.getMessageCount(sessionId);

        // Then
        assertTrue(countAfterFirst >= 2);  // 用户消息 + AI 响应
        assertTrue(countAfterSecond > countAfterFirst);  // 记忆增长
    }

    @Test
    @DisplayName("清理会话_存在会话_应该成功清理")
    void clearSession_existingSession_shouldClearSuccessfully() {
        // Given
        Long agentId = 100L;
        String sessionId = "session-1";
        Agent agent = createActiveAgent(agentId);

        when(agentService.getAgentById(agentId)).thenReturn(agent);
        when(workflowService.getById(anyLong())).thenReturn(null);

        // 先创建会话
        dialogSessionService.chat(agentId, sessionId, "测试消息");
        assertTrue(dialogSessionService.hasSession(sessionId));

        // When
        dialogSessionService.clearSession(sessionId);

        // Then
        assertFalse(dialogSessionService.hasSession(sessionId));
        assertEquals(0, dialogSessionService.getMessageCount(sessionId));
    }

    @Test
    @DisplayName("清理会话_不存在会话_应该无副作用")
    void clearSession_nonExistingSession_shouldHaveNoEffect() {
        // When
        dialogSessionService.clearSession("nonexistent-session");

        // Then - 不应该抛异常
        assertFalse(dialogSessionService.hasSession("nonexistent-session"));
    }

    @Test
    @DisplayName("获取历史_存在会话_应该返回历史记录")
    void getHistory_existingSession_shouldReturnHistory() {
        // Given
        Long agentId = 100L;
        String sessionId = "session-1";
        Agent agent = createActiveAgent(agentId);

        when(agentService.getAgentById(agentId)).thenReturn(agent);
        when(workflowService.getById(anyLong())).thenReturn(null);

        // 先创建对话
        dialogSessionService.chat(agentId, sessionId, "用户消息");

        // When
        List<ChatMessage> history = dialogSessionService.getHistory(sessionId);

        // Then
        assertFalse(history.isEmpty());
        assertTrue(history.size() >= 2);  // 用户消息 + AI 响应
        assertEquals("user", history.get(0).getRole());
    }

    @Test
    @DisplayName("获取历史_不存在会话_应该返回空列表")
    void getHistory_nonExistingSession_shouldReturnEmptyList() {
        // When
        List<ChatMessage> history = dialogSessionService.getHistory("nonexistent-session");

        // Then
        assertTrue(history.isEmpty());
    }

    @Test
    @DisplayName("获取活跃会话数_多个会话_应该返回正确数量")
    void getActiveSessionCount_multipleSessions_shouldReturnCorrectCount() {
        // Given
        Agent agent = createActiveAgent(100L);
        when(agentService.getAgentById(100L)).thenReturn(agent);
        when(workflowService.getById(anyLong())).thenReturn(null);

        // When
        dialogSessionService.chat(100L, "session-1", "消息1");
        dialogSessionService.chat(100L, "session-2", "消息2");
        dialogSessionService.chat(100L, "session-3", "消息3");

        // Then
        assertEquals(3, dialogSessionService.getActiveSessionCount());
    }

    @Test
    @DisplayName("处理聊天消息_工作流执行成功_应该返回工作流输出")
    void chat_workflowSuccess_shouldReturnWorkflowOutput() {
        // Given
        Long agentId = 100L;
        String sessionId = "session-1";
        String message = "你好";
        Agent agent = createActiveAgent(agentId);
        Workflow workflow = createWorkflow(agent.getWorkflowId());
        WorkflowVersion version = createWorkflowVersion(agent.getWorkflowId(), agent.getWorkflowVersion());

        ExecutionResult successResult = ExecutionResult.builder()
            .success(true)
            .output(Map.of("output", "工作流生成的内容"))
            .build();

        when(agentService.getAgentById(agentId)).thenReturn(agent);
        when(workflowService.getById(agent.getWorkflowId())).thenReturn(workflow);
        when(workflowVersionService.getVersion(agent.getWorkflowId(), agent.getWorkflowVersion()))
            .thenReturn(version);
        when(workflowExecutor.execute(any(WorkflowContext.class))).thenReturn(successResult);

        // When
        ChatResponse response = dialogSessionService.chat(agentId, sessionId, message);

        // Then
        assertEquals("message", response.getType());
        assertEquals("工作流生成的内容", response.getContent());
    }

    /**
     * 创建活跃状态的智能体
     */
    private Agent createActiveAgent(Long agentId) {
        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setName("测试智能体");
        agent.setWorkflowId(1L);
        agent.setWorkflowVersion(1);
        agent.setStatus(Agent.STATUS_ACTIVE);
        return agent;
    }

    /**
     * 创建非活跃状态的智能体
     */
    private Agent createInactiveAgent(Long agentId) {
        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setName("测试智能体");
        agent.setStatus(Agent.STATUS_INACTIVE);
        return agent;
    }

    /**
     * 创建工作流
     */
    private Workflow createWorkflow(Long workflowId) {
        Workflow workflow = new Workflow();
        workflow.setId(workflowId);
        workflow.setName("测试工作流");
        return workflow;
    }

    /**
     * 创建工作流版本
     */
    private WorkflowVersion createWorkflowVersion(Long workflowId, Integer version) {
        WorkflowVersion workflowVersion = new WorkflowVersion();
        workflowVersion.setWorkflowId(workflowId);
        workflowVersion.setVersion(version);
        return workflowVersion;
    }
}