package com.workflow.engine.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "debug_execution", autoResultMap = true)
public class DebugExecution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workflowId;
    private Integer workflowVersion;
    private String mode;
    private String targetNodeId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> input;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> output;

    private String status;
    private Long durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public static final String MODE_SINGLE_NODE = "SINGLE_NODE";
    public static final String MODE_FULL = "FULL";
}