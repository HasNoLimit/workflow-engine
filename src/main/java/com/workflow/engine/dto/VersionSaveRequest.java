package com.workflow.engine.dto;

import com.workflow.engine.engine.model.WorkflowJSON;
import jakarta.validation.constraints.NotNull;

/**
 * 保存工作流版本请求
 * <p>
 * 用于保存工作流画布数据的新版本
 * </p>
 *
 * @param canvas     画布 JSON 数据（FlowGram.AI 格式）
 * @param changeNote 变更说明
 */
public record VersionSaveRequest(
    /** 画布 JSON 数据（FlowGram.AI 格式） */
    @NotNull(message = "画布数据不能为空")
    WorkflowJSON canvas,

    /** 变更说明 */
    String changeNote
) {}