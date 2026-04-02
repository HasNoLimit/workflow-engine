package com.workflow.engine.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "node_execution_log", autoResultMap = true)
public class NodeExecutionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long executionId;
    private String nodeId;
    private String nodeType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> input;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> output;

    private String status;
    private Long durationMs;
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime executedAt;
}