package com.workflow.engine.executor;

import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import com.workflow.engine.mapper.WorkflowCheckpointMapper;
import com.workflow.engine.model.WorkflowCheckpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 工作流检查点服务
 * <p>
 * 结合 LangGraph4j MemorySaver 和数据库持久化，
 * 支持工作流执行状态恢复和暂停/继续功能。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowCheckpointService {

    /** 内存检查点保存器（LangGraph4j 内置） */
    private final MemorySaver memorySaver = new MemorySaver();

    /** 检查点 Mapper（数据库持久化） */
    private final WorkflowCheckpointMapper checkpointMapper;

    /**
     * 获取内存检查点保存器
     * <p>
     * 用于 LangGraph4j CompileConfig 配置
     * </p>
     * @return MemorySaver 实例
     */
    public MemorySaver getMemorySaver() {
        return memorySaver;
    }

    /**
     * 保存检查点到数据库
     * <p>
     * 将执行状态持久化到数据库，用于长期保存和恢复
     * </p>
     * @param threadId 线程ID（用于标识执行会话）
     * @param state 工作流状态
     */
    public void saveToDatabase(String threadId, WorkflowAgentState state) {
        log.info("保存检查点到数据库: threadId={}", threadId);

        try {
            // 序列化状态数据
            Map<String, Object> stateData = new HashMap<>(state.data());

            // 查询已有检查点
            WorkflowCheckpoint existing = checkpointMapper.findLatestByThreadId(threadId);

            if (existing != null) {
                // 更新已有检查点
                existing.setState(stateData);
                existing.setCreatedAt(LocalDateTime.now());
                checkpointMapper.updateById(existing);
                log.info("检查点更新成功: threadId={}", threadId);
            } else {
                // 创建新的检查点
                WorkflowCheckpoint entity = new WorkflowCheckpoint();
                entity.setThreadId(threadId);
                entity.setCheckpointId("checkpoint-" + System.currentTimeMillis());
                entity.setState(stateData);
                entity.setCreatedAt(LocalDateTime.now());
                checkpointMapper.insert(entity);
                log.info("检查点保存成功: threadId={}", threadId);
            }

        } catch (Exception e) {
            log.error("检查点保存失败: threadId={}, error={}", threadId, e.getMessage(), e);
        }
    }

    /**
     * 从数据库加载检查点
     * <p>
     * 根据线程ID从数据库加载最新的检查点状态
     * </p>
     * @param threadId 线程ID
     * @return 工作流状态，如果不存在返回 Optional.empty()
     */
    public Optional<WorkflowAgentState> loadFromDatabase(String threadId) {
        log.info("从数据库加载检查点: threadId={}", threadId);

        try {
            WorkflowCheckpoint entity = checkpointMapper.findLatestByThreadId(threadId);

            if (entity == null) {
                log.info("检查点不存在: threadId={}", threadId);
                return Optional.empty();
            }

            Map<String, Object> stateData = entity.getState();
            if (stateData == null || stateData.isEmpty()) {
                return Optional.empty();
            }

            WorkflowAgentState state = new WorkflowAgentState(stateData);
            log.info("检查点加载成功: threadId={}", threadId);
            return Optional.of(state);

        } catch (Exception e) {
            log.error("检查点加载失败: threadId={}, error={}", threadId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 删除检查点
     * <p>
     * 工作流执行完成后清理检查点数据
     * </p>
     * @param threadId 线程ID
     */
    public void deleteByThreadId(String threadId) {
        log.info("删除检查点: threadId={}", threadId);
        try {
            java.util.List<WorkflowCheckpoint> checkpoints = checkpointMapper.findByThreadId(threadId);
            for (WorkflowCheckpoint checkpoint : checkpoints) {
                checkpointMapper.deleteById(checkpoint.getId());
            }
            log.info("检查点删除成功: threadId={}, count={}", threadId, checkpoints.size());
        } catch (Exception e) {
            log.error("检查点删除失败: threadId={}, error={}", threadId, e.getMessage(), e);
        }
    }
}