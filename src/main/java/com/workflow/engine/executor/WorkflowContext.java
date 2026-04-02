package com.workflow.engine.executor;

import com.workflow.engine.engine.model.WorkflowJSON;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流执行上下文
 * <p>
 * 包含工作流执行过程中的上下文信息，如画布定义、执行配置等
 * </p>
 */
@Data
@Builder
public class WorkflowContext {

    /** 工作流ID */
    private Long workflowId;

    /** 工作流版本ID */
    private Long versionId;

    /** 执行ID */
    private Long executionId;

    /** 智能体ID（如果是由智能体触发） */
    private Long agentId;

    /** 画布定义（FlowGram.AI JSON） */
    private WorkflowJSON canvas;

    /** 输入参数 */
    private Map<String, Object> input;

    /** 执行模式：SYNC（同步）, ASYNC（异步） */
    @Builder.Default
    private String executionMode = "SYNC";

    /** 是否启用调试模式 */
    @Builder.Default
    private boolean debugMode = false;

    /** 是否启用检查点 */
    @Builder.Default
    private boolean checkpointEnabled = true;

    /** 线程ID（用于状态恢复） */
    private String threadId;

    /** 执行开始时间 */
    private LocalDateTime startedAt;

    /** 执行结束时间 */
    private LocalDateTime completedAt;

    /**
     * 创建线程ID
     * @return 线程ID
     */
    public String getOrCreateThreadId() {
        if (threadId == null || threadId.isEmpty()) {
            threadId = "thread-" + workflowId + "-" + System.currentTimeMillis();
        }
        return threadId;
    }

    /**
     * 计算执行时长（毫秒）
     * @return 执行时长，如果未结束返回 null
     */
    public Long calculateDuration() {
        if (startedAt == null || completedAt == null) {
            return null;
        }
        return java.time.Duration.between(startedAt, completedAt).toMillis();
    }
}