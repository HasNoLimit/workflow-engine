package com.workflow.engine.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 * <p>
 * 配置 WebSocket 端点和相关参数，支持智能体对话实时通信
 * </p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * 智能体 WebSocket 处理器
     */
    private final AgentWebSocketHandler agentWebSocketHandler;

    /**
     * 构造函数注入处理器
     *
     * @param agentWebSocketHandler 智能体对话处理器
     */
    public WebSocketConfig(AgentWebSocketHandler agentWebSocketHandler) {
        this.agentWebSocketHandler = agentWebSocketHandler;
    }

    /**
     * 注册 WebSocket 处理器
     * <p>
     * 配置智能体对话 WebSocket 端点:
     * - 端点路径: /ws/agent/{agentId}
     * - 允许所有来源跨域访问
     * </p>
     *
     * @param registry WebSocket 处理器注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册智能体对话 WebSocket 端点
        registry.addHandler(agentWebSocketHandler, "/ws/agent/*")
            .setAllowedOrigins("*");  // 允许跨域访问
    }
}