package com.workflow.engine.websocket.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatResponse 单元测试
 * <p>
 * 测试聊天响应 DTO 的各项功能
 * </p>
 */
class ChatResponseTest {

    @Test
    @DisplayName("创建连接成功响应_应该正确设置所有字段")
    void connectedResponse_shouldHaveCorrectFields() {
        // When
        ChatResponse response = ChatResponse.connected("session-123", 100L);

        // Then
        assertEquals("connected", response.getType());
        assertEquals("session-123", response.getSessionId());
        assertEquals(100L, response.getAgentId());
        assertNotNull(response.getTimestamp());
        assertNull(response.getContent());
        assertNull(response.getError());
        assertNull(response.getHistory());
    }

    @Test
    @DisplayName("创建消息响应_不带智能体ID_应该正确设置")
    void messageResponse_withoutAgentId_shouldHaveCorrectFields() {
        // When
        ChatResponse response = ChatResponse.message("session-123", "回复内容");

        // Then
        assertEquals("message", response.getType());
        assertEquals("session-123", response.getSessionId());
        assertEquals("回复内容", response.getContent());
        assertNotNull(response.getTimestamp());
    }

    @Test
    @DisplayName("创建消息响应_带智能体ID_应该正确设置")
    void messageResponse_withAgentId_shouldHaveCorrectFields() {
        // When
        ChatResponse response = ChatResponse.message("session-123", 100L, "回复内容");

        // Then
        assertEquals("message", response.getType());
        assertEquals("session-123", response.getSessionId());
        assertEquals(100L, response.getAgentId());
        assertEquals("回复内容", response.getContent());
        assertNotNull(response.getTimestamp());
    }

    @Test
    @DisplayName("创建错误响应_不带会话ID_应该正确设置")
    void errorResponse_withoutSessionId_shouldHaveCorrectFields() {
        // When
        ChatResponse response = ChatResponse.error("发生错误");

        // Then
        assertEquals("error", response.getType());
        assertEquals("发生错误", response.getError());
        assertNotNull(response.getTimestamp());
        assertNull(response.getSessionId());
    }

    @Test
    @DisplayName("创建错误响应_带会话ID_应该正确设置")
    void errorResponse_withSessionId_shouldHaveCorrectFields() {
        // When
        ChatResponse response = ChatResponse.error("session-123", "发生错误");

        // Then
        assertEquals("error", response.getType());
        assertEquals("session-123", response.getSessionId());
        assertEquals("发生错误", response.getError());
        assertNotNull(response.getTimestamp());
    }

    @Test
    @DisplayName("创建正在输入响应_应该正确设置")
    void typingResponse_shouldHaveCorrectFields() {
        // When
        ChatResponse response = ChatResponse.typing("session-123");

        // Then
        assertEquals("typing", response.getType());
        assertEquals("session-123", response.getSessionId());
        assertNotNull(response.getTimestamp());
        assertNull(response.getContent());
    }

    @Test
    @DisplayName("创建历史记录响应_应该正确设置")
    void historyResponse_shouldHaveCorrectFields() {
        // Given
        List<ChatMessage> history = List.of(
            ChatMessage.user("问题"),
            ChatMessage.assistant("回答")
        );

        // When
        ChatResponse response = ChatResponse.history("session-123", history);

        // Then
        assertEquals("history", response.getType());
        assertEquals("session-123", response.getSessionId());
        assertEquals(history, response.getHistory());
        assertNotNull(response.getTimestamp());
    }
}