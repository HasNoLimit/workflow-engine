package com.workflow.engine.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "workflow_checkpoint", autoResultMap = true)
public class WorkflowCheckpoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String threadId;
    private String checkpointId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> state;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}