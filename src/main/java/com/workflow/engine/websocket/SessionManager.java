package com.workflow.engine.websocket;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器
 * <p>
 * 管理所有活跃的 WebSocket 连接，支持按会话ID和智能体ID查询。
 * 使用 ConcurrentHashMap 保证并发安全。
 * </p>
 */
@Slf4j
@Component
public class SessionManager {

    /**
     * 会话ID -> 会话信息映射
     * <p>
     * 使用 ConcurrentHashMap 保证并发访问安全
     * </p>
     */
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    /**
     * 注册会话
     * <p>
     * 将新建立的 WebSocket 会话添加到管理器中
     * </p>
     *
     * @param sessionId 会话ID
     * @param session   WebSocket 会话对象
     * @param agentId   智能体ID
     */
    public void registerSession(String sessionId, WebSocketSession session, Long agentId) {
        SessionInfo info = new SessionInfo();
        info.setSession(session);
        info.setAgentId(agentId);
        info.setConnectedAt(System.currentTimeMillis());

        sessions.put(sessionId, info);
        log.debug("会话已注册: sessionId={}, agentId={}, 当前活跃会话数={}",
            sessionId, agentId, sessions.size());
    }

    /**
     * 移除会话
     * <p>
     * 从管理器中移除已关闭的会话
     * </p>
     *
     * @param sessionId 会话ID
     */
    public void removeSession(String sessionId) {
        SessionInfo removed = sessions.remove(sessionId);
        if (removed != null) {
            log.debug("会话已移除: sessionId={}, agentId={}, 当前活跃会话数={}",
                sessionId, removed.getAgentId(), sessions.size());
        }
    }

    /**
     * 获取 WebSocket 会话
     *
     * @param sessionId 会话ID
     * @return WebSocket 会话对象，不存在时返回 null
     */
    public WebSocketSession getSession(String sessionId) {
        SessionInfo info = sessions.get(sessionId);
        return info != null ? info.getSession() : null;
    }

    /**
     * 获取智能体ID
     *
     * @param sessionId 会话ID
     * @return 智能体ID，不存在时返回 null
     */
    public Long getAgentId(String sessionId) {
        SessionInfo info = sessions.get(sessionId);
        return info != null ? info.getAgentId() : null;
    }

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    /**
     * 获取活跃会话数量
     *
     * @return 当前活跃会话数
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 获取会话连接时间
     *
     * @param sessionId 会话ID
     * @return 连接时间戳（毫秒），不存在时返回 -1
     */
    public long getConnectedAt(String sessionId) {
        SessionInfo info = sessions.get(sessionId);
        return info != null ? info.getConnectedAt() : -1;
    }

    /**
     * 关闭指定会话
     * <p>
     * 主动关闭指定会话，用于清理异常连接或超时连接
     * </p>
     *
     * @param sessionId 会话ID
     * @param reason    关闭原因
     */
    public void closeSession(String sessionId, String reason) {
        SessionInfo info = sessions.remove(sessionId);
        if (info != null) {
            try {
                info.getSession().close(
                    new org.springframework.web.socket.CloseStatus(
                        org.springframework.web.socket.CloseStatus.GOING_AWAY.getCode(),
                        reason
                    )
                );
                log.info("会话已关闭: sessionId={}, reason={}", sessionId, reason);
            } catch (IOException e) {
                log.warn("关闭会话失败: sessionId={}", sessionId, e);
            }
        }
    }

    /**
     * 关闭所有会话
     * <p>
     * 用于应用关闭时清理所有活跃连接
     * </p>
     */
    public void closeAllSessions() {
        log.info("关闭所有 WebSocket 会话，当前活跃数: {}", sessions.size());

        sessions.forEach((sessionId, info) -> {
            try {
                info.getSession().close();
                log.debug("会话已关闭: sessionId={}", sessionId);
            } catch (IOException e) {
                log.warn("关闭会话失败: sessionId={}", sessionId, e);
            }
        });

        sessions.clear();
        log.info("所有 WebSocket 会话已清理");
    }

    /**
     * 会话信息
     * <p>
     * 存储单个 WebSocket 会话的详细信息
     * </p>
     */
    @Data
    public static class SessionInfo {
        /**
         * WebSocket 会话对象
         */
        private WebSocketSession session;

        /**
         * 关联的智能体ID
         */
        private Long agentId;

        /**
         * 连接建立时间（毫秒）
         */
        private long connectedAt;
    }
}