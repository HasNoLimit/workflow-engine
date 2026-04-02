package com.workflow.engine.websocket.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天响应
 * <p>
 * WebSocket 服务端返回的聊天响应格式
 * </p>
 */
@Data
@Builder
public class ChatResponse {
    /**
     * 消息类型
     * <p>
     * - connected: 连接成功
     * - message: 聊天消息
     * - error: 错误消息
     * - typing: 正在输入
     * - history: 历史记录
     * </p>
     */
    private String type;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 响应内容
     */
    private String content;

    /**
     * 对话历史
     */
    private List<ChatMessage> history;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 创建连接成功响应
     *
     * @param sessionId 会话ID
     * @param agentId    智能体ID
     * @return 连接成功响应
     */
    public static ChatResponse connected(String sessionId, Long agentId) {
        return ChatResponse.builder()
            .type("connected")
            .sessionId(sessionId)
            .agentId(agentId)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 创建消息响应
     *
     * @param sessionId 会话ID
     * @param content   消息内容
     * @return 消息响应
     */
    public static ChatResponse message(String sessionId, String content) {
        return ChatResponse.builder()
            .type("message")
            .sessionId(sessionId)
            .content(content)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 创建带智能体ID的消息响应
     *
     * @param sessionId 会话ID
     * @param agentId   智能体ID
     * @param content   消息内容
     * @return 消息响应
     */
    public static ChatResponse message(String sessionId, Long agentId, String content) {
        return ChatResponse.builder()
            .type("message")
            .sessionId(sessionId)
            .agentId(agentId)
            .content(content)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 创建错误响应
     *
     * @param error 错误信息
     * @return 错误响应
     */
    public static ChatResponse error(String error) {
        return ChatResponse.builder()
            .type("error")
            .error(error)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 创建带会话ID的错误响应
     *
     * @param sessionId 会话ID
     * @param error     错误信息
     * @return 错误响应
     */
    public static ChatResponse error(String sessionId, String error) {
        return ChatResponse.builder()
            .type("error")
            .sessionId(sessionId)
            .error(error)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 创建正在输入响应
     *
     * @param sessionId 会话ID
     * @return 正在输入响应
     */
    public static ChatResponse typing(String sessionId) {
        return ChatResponse.builder()
            .type("typing")
            .sessionId(sessionId)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 创建历史记录响应
     *
     * @param sessionId 会话ID
     * @param history   对话历史
     * @return 历史记录响应
     */
    public static ChatResponse history(String sessionId, List<ChatMessage> history) {
        return ChatResponse.builder()
            .type("history")
            .sessionId(sessionId)
            .history(history)
            .timestamp(LocalDateTime.now())
            .build();
    }
}