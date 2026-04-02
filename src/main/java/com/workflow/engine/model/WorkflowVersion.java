package com.workflow.engine.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.workflow.engine.engine.model.WorkflowJSON;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "workflow_version", autoResultMap = true)
public class WorkflowVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workflowId;
    private Integer version;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private WorkflowJSON canvas;

    private String changeNote;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}