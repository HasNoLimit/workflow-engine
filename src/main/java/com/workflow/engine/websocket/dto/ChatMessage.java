package com.workflow.engine.websocket.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息
 * <p>
 * 用于记录对话中的单条消息，包含角色、内容和时间戳
 * </p>
 */
@Data
@Builder
public class ChatMessage {
    /**
     * 角色: user, assistant, system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 创建用户消息
     *
     * @param content 消息内容
     * @return 用户消息实例
     */
    public static ChatMessage user(String content) {
        return ChatMessage.builder()
            .role("user")
            .content(content)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 创建助手消息
     *
     * @param content 消息内容
     * @return 助手消息实例
     */
    public static ChatMessage assistant(String content) {
        return ChatMessage.builder()
            .role("assistant")
            .content(content)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 创建系统消息
     *
     * @param content 消息内容
     * @return 系统消息实例
     */
    public static ChatMessage system(String content) {
        return ChatMessage.builder()
            .role("system")
            .content(content)
            .timestamp(LocalDateTime.now())
            .build();
    }
}