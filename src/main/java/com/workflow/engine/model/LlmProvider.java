package com.workflow.engine.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "llm_provider", autoResultMap = true)
public class LlmProvider {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String providerType;
    private String apiKey;
    private String baseUrl;
    private Integer timeoutSeconds;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> options;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}