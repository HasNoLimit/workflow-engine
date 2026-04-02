package com.workflow.engine.node;

import java.util.Map;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 开始节点处理器
 * <p>
 * 工作流的入口节点，初始化执行状态。
 * 开始节点是每个工作流的起点，不做额外的业务处理。
 * </p>
 */
@Slf4j
@Component
public class StartNodeHandler implements NodeHandler {

    @Override
    public String getNodeType() {
        return "start";
    }

    @Override
    public WorkflowState execute(NodeJSON node, WorkflowState state) {
        log.info("工作流开始执行: workflowId={}, startNodeId={}, input={}",
                state.getWorkflowId(), node.id(), state.getInput());

        // 开始节点初始化执行状态
        // 将输入数据复制到变量存储，便于后续节点访问
        WorkflowState newState = state.withStatus(WorkflowState.STATUS_RUNNING);

        // 将输入参数添加到变量存储
        if (state.getInput() != null) {
            for (Map.Entry<String, Object> entry : state.getInput().entrySet()) {
                newState = newState.setVariable(entry.getKey(), entry.getValue());
            }
        }

        log.info("开始节点执行完成: nodeId={}, 状态已初始化", node.id());
        return newState;
    }

    @Override
    public String getDescription(NodeJSON node) {
        return "工作流入口节点";
    }
}