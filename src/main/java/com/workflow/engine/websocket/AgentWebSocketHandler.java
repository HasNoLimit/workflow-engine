package com.workflow.engine.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.engine.websocket.dto.ChatRequest;
import com.workflow.engine.websocket.dto.ChatResponse;
import com.workflow.engine.service.DialogSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;

/**
 * 智能体对话 WebSocket 处理器
 * <p>
 * 处理用户与智能体的实时对话通信，包括：
 * - 连接建立和握手
 * - 消息接收和处理
 * - 响应推送
 * - 连接断开和清理
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentWebSocketHandler implements WebSocketHandler {

    /**
     * 对话会话服务
     */
    private final DialogSessionService dialogSessionService;

    /**
     * WebSocket 会话管理器
     */
    private final SessionManager sessionManager;

    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 连接建立时调用
     * <p>
     * 处理流程:
     * 1. 从 URI 中提取智能体ID
     * 2. 注册会话到管理器
     * 3. 发送连接成功确认消息
     * </p>
     *
     * @param session WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 URI 中提取智能体ID
        Long agentId = extractAgentId(session);
        String sessionId = session.getId();

        // 注册会话到管理器
        sessionManager.registerSession(sessionId, session, agentId);

        log.info("WebSocket 连接建立: sessionId={}, agentId={}, remoteAddress={}",
            sessionId, agentId, session.getRemoteAddress());

        // 发送连接成功确认消息
        sendMessage(session, ChatResponse.connected(sessionId, agentId));
    }

    /**
     * 接收消息时调用
     * <p>
     * 处理流程:
     * 1. 解析消息内容
     * 2. 验证消息格式
     * 3. 调用对话服务处理消息
     * 4. 发送响应给客户端
     * </p>
     *
     * @param session WebSocket 会话
     * @param message  接收到的消息
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // 只处理文本消息
        if (!(message instanceof TextMessage textMessage)) {
            log.warn("收到非文本消息: sessionId={}, messageClass={}",
                session.getId(), message.getClass().getSimpleName());
            return;
        }

        String payload = textMessage.getPayload();
        log.debug("收到消息: sessionId={}, payload={}", session.getId(), payload);

        try {
            // 解析请求 JSON
            ChatRequest request = objectMapper.readValue(payload, ChatRequest.class);

            // 验证消息内容
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                sendMessage(session, ChatResponse.error(session.getId(), "消息内容不能为空"));
                return;
            }

            // 获取智能体ID
            Long agentId = extractAgentId(session);

            // 发送正在输入状态（可选，用于提升用户体验）
            sendMessage(session, ChatResponse.typing(session.getId()));

            // 调用对话服务处理消息
            ChatResponse response = dialogSessionService.chat(
                agentId,
                session.getId(),
                request.getMessage()
            );

            // 发送响应给客户端
            sendMessage(session, response);

            log.debug("消息处理完成: sessionId={}, agentId={}", session.getId(), agentId);

        } catch (IOException e) {
            // JSON 解析失败
            log.error("消息解析失败: sessionId={}, payload={}", session.getId(), payload, e);
            sendMessage(session, ChatResponse.error(session.getId(), "消息格式错误"));

        } catch (Exception e) {
            // 其他处理错误
            log.error("处理消息失败: sessionId={}", session.getId(), e);
            sendMessage(session, ChatResponse.error(session.getId(), "处理消息失败: " + e.getMessage()));
        }
    }

    /**
     * 处理传输错误
     * <p>
     * 当 WebSocket 连接发生传输层错误时调用
     * </p>
     *
     * @param session   WebSocket 会话
     * @param exception 传输错误
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();

        log.error("WebSocket 传输错误: sessionId={}, error={}",
            sessionId, exception.getMessage());

        // 如果连接仍然打开，尝试发送错误通知
        if (session.isOpen()) {
            try {
                sendMessage(session, ChatResponse.error(sessionId, "连接发生错误，请重新连接"));
            } catch (IOException e) {
                log.warn("发送错误通知失败: sessionId={}", sessionId);
            }
        }

        // 从管理器中移除异常会话
        sessionManager.removeSession(sessionId);
    }

    /**
     * 连接关闭时调用
     * <p>
     * 处理流程:
     * 1. 从管理器移除会话
     * 2. 清理对话会话数据
     * </p>
     *
     * @param session WebSocket 会话
     * @param status  关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        Long agentId = extractAgentId(session);

        // 从管理器移除会话
        sessionManager.removeSession(sessionId);

        // 清理对话会话数据（聊天记忆等）
        dialogSessionService.clearSession(sessionId);

        log.info("WebSocket 连接关闭: sessionId={}, agentId={}, status={}",
            sessionId, agentId, status);
    }

    /**
     * 是否支持分片消息
     * <p>
     * 返回 false 表示不支持分片，需要等待完整消息
     * </p>
     *
     * @return false - 不支持分片消息
     */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 从 WebSocket URI 中提取智能体ID
     * <p>
     * URI 格式: /ws/agent/{agentId}
     * </p>
     *
     * @param session WebSocket 会话
     * @return 智能体ID
     * @throws IllegalArgumentException URI 格式无效时抛出
     */
    private Long extractAgentId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            throw new IllegalArgumentException("WebSocket URI 为空");
        }

        String path = uri.getPath();
        // /ws/agent/{agentId}
        String[] parts = path.split("/");

        // 验证路径格式
        if (parts.length < 3 || !"agent".equals(parts[parts.length - 2])) {
            throw new IllegalArgumentException("无效的 WebSocket 端点路径: " + path);
        }

        // 解析智能体ID
        try {
            return Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的智能体ID: " + parts[parts.length - 1]);
        }
    }

    /**
     * 发送消息到客户端
     * <p>
     * 将响应对象序列化为 JSON 并发送
     * </p>
     *
     * @param session  WebSocket 会话
     * @param response 响应对象
     * @throws IOException 发送失败时抛出
     */
    private void sendMessage(WebSocketSession session, ChatResponse response) throws IOException {
        if (!session.isOpen()) {
            log.warn("会话已关闭，无法发送消息: sessionId={}", session.getId());
            return;
        }

        String json = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(json));

        log.debug("消息已发送: sessionId={}, type={}", session.getId(), response.getType());
    }
}