package com.workflow.engine.websocket.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatRequest 单元测试
 * <p>
 * 测试聊天请求 DTO 的各项功能
 * </p>
 */
class ChatRequestTest {

    @Test
    @DisplayName("设置消息内容_应该正确存储")
    void setMessage_shouldStoreCorrectly() {
        // Given
        ChatRequest request = new ChatRequest();

        // When
        request.setMessage("测试消息");

        // Then
        assertEquals("测试消息", request.getMessage());
    }

    @Test
    @DisplayName("设置会话ID_应该正确存储")
    void setSessionId_shouldStoreCorrectly() {
        // Given
        ChatRequest request = new ChatRequest();

        // When
        request.setSessionId("session-123");

        // Then
        assertEquals("session-123", request.getSessionId());
    }

    @Test
    @DisplayName("创建请求_所有字段默认null")
    void newRequest_allFieldsNullByDefault() {
        // When
        ChatRequest request = new ChatRequest();

        // Then
        assertNull(request.getMessage());
        assertNull(request.getSessionId());
    }

    @Test
    @DisplayName("设置空消息_应该能存储")
    void setMessage_emptyMessage_shouldStore() {
        // Given
        ChatRequest request = new ChatRequest();

        // When
        request.setMessage("");

        // Then
        assertEquals("", request.getMessage());
    }

    @Test
    @DisplayName("设置空白消息_应该能存储")
    void setMessage_whitespaceMessage_shouldStore() {
        // Given
        ChatRequest request = new ChatRequest();

        // When
        request.setMessage("   ");

        // Then
        assertEquals("   ", request.getMessage());
    }
}