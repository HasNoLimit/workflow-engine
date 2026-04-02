package com.workflow.engine.node;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 结束节点处理器
 * <p>
 * 工作流的终止节点，标记执行完成。
 * 结束节点将最终输出数据整理并标记状态为成功。
 * </p>
 */
@Slf4j
@Component
public class EndNodeHandler implements NodeHandler {

    @Override
    public String getNodeType() {
        return "end";
    }

    @Override
    public WorkflowState execute(NodeJSON node, WorkflowState state) {
        log.info("工作流执行完成: workflowId={}, endNodeId={}, executionPath={}",
                state.getWorkflowId(), node.id(), state.getExecutionPath());

        // 结束节点将当前输出作为最终输出
        // 标记状态为成功
        WorkflowState finalState = state.withStatus(WorkflowState.STATUS_SUCCESS);

        // 如果节点配置中指定了输出字段，从变量中提取
        if (node.data() != null && node.data().containsKey("outputFields")) {
            @SuppressWarnings("unchecked")
            java.util.List<String> outputFields = (java.util.List<String>) node.data().get("outputFields");
            java.util.Map<String, Object> finalOutput = new java.util.HashMap<>();

            for (String field : outputFields) {
                Object value = finalState.getVariable(field);
                if (value != null) {
                    finalOutput.put(field, value);
                }
            }

            finalState = finalState.withOutput(finalOutput);
        }

        log.info("结束节点执行完成: nodeId={}, 输出数据已整理", node.id());
        return finalState;
    }

    @Override
    public String getDescription(NodeJSON node) {
        return "工作流终止节点";
    }
}