package com.workflow.engine.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 聊天请求
 * <p>
 * WebSocket 客户端发送的聊天请求格式
 * </p>
 */
@Data
public class ChatRequest {
    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息不能为空")
    private String message;

    /**
     * 会话ID（可选）
     * <p>
     * 用于恢复之前的会话，如果为空则创建新会话
     * </p>
     */
    private String sessionId;
}