package com.workflow.engine.node;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;
import com.workflow.engine.exception.WorkflowExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 条件节点处理器
 * <p>
 * 根据条件表达式决定工作流的执行路径。
 * 支持基于变量值的条件判断，返回下一个节点的标识。
 * </p>
 */
@Slf4j
@Component
public class ConditionNodeHandler implements NodeHandler {

    @Override
    public String getNodeType() {
        return "condition";
    }

    @Override
    public WorkflowState execute(NodeJSON node, WorkflowState state) {
        log.info("执行条件节点: nodeId={}, workflowId={}", node.id(), state.getWorkflowId());

        // 获取条件配置
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> conditions = node.data() != null
                ? (java.util.List<Map<String, Object>>) node.data().get("conditions")
                : new java.util.ArrayList<>();

        if (conditions.isEmpty()) {
            throw new WorkflowExecutionException("条件节点缺少条件配置: " + node.id());
        }

        // 评估条件
        String matchedBranch = evaluateConditions(conditions, state);

        log.info("条件节点评估结果: nodeId={}, matchedBranch={}", node.id(), matchedBranch);

        // 将匹配的分支存储到状态中，供后续路由使用
        state = state.setVariable("condition_branch_" + node.id(), matchedBranch);

        log.info("条件节点执行完成: nodeId={}, 分支={}", node.id(), matchedBranch);
        return state;
    }

    /**
     * 评估条件列表，返回匹配的分支标识
     * @param conditions 条件配置列表
     * @param state 当前状态
     * @return 匹配的分支标识
     */
    private String evaluateConditions(java.util.List<Map<String, Object>> conditions, WorkflowState state) {
        for (Map<String, Object> condition : conditions) {
            String expression = (String) condition.get("expression");
            String branch = (String) condition.get("branch");

            if (expression == null || branch == null) {
                continue;
            }

            // 解析并评估表达式
            boolean result = evaluateExpression(expression, state);

            if (result) {
                return branch;
            }
        }

        // 返回默认分支
        return "default";
    }

    /**
     * 评估单个表达式
     * @param expression 表达式（如 "$variable == 'value'"）
     * @param state 当前状态
     * @return 表达式结果
     */
    private boolean evaluateExpression(String expression, WorkflowState state) {
        // TODO: 实现更完善的表达式解析器
        // 当前为简化实现，支持基本的变量比较

        if (expression.startsWith("$")) {
            // 变量引用表达式
            String[] parts = expression.split("==");
            if (parts.length == 2) {
                String varName = parts[0].trim().substring(1); // 移除 $ 前缀
                String expectedValue = parts[1].trim().replace("'", "").replace("\"", "");

                Object actualValue = state.getVariable(varName);
                if (actualValue != null) {
                    return actualValue.toString().equals(expectedValue);
                }
            }
        }

        // 默认返回 false
        return false;
    }

    /**
     * 获取条件节点应该路由到的下一个节点
     * @param node 节点配置
     * @param state 当前状态
     * @return 下一个节点的端口标识
     */
    public String getRoutingPort(NodeJSON node, WorkflowState state) {
        return state.getVariable("condition_branch_" + node.id());
    }

    @Override
    public boolean validate(NodeJSON node) {
        // 条件节点必须有条件配置
        return node.data() != null && node.data().containsKey("conditions");
    }

    @Override
    public String getDescription(NodeJSON node) {
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> conditions = node.data() != null
                ? (java.util.List<Map<String, Object>>) node.data().get("conditions")
                : new java.util.ArrayList<>();
        return "条件节点 (分支数: " + conditions.size() + ")";
    }
}