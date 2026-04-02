package com.workflow.engine.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "agent", autoResultMap = true)
public class Agent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workflowId;
    private Integer workflowVersion;
    private String name;
    private String type;
    private String status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    private String apiKey;
    private String webhookUrl;
    private Integer timeoutSeconds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public static final String TYPE_DIALOG = "DIALOG";
    public static final String TYPE_API = "API";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
}