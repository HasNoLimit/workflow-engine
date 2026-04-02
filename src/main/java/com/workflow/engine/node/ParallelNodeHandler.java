package com.workflow.engine.node;

import com.workflow.engine.engine.model.NodeJSON;
import com.workflow.engine.executor.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 并行节点处理器
 * <p>
 * 并行执行多个分支，收集所有分支结果后继续。
 * 支持配置并行分支数量和超时时间。
 * </p>
 */
@Slf4j
@Component
public class ParallelNodeHandler implements NodeHandler {

    @Override
    public String getNodeType() {
        return "parallel";
    }

    @Override
    public WorkflowState execute(NodeJSON node, WorkflowState state) {
        log.info("执行并行节点: nodeId={}, workflowId={}", node.id(), state.getWorkflowId());

        // 获取并行分支配置
        @SuppressWarnings("unchecked")
        List<String> branches = node.data() != null
                ? (List<String>) node.data().get("branches")
                : new ArrayList<>();

        if (branches.isEmpty()) {
            log.warn("并行节点没有配置分支，直接跳过: nodeId={}", node.id());
            return state;
        }

        // 获取超时配置（毫秒）
        Integer timeoutMs = node.data() != null
                ? (Integer) node.data().get("timeoutMs")
                : 30000; // 默认 30 秒

        log.info("并行节点配置: branches={}, timeoutMs={}", branches.size(), timeoutMs);

        // TODO: 实现真正的并行执行
        // 当前为占位实现，后续需要:
        // 1. 使用 CompletableFuture 或 Virtual Threads 并行执行
        // 2. 每个分支独立执行
        // 3. 收集所有分支结果
        // 4. 处理超时和异常

        // 模拟并行执行结果
        Map<String, Object> parallelResults = new ConcurrentHashMap<>();
        for (String branch : branches) {
            parallelResults.put(branch, "分支 " + branch + " 执行结果占位");
        }

        // 将并行结果存储到变量
        String outputKey = "parallel_output_" + node.id();
        state = state.setVariable(outputKey, parallelResults);

        log.info("并行节点执行完成: nodeId={}, 分支数={}, 输出已存储到 {}",
                node.id(), branches.size(), outputKey);
        return state;
    }

    /**
     * 并行执行多个任务并收集结果
     * @param tasks 任务列表
     * @param timeoutMs 超时时间（毫秒）
     * @return 任务结果映射
     */
    private Map<String, Object> executeParallel(
            List<ParallelTask> tasks,
            int timeoutMs) {

        Map<String, Object> results = new ConcurrentHashMap<>();

        // 使用 CompletableFuture 并行执行
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ParallelTask task : tasks) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Object result = task.execute();
                    results.put(task.getId(), result);
                } catch (Exception e) {
                    results.put(task.getId(), Map.of("error", e.getMessage()));
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("并行执行超时: timeoutMs={}", timeoutMs);
            // 取消未完成的任务
            futures.forEach(f -> f.cancel(true));
        } catch (Exception e) {
            log.error("并行执行异常", e);
        }

        return results;
    }

    @Override
    public boolean validate(NodeJSON node) {
        // 并行节点必须有分支配置
        return node.data() != null && node.data().containsKey("branches");
    }

    @Override
    public String getDescription(NodeJSON node) {
        @SuppressWarnings("unchecked")
        List<String> branches = node.data() != null
                ? (List<String>) node.data().get("branches")
                : new ArrayList<>();
        return "并行节点 (分支数: " + branches.size() + ")";
    }

    /**
     * 并行任务接口
     */
    public interface ParallelTask {
        String getId();
        Object execute();
    }
}