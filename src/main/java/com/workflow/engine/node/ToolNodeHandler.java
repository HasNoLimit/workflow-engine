package com.workflow.engine.node;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;
import com.workflow.engine.exception.WorkflowExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工具节点处理器
 * <p>
 * 执行外部工具或 API 调用。
 * 支持自定义工具 ID 和参数配置。
 * </p>
 */
@Slf4j
@Component
public class ToolNodeHandler implements NodeHandler {

    @Override
    public String getNodeType() {
        return "tool";
    }

    @Override
    public WorkflowState execute(NodeJSON node, WorkflowState state) {
        log.info("执行工具节点: nodeId={}, workflowId={}", node.id(), state.getWorkflowId());

        // 获取节点配置
        String toolId = node.getToolId();

        // 验证必要配置
        if (toolId == null || toolId.isEmpty()) {
            throw new WorkflowExecutionException("工具节点缺少工具配置: " + node.id());
        }

        // 获取工具参数配置
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> toolParams = node.data() != null
                ? (java.util.Map<String, Object>) node.data().get("params")
                : new java.util.HashMap<>();

        // TODO: 集成 LangChain4j Tool 执行
        // 当前为占位实现，后续需要:
        // 1. 从数据库获取工具配置
        // 2. 创建 ToolCallback 实例
        // 3. 构建工具调用请求
        // 4. 执行工具调用
        // 5. 处理响应

        log.info("工具节点配置: toolId={}, params={}", toolId, toolParams);

        // 模拟工具响应（占位）
        Object result = "工具执行结果占位 - 需要集成工具系统";
        if (toolParams != null) {
            result = java.util.Map.of("toolId", toolId, "params", toolParams, "status", "placeholder");
        }

        // 将结果存储到变量
        String outputKey = "tool_output_" + node.id();
        state = state.setVariable(outputKey, result);

        log.info("工具节点执行完成: nodeId={}, 输出已存储到 {}", node.id(), outputKey);
        return state;
    }

    @Override
    public boolean validate(NodeJSON node) {
        // 工具节点必须有工具 ID
        String toolId = node.getToolId();
        return toolId != null && !toolId.isEmpty();
    }

    @Override
    public String getDescription(NodeJSON node) {
        String toolId = node.getToolId();
        return "工具节点 (工具: " + (toolId != null ? toolId : "未配置") + ")";
    }
}