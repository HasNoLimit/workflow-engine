package com.workflow.engine.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "llm_model", autoResultMap = true)
public class LlmModel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long providerId;
    private String modelId;
    private String modelName;
    private Integer maxTokens;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> capabilities;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}