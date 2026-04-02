package com.workflow.engine.node;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;
import com.workflow.engine.exception.WorkflowExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM 节点处理器
 * <p>
 * 调用大语言模型进行文本生成或对话。
 * 支持自定义系统提示词、用户提示词和模型配置。
 * </p>
 */
@Slf4j
@Component
public class LLMNodeHandler implements NodeHandler {

    @Override
    public String getNodeType() {
        return "llm";
    }

    @Override
    public WorkflowState execute(NodeJSON node, WorkflowState state) {
        log.info("执行 LLM 节点: nodeId={}, workflowId={}", node.id(), state.getWorkflowId());

        // 获取节点配置
        String modelId = node.getModelId();
        String systemPrompt = node.getSystemPrompt();
        String userPrompt = node.getUserPrompt();

        // 验证必要配置
        if (modelId == null || modelId.isEmpty()) {
            throw new WorkflowExecutionException("LLM 节点缺少模型配置: " + node.id());
        }

        // TODO: 集成 LangChain4j 调用模型
        // 当前为占位实现，后续需要:
        // 1. 从数据库获取模型配置
        // 2. 创建 ChatLanguageModel 实例
        // 3. 构建对话请求
        // 4. 执行模型调用
        // 5. 处理响应

        log.info("LLM 节点配置: modelId={}, systemPrompt={}, userPrompt={}",
                modelId, systemPrompt, userPrompt);

        // 将用户提示词添加到对话历史
        if (userPrompt != null) {
            state = state.addChatMessage("user", userPrompt);
        }

        // 模拟 LLM 响应（占位）
        String response = "LLM 响应占位 - 需要集成 LangChain4j";
        state = state.addChatMessage("assistant", response);

        // 将响应存储到变量
        String outputKey = "llm_output_" + node.id();
        state = state.setVariable(outputKey, response);

        log.info("LLM 节点执行完成: nodeId={}, 输出已存储到 {}", node.id(), outputKey);
        return state;
    }

    @Override
    public boolean validate(NodeJSON node) {
        // LLM 节点必须有模型配置
        String modelId = node.getModelId();
        return modelId != null && !modelId.isEmpty();
    }

    @Override
    public String getDescription(NodeJSON node) {
        String modelId = node.getModelId();
        return "LLM 节点 (模型: " + (modelId != null ? modelId : "未配置") + ")";
    }
}