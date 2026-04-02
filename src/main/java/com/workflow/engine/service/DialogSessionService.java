package com.workflow.engine.service;

import com.workflow.engine.executor.ExecutionResult;
import com.workflow.engine.executor.WorkflowContext;
import com.workflow.engine.executor.WorkflowExecutor;
import com.workflow.engine.model.Agent;
import com.workflow.engine.websocket.dto.ChatMessage;
import com.workflow.engine.websocket.dto.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 对话会话服务
 * <p>
 * 管理对话会话和聊天记忆，处理用户与智能体的对话交互。
 * 使用 LangChain4j ChatMemory 管理对话历史。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogSessionService {

    /**
     * 智能体服务
     */
    private final AgentService agentService;

    /**
     * 工作流执行器
     */
    private final WorkflowExecutor workflowExecutor;

    /**
     * 工作流服务
     */
    private final WorkflowService workflowService;

    /**
     * 工作流版本服务
     */
    private final WorkflowVersionService workflowVersionService;

    /**
     * 会话ID -> 聊天记忆映射
     * <p>
     * 使用 ConcurrentHashMap 保证并发安全
     * </p>
     */
    private final Map<String, ChatMemory> chatMemories = new ConcurrentHashMap<>();

    /**
     * 最大记忆窗口大小
     * <p>
     * 保留最近 20 条消息，避免内存占用过大
     * </p>
     */
    private static final int MAX_MEMORY_SIZE = 20;

    /**
     * 处理聊天消息
     * <p>
     * 处理流程:
     * 1. 获取智能体信息
     * 2. 获取或创建聊天记忆
     * 3. 添加用户消息到记忆
     * 4. 执行工作流处理消息
     * 5. 添加 AI 响应到记忆
     * 6. 返回响应结果
     * </p>
     *
     * @param agentId   智能体ID
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return 聊天响应
     */
    public ChatResponse chat(Long agentId, String sessionId, String message) {
        log.info("处理聊天消息: agentId={}, sessionId={}, messageLength={}",
            agentId, sessionId, message.length());

        try {
            // 1. 获取智能体信息
            Agent agent = agentService.getAgentById(agentId);
            if (agent == null) {
                log.warn("智能体不存在: agentId={}", agentId);
                return ChatResponse.error(sessionId, "智能体不存在");
            }

            // 2. 验证智能体状态
            if (!Agent.STATUS_ACTIVE.equals(agent.getStatus())) {
                log.warn("智能体未激活: agentId={}, status={}", agentId, agent.getStatus());
                return ChatResponse.error(sessionId, "智能体未激活，请先发布智能体");
            }

            // 3. 获取或创建聊天记忆
            ChatMemory chatMemory = getOrCreateChatMemory(sessionId);

            // 4. 添加用户消息到记忆
            chatMemory.add(UserMessage.from(message));
            log.debug("用户消息已添加到记忆: sessionId={}, messagesCount={}",
                sessionId, chatMemory.messages().size());

            // 5. 执行工作流处理消息
            String response = processWithWorkflow(agent, message, chatMemory);

            // 6. 添加 AI 响应到记忆
            chatMemory.add(AiMessage.from(response));
            log.debug("AI 响应已添加到记忆: sessionId={}, messagesCount={}",
                sessionId, chatMemory.messages().size());

            // 7. 返回成功响应
            return ChatResponse.message(sessionId, agentId, response);

        } catch (Exception e) {
            log.error("处理聊天消息失败: agentId={}, sessionId={}", agentId, sessionId, e);
            return ChatResponse.error(sessionId, "处理消息失败: " + e.getMessage());
        }
    }

    /**
     * 清理对话会话
     * <p>
     * 移除会话的聊天记忆，释放资源
     * </p>
     *
     * @param sessionId 会话ID
     */
    public void clearSession(String sessionId) {
        ChatMemory removed = chatMemories.remove(sessionId);
        if (removed != null) {
            log.info("会话已清理: sessionId={}, removedMessagesCount={}",
                sessionId, removed.messages().size());
        } else {
            log.debug("会话不存在或已清理: sessionId={}", sessionId);
        }
    }

    /**
     * 获取会话的聊天历史
     * <p>
     * 返回当前会话的所有聊天消息记录
     * </p>
     *
     * @param sessionId 会话ID
     * @return 聊天历史列表
     */
    public List<ChatMessage> getHistory(String sessionId) {
        ChatMemory chatMemory = chatMemories.get(sessionId);
        if (chatMemory == null) {
            return List.of();
        }

        return chatMemory.messages().stream()
            .map(msg -> {
                if (msg instanceof UserMessage userMsg) {
                    return ChatMessage.user(userMsg.singleText());
                } else if (msg instanceof AiMessage aiMsg) {
                    return ChatMessage.assistant(aiMsg.text());
                } else {
                    return ChatMessage.system(msg.toString());
                }
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取会话消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    public int getMessageCount(String sessionId) {
        ChatMemory chatMemory = chatMemories.get(sessionId);
        return chatMemory != null ? chatMemory.messages().size() : 0;
    }

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean hasSession(String sessionId) {
        return chatMemories.containsKey(sessionId);
    }

    /**
     * 获取活跃会话数量
     *
     * @return 当前活跃会话数
     */
    public int getActiveSessionCount() {
        return chatMemories.size();
    }

    /**
     * 获取或创建聊天记忆
     * <p>
     * 如果会话不存在则创建新的聊天记忆，使用滑动窗口保留最近消息
     * </p>
     *
     * @param sessionId 会话ID
     * @return 聊天记忆实例
     */
    private ChatMemory getOrCreateChatMemory(String sessionId) {
        return chatMemories.computeIfAbsent(sessionId,
            id -> MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_SIZE));
    }

    /**
     * 使用工作流处理消息
     * <p>
     * 执行智能体关联的工作流，生成响应内容。
     * 当前为简化实现，后续将集成 LangChain4j AI Service。
     * </p>
     *
     * @param agent      智能体
     * @param message    用户消息
     * @param chatMemory 聊天记忆
     * @return 响应内容
     */
    private String processWithWorkflow(Agent agent, String message, ChatMemory chatMemory) {
        log.debug("执行工作流处理: agentId={}, workflowId={}, version={}",
            agent.getId(), agent.getWorkflowId(), agent.getWorkflowVersion());

        try {
            // 获取工作流
            var workflow = workflowService.getById(agent.getWorkflowId());
            if (workflow == null) {
                log.warn("工作流不存在: workflowId={}", agent.getWorkflowId());
                return getDefaultResponse(agent, message);
            }

            // 获取工作流版本定义（画布 JSON）
            var version = workflowVersionService.getVersion(
                agent.getWorkflowId(),
                agent.getWorkflowVersion()
            );

            if (version == null) {
                log.warn("工作流版本不存在: workflowId={}, version={}",
                    agent.getWorkflowId(), agent.getWorkflowVersion());
                return getDefaultResponse(agent, message);
            }

            // 构建执行上下文
            WorkflowContext context = WorkflowContext.builder()
                .workflowId(agent.getWorkflowId())
                .canvas(version.getCanvas())
                .input(Map.of("message", message, "agentId", agent.getId()))
                .build();

            // 执行工作流
            ExecutionResult result = workflowExecutor.execute(context);

            if (result.isSuccess() && result.getOutput() != null) {
                // 提取输出内容
                Object output = result.getOutput().get("output");
                if (output != null) {
                    return output.toString();
                }
            }

            // 执行失败或无输出，返回默认响应
            log.warn("工作流执行未产生有效输出: agentId={}, success={}",
                agent.getId(), result.isSuccess());
            return getDefaultResponse(agent, message);

        } catch (Exception e) {
            log.error("工作流执行失败: agentId={}", agent.getId(), e);
            return getDefaultResponse(agent, message);
        }
    }

    /**
     * 获取默认响应
     * <p>
     * 当工作流执行失败或无法获取时，返回默认的响应内容
     * </p>
     *
     * @param agent  智能体
     * @param message 用户消息
     * @return 默认响应
     */
    private String getDefaultResponse(Agent agent, String message) {
        // 当前为简化实现，返回固定响应
        // TODO: 后续集成 LangChain4j AI Service，使用 LLM 生成响应
        return "您好，我是智能体 " + agent.getName() + "。" +
            "收到您的消息: \"" + message + "\"。" +
            "我正在学习中，暂时无法提供完整服务。请稍后再试。";
    }
}