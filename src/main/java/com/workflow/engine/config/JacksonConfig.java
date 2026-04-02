package com.workflow.engine.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 配置类
 * <p>
 * 配置 JSON 序列化和反序列化行为
 * </p>
 */
@Configuration
public class JacksonConfig {

    /**
     * 创建并配置 ObjectMapper
     * <p>
     * 配置项：
     * - 注册 JavaTimeModule 处理日期时间类型
     * - 禁用日期时间作为时间戳序列化
     * - 忽略未知属性（用于处理 record 类型的 getter 方法）
     * </p>
     *
     * @return 配置好的 ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略未知属性，解决 record 类型 getter 方法导致的序列化/反序列化问题
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}