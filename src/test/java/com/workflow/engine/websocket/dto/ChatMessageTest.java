package com.workflow.engine.websocket.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatMessage 单元测试
 * <p>
 * 测试聊天消息 DTO 的各项功能
 * </p>
 */
class ChatMessageTest {

    @Test
    @DisplayName("创建用户消息_应该正确设置角色和内容")
    void userMessage_shouldHaveCorrectRoleAndContent() {
        // When
        ChatMessage message = ChatMessage.user("测试消息");

        // Then
        assertEquals("user", message.getRole());
        assertEquals("测试消息", message.getContent());
        assertNotNull(message.getTimestamp());
    }

    @Test
    @DisplayName("创建助手消息_应该正确设置角色和内容")
    void assistantMessage_shouldHaveCorrectRoleAndContent() {
        // When
        ChatMessage message = ChatMessage.assistant("助手回复");

        // Then
        assertEquals("assistant", message.getRole());
        assertEquals("助手回复", message.getContent());
        assertNotNull(message.getTimestamp());
    }

    @Test
    @DisplayName("创建系统消息_应该正确设置角色和内容")
    void systemMessage_shouldHaveCorrectRoleAndContent() {
        // When
        ChatMessage message = ChatMessage.system("系统提示");

        // Then
        assertEquals("system", message.getRole());
        assertEquals("系统提示", message.getContent());
        assertNotNull(message.getTimestamp());
    }

    @Test
    @DisplayName("使用Builder创建消息_应该正确设置所有字段")
    void builderCreate_shouldSetAllFields() {
        // Given
        LocalDateTime fixedTime = LocalDateTime.of(2024, 1, 1, 12, 0);

        // When
        ChatMessage message = ChatMessage.builder()
            .role("custom")
            .content("自定义内容")
            .timestamp(fixedTime)
            .build();

        // Then
        assertEquals("custom", message.getRole());
        assertEquals("自定义内容", message.getContent());
        assertEquals(fixedTime, message.getTimestamp());
    }
}