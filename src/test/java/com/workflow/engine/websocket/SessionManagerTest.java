package com.workflow.engine.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SessionManager 单元测试
 * <p>
 * 测试 WebSocket 会话管理器的各项功能
 * </p>
 */
class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
    }

    @Test
    @DisplayName("注册会话_正常情况_应该成功注册")
    void registerSession_withValidParams_shouldRegisterSuccessfully() {
        // Given
        WebSocketSession mockSession = createMockSession("session-1");
        Long agentId = 100L;

        // When
        sessionManager.registerSession("session-1", mockSession, agentId);

        // Then
        assertTrue(sessionManager.hasSession("session-1"));
        assertEquals(mockSession, sessionManager.getSession("session-1"));
        assertEquals(agentId, sessionManager.getAgentId("session-1"));
        assertEquals(1, sessionManager.getActiveSessionCount());
    }

    @Test
    @DisplayName("注册多个会话_应该正确管理所有会话")
    void registerMultipleSessions_shouldManageAllCorrectly() {
        // Given
        WebSocketSession session1 = createMockSession("session-1");
        WebSocketSession session2 = createMockSession("session-2");
        WebSocketSession session3 = createMockSession("session-3");

        // When
        sessionManager.registerSession("session-1", session1, 100L);
        sessionManager.registerSession("session-2", session2, 101L);
        sessionManager.registerSession("session-3", session3, 102L);

        // Then
        assertEquals(3, sessionManager.getActiveSessionCount());
        assertEquals(100L, sessionManager.getAgentId("session-1"));
        assertEquals(101L, sessionManager.getAgentId("session-2"));
        assertEquals(102L, sessionManager.getAgentId("session-3"));
    }

    @Test
    @DisplayName("移除会话_已注册会话_应该成功移除")
    void removeSession_existingSession_shouldRemoveSuccessfully() {
        // Given
        WebSocketSession mockSession = createMockSession("session-1");
        sessionManager.registerSession("session-1", mockSession, 100L);

        // When
        sessionManager.removeSession("session-1");

        // Then
        assertFalse(sessionManager.hasSession("session-1"));
        assertNull(sessionManager.getSession("session-1"));
        assertNull(sessionManager.getAgentId("session-1"));
        assertEquals(0, sessionManager.getActiveSessionCount());
    }

    @Test
    @DisplayName("移除会话_不存在会话_应该无副作用")
    void removeSession_nonExistingSession_shouldHaveNoEffect() {
        // Given
        WebSocketSession mockSession = createMockSession("session-1");
        sessionManager.registerSession("session-1", mockSession, 100L);

        // When
        sessionManager.removeSession("session-nonexistent");

        // Then
        assertEquals(1, sessionManager.getActiveSessionCount());
        assertTrue(sessionManager.hasSession("session-1"));
    }

    @Test
    @DisplayName("获取会话_不存在_应该返回null")
    void getSession_nonExistingSession_shouldReturnNull() {
        // When
        WebSocketSession result = sessionManager.getSession("nonexistent");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("获取智能体ID_不存在会话_应该返回null")
    void getAgentId_nonExistingSession_shouldReturnNull() {
        // When
        Long agentId = sessionManager.getAgentId("nonexistent");

        // Then
        assertNull(agentId);
    }

    @Test
    @DisplayName("检查会话存在_已注册_应该返回true")
    void hasSession_existingSession_shouldReturnTrue() {
        // Given
        WebSocketSession mockSession = createMockSession("session-1");
        sessionManager.registerSession("session-1", mockSession, 100L);

        // When
        boolean exists = sessionManager.hasSession("session-1");

        // Then
        assertTrue(exists);
    }

    @Test
    @DisplayName("检查会话存在_未注册_应该返回false")
    void hasSession_nonExistingSession_shouldReturnFalse() {
        // When
        boolean exists = sessionManager.hasSession("session-1");

        // Then
        assertFalse(exists);
    }

    @Test
    @DisplayName("获取连接时间_已注册会话_应该返回正确时间")
    void getConnectedAt_existingSession_shouldReturnTimestamp() {
        // Given
        WebSocketSession mockSession = createMockSession("session-1");
        sessionManager.registerSession("session-1", mockSession, 100L);

        // When
        long connectedAt = sessionManager.getConnectedAt("session-1");

        // Then
        assertTrue(connectedAt > 0);
        assertTrue(connectedAt <= System.currentTimeMillis());
    }

    @Test
    @DisplayName("获取连接时间_不存在会话_应该返回-1")
    void getConnectedAt_nonExistingSession_shouldReturnMinusOne() {
        // When
        long connectedAt = sessionManager.getConnectedAt("nonexistent");

        // Then
        assertEquals(-1, connectedAt);
    }

    @Test
    @DisplayName("关闭指定会话_已注册会话_应该成功关闭")
    void closeSession_existingSession_shouldCloseSuccessfully() throws IOException {
        // Given
        WebSocketSession mockSession = createMockSession("session-1");
        sessionManager.registerSession("session-1", mockSession, 100L);

        // When
        sessionManager.closeSession("session-1", "test close");

        // Then
        assertFalse(sessionManager.hasSession("session-1"));
        verify(mockSession).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("关闭所有会话_多个活跃会话_应该全部关闭")
    void closeAllSessions_multipleActiveSessions_shouldCloseAll() throws IOException {
        // Given
        WebSocketSession session1 = createMockSession("session-1");
        WebSocketSession session2 = createMockSession("session-2");
        sessionManager.registerSession("session-1", session1, 100L);
        sessionManager.registerSession("session-2", session2, 101L);

        // When
        sessionManager.closeAllSessions();

        // Then
        assertEquals(0, sessionManager.getActiveSessionCount());
        verify(session1).close();
        verify(session2).close();
    }

    @Test
    @DisplayName("关闭所有会话_关闭失败_应该继续关闭其他会话")
    void closeAllSessions_closeFails_shouldContinueClosingOthers() throws IOException {
        // Given
        WebSocketSession session1 = createMockSession("session-1");
        WebSocketSession session2 = createMockSession("session-2");
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        // 模拟第一个会话关闭失败
        doThrow(new IOException("close error")).when(session1).close();

        sessionManager.registerSession("session-1", session1, 100L);
        sessionManager.registerSession("session-2", session2, 101L);

        // When
        sessionManager.closeAllSessions();

        // Then
        assertEquals(0, sessionManager.getActiveSessionCount());
        verify(session1).close();
        verify(session2).close(); // 第二个会话仍应该被关闭
    }

    /**
     * 创建模拟 WebSocket 会话
     */
    private WebSocketSession createMockSession(String sessionId) {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn(sessionId);
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession.getUri()).thenReturn(URI.create("/ws/agent/100"));
        return mockSession;
    }
}