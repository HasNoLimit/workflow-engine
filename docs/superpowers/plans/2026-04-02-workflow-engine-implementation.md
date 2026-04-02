# 工作流画布引擎 - 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个完整的工作流画布引擎，支持 FlowGram.AI JSON 格式，实现智能体创建、发布和执行。

**Architecture:** 采用分层架构，使用 LangGraph4j 作为工作流编排引擎，MyBatis-Plus 进行数据交互，Spring Boot 3.3 作为主框架。充分利用 LangGraph4j 的 Hook 和 Checkpoint 特性。

**Tech Stack:** Java 17, Spring Boot 3.3.0, MyBatis-Plus 3.5.7, LangChain4j 1.12.2, LangGraph4j 1.6.2, PostgreSQL

---

## 文件结构

```
src/main/java/com/workflow/engine/
├── WorkflowEngineApplication.java
├── config/
│   ├── MybatisPlusConfig.java
│   ├── JacksonConfig.java
│   └── AsyncConfig.java
├── model/
│   ├── Workflow.java
│   ├── WorkflowVersion.java
│   ├── WorkflowCheckpoint.java
│   ├── Agent.java
│   ├── WorkflowExecution.java
│   ├── NodeExecutionLog.java
│   ├── DebugExecution.java
│   ├── DebugNodeLog.java
│   ├── LlmProvider.java
│   └── LlmModel.java
├── mapper/
│   ├── WorkflowMapper.java
│   ├── WorkflowVersionMapper.java
│   ├── WorkflowCheckpointMapper.java
│   ├── AgentMapper.java
│   ├── WorkflowExecutionMapper.java
│   ├── NodeExecutionLogMapper.java
│   ├── DebugExecutionMapper.java
│   ├── DebugNodeLogMapper.java
│   ├── LlmProviderMapper.java
│   └── LlmModelMapper.java
├── service/
│   ├── WorkflowService.java
│   ├── WorkflowVersionService.java
│   ├── AgentService.java
│   └── ...
├── controller/
│   └── ...
├── engine/
│   ├── model/
│   │   ├── WorkflowJSON.java
│   │   ├── NodeJSON.java
│   │   ├── EdgeJSON.java
│   │   └── ...
│   ├── node/
│   │   ├── NodeHandler.java
│   │   └── ...
│   └── ...
├── dto/
│   └── ...
└── exception/
    └── ...

src/main/resources/
├── application.yml
├── tools.yml
├── llm-providers.yml
└── db/migration/
    ├── V1__init_schema.sql
    └── ...
```

---

## 阶段一：项目初始化

### Task 1: 创建 Spring Boot 项目结构

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/workflow/engine/WorkflowEngineApplication.java`
- Create: `src/main/resources/application.yml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.workflow</groupId>
    <artifactId>workflow-engine</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>workflow-engine</name>
    <description>Workflow Canvas Engine</description>
    
    <properties>
        <java.version>17</java.version>
        <langchain4j.version>1.12.2</langchain4j.version>
        <langgraph4j.version>1.6.2</langgraph4j.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>dev.langchain4j</groupId>
                <artifactId>langchain4j-bom</artifactId>
                <version>${langchain4j.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.bsc.langgraph4j</groupId>
                <artifactId>langgraph4j-bom</artifactId>
                <version>${langgraph4j.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        
        <!-- LangChain4j -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
        </dependency>
        
        <!-- LangGraph4j -->
        <dependency>
            <groupId>org.bsc.langgraph4j</groupId>
            <artifactId>langgraph4j-core-jdk8</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        
        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-yaml</artifactId>
        </dependency>
        
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 Application 主类**

```java
package com.workflow.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WorkflowEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowEngineApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: workflow-engine
  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_engine
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration
  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

logging:
  level:
    com.workflow.engine: DEBUG
    org.bsc.langgraph4j: DEBUG
```

- [ ] **Step 4: 创建测试配置 application-test.yml**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  flyway:
    enabled: false

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

- [ ] **Step 5: 提交代码**

```bash
git add pom.xml src/main/java/com/workflow/engine/WorkflowEngineApplication.java src/main/resources/application.yml
git commit -m "feat: initialize Spring Boot project with dependencies

- Add Spring Boot 3.3.0 parent
- Add MyBatis-Plus 3.5.7
- Add LangChain4j 1.12.2
- Add LangGraph4j 1.6.2
- Add PostgreSQL and Flyway
- Add WebSocket support

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 创建数据库迁移脚本

**Files:**
- Create: `src/main/resources/db/migration/V1__init_schema.sql`

- [ ] **Step 1: 创建初始化数据库迁移脚本**

```sql
-- V1__init_schema.sql

-- 工作流定义
CREATE TABLE workflow (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    version INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE workflow IS '工作流定义';
COMMENT ON COLUMN workflow.id IS '主键ID';
COMMENT ON COLUMN workflow.name IS '工作流名称';
COMMENT ON COLUMN workflow.description IS '工作流描述';
COMMENT ON COLUMN workflow.version IS '当前版本号';
COMMENT ON COLUMN workflow.status IS '状态: DRAFT, PUBLISHED, ARCHIVED';

-- 工作流版本快照
CREATE TABLE workflow_version (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL REFERENCES workflow(id) ON DELETE CASCADE,
    version INT NOT NULL,
    canvas JSONB NOT NULL,
    change_note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_workflow_version ON workflow_version(workflow_id, version);

COMMENT ON TABLE workflow_version IS '工作流版本快照';
COMMENT ON COLUMN workflow_version.canvas IS 'FlowGram.AI JSON 数据';

-- 工作流检查点（用于 LangGraph4j Checkpoint）
CREATE TABLE workflow_checkpoint (
    id BIGSERIAL PRIMARY KEY,
    thread_id VARCHAR(100) NOT NULL,
    checkpoint_id VARCHAR(100) NOT NULL,
    state JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_checkpoint_thread ON workflow_checkpoint(thread_id, created_at DESC);

COMMENT ON TABLE workflow_checkpoint IS '工作流执行检查点';

-- 智能体定义
CREATE TABLE agent (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL REFERENCES workflow(id) ON DELETE CASCADE,
    workflow_version INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    config JSONB,
    api_key VARCHAR(50),
    webhook_url VARCHAR(255),
    timeout_seconds INT DEFAULT 300,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_workflow ON agent(workflow_id);
CREATE INDEX idx_agent_api_key ON agent(api_key);

COMMENT ON TABLE agent IS '智能体定义';
COMMENT ON COLUMN agent.type IS '类型: DIALOG, API';
COMMENT ON COLUMN agent.status IS '状态: ACTIVE, INACTIVE';

-- 工作流执行记录
CREATE TABLE workflow_execution (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    agent_id BIGINT REFERENCES agent(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL,
    input JSONB,
    output JSONB,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms BIGINT
);

CREATE INDEX idx_execution_workflow ON workflow_execution(workflow_id);
CREATE INDEX idx_execution_agent ON workflow_execution(agent_id);
CREATE INDEX idx_execution_status ON workflow_execution(status);

COMMENT ON TABLE workflow_execution IS '工作流执行记录';
COMMENT ON COLUMN workflow_execution.status IS '状态: RUNNING, SUCCESS, FAILED, TIMEOUT';

-- 节点执行日志
CREATE TABLE node_execution_log (
    id BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL REFERENCES workflow_execution(id) ON DELETE CASCADE,
    node_id VARCHAR(50) NOT NULL,
    node_type VARCHAR(50) NOT NULL,
    input JSONB,
    output JSONB,
    status VARCHAR(20) NOT NULL,
    duration_ms BIGINT,
    error_message TEXT,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_node_log_execution ON node_execution_log(execution_id);

COMMENT ON TABLE node_execution_log IS '节点执行日志';

-- 调试执行记录
CREATE TABLE debug_execution (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    workflow_version INT NOT NULL,
    mode VARCHAR(20) NOT NULL,
    target_node_id VARCHAR(50),
    input JSONB,
    output JSONB,
    status VARCHAR(20) NOT NULL,
    duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_debug_execution_workflow ON debug_execution(workflow_id, created_at DESC);

COMMENT ON TABLE debug_execution IS '调试执行记录';
COMMENT ON COLUMN debug_execution.mode IS '模式: SINGLE_NODE, FULL';

-- 调试节点日志
CREATE TABLE debug_node_log (
    id BIGSERIAL PRIMARY KEY,
    debug_execution_id BIGINT NOT NULL REFERENCES debug_execution(id) ON DELETE CASCADE,
    node_id VARCHAR(50) NOT NULL,
    node_type VARCHAR(50) NOT NULL,
    step_index INT NOT NULL,
    input JSONB,
    output JSONB,
    status VARCHAR(20) NOT NULL,
    duration_ms BIGINT,
    error_message TEXT,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_debug_node_log_execution ON debug_node_log(debug_execution_id, step_index);

COMMENT ON TABLE debug_node_log IS '调试节点日志';

-- LLM 提供者配置
CREATE TABLE llm_provider (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    api_key VARCHAR(255),
    base_url VARCHAR(255) NOT NULL,
    timeout_seconds INT DEFAULT 60,
    options JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE llm_provider IS 'LLM提供者配置';
COMMENT ON COLUMN llm_provider.provider_type IS '类型: qwen, baidu, zhipu, ollama, custom';

-- LLM 模型配置
CREATE TABLE llm_model (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES llm_provider(id) ON DELETE CASCADE,
    model_id VARCHAR(100) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    max_tokens INT,
    input_price DECIMAL(10, 4),
    output_price DECIMAL(10, 4),
    capabilities JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_model_provider ON llm_model(provider_id);
CREATE UNIQUE INDEX idx_model_id ON llm_model(model_id);

COMMENT ON TABLE llm_model IS 'LLM模型配置';
```

- [ ] **Step 2: 提交代码**

```bash
git add src/main/resources/db/migration/V1__init_schema.sql
git commit -m "feat: add database schema migration

- Add workflow and workflow_version tables
- Add agent table for DIALOG and API types
- Add workflow_execution and node_execution_log tables
- Add debug_execution and debug_node_log tables
- Add workflow_checkpoint for LangGraph4j
- Add llm_provider and llm_model tables

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: 创建配置类

**Files:**
- Create: `src/main/java/com/workflow/engine/config/MybatisPlusConfig.java`
- Create: `src/main/java/com/workflow/engine/config/JacksonConfig.java`
- Create: `src/main/java/com/workflow/engine/config/AsyncConfig.java`

- [ ] **Step 1: 创建 MyBatis-Plus 配置**

```java
package com.workflow.engine.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@MapperScan("com.workflow.engine.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "startedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "executedAt", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```

- [ ] **Step 2: 创建 Jackson 配置**

```java
package com.workflow.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

- [ ] **Step 3: 创建异步配置**

```java
package com.workflow.engine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("workflow-async-");
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 4: 提交代码**

```bash
git add src/main/java/com/workflow/engine/config/
git commit -m "feat: add configuration classes

- Add MyBatis-Plus config with pagination and auto-fill
- Add Jackson config for Java 8 time module
- Add async config with thread pool

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 阶段二：数据模型层

### Task 4: 创建 FlowGram.AI 数据模型

**Files:**
- Create: `src/main/java/com/workflow/engine/engine/model/WorkflowJSON.java`
- Create: `src/main/java/com/workflow/engine/engine/model/NodeJSON.java`
- Create: `src/main/java/com/workflow/engine/engine/model/EdgeJSON.java`
- Test: `src/test/java/com/workflow/engine/engine/model/WorkflowJSONTest.java`

- [ ] **Step 1: 写失败的测试**

```java
package com.workflow.engine.engine.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowJSONTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeFlowGramAIJson() throws Exception {
        String json = """
            {
              "nodes": [
                {
                  "id": "start_0",
                  "type": "start",
                  "meta": { "position": { "x": 0, "y": 0 } },
                  "data": { "title": "Start" }
                },
                {
                  "id": "llm_0",
                  "type": "LLM",
                  "meta": { "position": { "x": 200, "y": 0 } },
                  "data": {
                    "modelId": "qwen-max",
                    "systemPrompt": "You are an assistant"
                  }
                }
              ],
              "edges": [
                { "sourceNodeID": "start_0", "targetNodeID": "llm_0" }
              ]
            }
            """;

        WorkflowJSON workflow = objectMapper.readValue(json, WorkflowJSON.class);

        assertNotNull(workflow);
        assertEquals(2, workflow.nodes().size());
        assertEquals(1, workflow.edges().size());
        assertEquals("start_0", workflow.nodes().get(0).id());
        assertEquals("LLM", workflow.nodes().get(1).type());
        assertEquals("start_0", workflow.edges().get(0).sourceNodeID());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -Dtest=WorkflowJSONTest
```

Expected: FAIL - classes not found

- [ ] **Step 3: 创建 WorkflowJSON record**

```java
package com.workflow.engine.engine.model;

import java.util.List;

public record WorkflowJSON(
    List<NodeJSON> nodes,
    List<EdgeJSON> edges
) {
    public NodeJSON findNode(String nodeId) {
        return nodes.stream()
            .filter(n -> n.id().equals(nodeId))
            .findFirst()
            .orElse(null);
    }

    public NodeJSON findStartNode() {
        return nodes.stream()
            .filter(n -> "start".equalsIgnoreCase(n.type()))
            .findFirst()
            .orElse(null);
    }

    public NodeJSON findEndNode() {
        return nodes.stream()
            .filter(n -> "end".equalsIgnoreCase(n.type()))
            .findFirst()
            .orElse(null);
    }
}
```

- [ ] **Step 4: 创建 NodeJSON record**

```java
package com.workflow.engine.engine.model;

import java.util.Map;

public record NodeJSON(
    String id,
    String type,
    Map<String, Object> meta,
    Map<String, Object> data
) {
    @SuppressWarnings("unchecked")
    public <T> T getDataValue(String key) {
        if (data == null) return null;
        return (T) data.get(key);
    }

    public String getModelId() {
        return getDataValue("modelId");
    }

    public String getSystemPrompt() {
        return getDataValue("systemPrompt");
    }

    public String getUserPrompt() {
        return getDataValue("userPrompt");
    }

    public String getToolId() {
        return getDataValue("toolId");
    }
}
```

- [ ] **Step 5: 创建 EdgeJSON record**

```java
package com.workflow.engine.engine.model;

public record EdgeJSON(
    String sourceNodeID,
    String targetNodeID,
    String sourcePortID,
    String targetPortID
) {
    public EdgeJSON(String sourceNodeID, String targetNodeID) {
        this(sourceNodeID, targetNodeID, null, null);
    }
}
```

- [ ] **Step 6: 运行测试确认通过**

```bash
mvn test -Dtest=WorkflowJSONTest
```

Expected: PASS

- [ ] **Step 7: 提交代码**

```bash
git add src/main/java/com/workflow/engine/engine/model/ src/test/java/com/workflow/engine/engine/model/
git commit -m "feat: add FlowGram.AI JSON data models

- Add WorkflowJSON record with node/edge lookup methods
- Add NodeJSON record with convenience getters
- Add EdgeJSON record with port support
- Add unit test for JSON deserialization

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: 创建 MyBatis-Plus Entity 类

**Files:**
- Create: `src/main/java/com/workflow/engine/model/Workflow.java`
- Create: `src/main/java/com/workflow/engine/model/WorkflowVersion.java`
- Create: `src/main/java/com/workflow/engine/model/Agent.java`
- Test: `src/test/java/com/workflow/engine/model/WorkflowTest.java`

- [ ] **Step 1: 创建 Workflow Entity**

```java
package com.workflow.engine.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("workflow")
public class Workflow {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Integer version;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
}
```

- [ ] **Step 2: 创建 WorkflowVersion Entity**

```java
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
```

- [ ] **Step 3: 创建 Agent Entity**

```java
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
```

- [ ] **Step 4: 创建 WorkflowExecution Entity**

```java
package com.workflow.engine.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "workflow_execution", autoResultMap = true)
public class WorkflowExecution {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workflowId;

    private Long agentId;

    private String status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> input;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> output;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Long durationMs;

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_TIMEOUT = "TIMEOUT";
}
```

- [ ] **Step 5: 创建 NodeExecutionLog Entity**

```java
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
```

- [ ] **Step 6: 创建 DebugExecution Entity**

```java
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
```

- [ ] **Step 7: 创建 DebugNodeLog Entity**

```java
package com.workflow.engine.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "debug_node_log", autoResultMap = true)
public class DebugNodeLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long debugExecutionId;

    private String nodeId;

    private String nodeType;

    private Integer stepIndex;

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
```

- [ ] **Step 8: 提交代码**

```bash
git add src/main/java/com/workflow/engine/model/
git commit -m "feat: add MyBatis-Plus entity classes

- Add Workflow entity
- Add WorkflowVersion entity with WorkflowJSON type handler
- Add Agent entity supporting DIALOG and API types
- Add WorkflowExecution entity
- Add NodeExecutionLog entity
- Add DebugExecution and DebugNodeLog entities

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: 创建 Mapper 接口

**Files:**
- Create: `src/main/java/com/workflow/engine/mapper/WorkflowMapper.java`
- Create: `src/main/java/com/workflow/engine/mapper/WorkflowVersionMapper.java`
- Create: `src/main/java/com/workflow/engine/mapper/AgentMapper.java`
- Create: `src/main/java/com/workflow/engine/mapper/WorkflowExecutionMapper.java`
- Create: `src/main/java/com/workflow/engine/mapper/NodeExecutionLogMapper.java`
- Create: `src/main/java/com/workflow/engine/mapper/DebugExecutionMapper.java`
- Create: `src/main/java/com/workflow/engine/mapper/DebugNodeLogMapper.java`

- [ ] **Step 1: 创建所有 Mapper 接口**

```java
// WorkflowMapper.java
package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.Workflow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowMapper extends BaseMapper<Workflow> {
}
```

```java
// WorkflowVersionMapper.java
package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.WorkflowVersion;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WorkflowVersionMapper extends BaseMapper<WorkflowVersion> {

    default WorkflowVersion findByWorkflowIdAndVersion(Long workflowId, Integer version) {
        return selectOne(new LambdaQueryWrapper<WorkflowVersion>()
            .eq(WorkflowVersion::getWorkflowId, workflowId)
            .eq(WorkflowVersion::getVersion, version));
    }

    default List<WorkflowVersion> findByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapper<WorkflowVersion>()
            .eq(WorkflowVersion::getWorkflowId, workflowId)
            .orderByDesc(WorkflowVersion::getVersion));
    }
}
```

```java
// AgentMapper.java
package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.Agent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentMapper extends BaseMapper<Agent> {

    default Agent findByApiKey(String apiKey) {
        return selectOne(new LambdaQueryWrapper<Agent>()
            .eq(Agent::getApiKey, apiKey));
    }

    default List<Agent> findByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapper<Agent>()
            .eq(Agent::getWorkflowId, workflowId));
    }

    default List<Agent> findByType(String type) {
        return selectList(new LambdaQueryWrapper<Agent>()
            .eq(Agent::getType, type)
            .eq(Agent::getStatus, Agent.STATUS_ACTIVE));
    }
}
```

```java
// WorkflowExecutionMapper.java
package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.WorkflowExecution;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WorkflowExecutionMapper extends BaseMapper<WorkflowExecution> {

    default List<WorkflowExecution> findByAgentId(Long agentId) {
        return selectList(new LambdaQueryWrapper<WorkflowExecution>()
            .eq(WorkflowExecution::getAgentId, agentId)
            .orderByDesc(WorkflowExecution::getStartedAt));
    }

    default List<WorkflowExecution> findByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapper<WorkflowExecution>()
            .eq(WorkflowExecution::getWorkflowId, workflowId)
            .orderByDesc(WorkflowExecution::getStartedAt));
    }
}
```

```java
// NodeExecutionLogMapper.java
package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.NodeExecutionLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NodeExecutionLogMapper extends BaseMapper<NodeExecutionLog> {

    default List<NodeExecutionLog> findByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapper<NodeExecutionLog>()
            .eq(NodeExecutionLog::getExecutionId, executionId)
            .orderByAsc(NodeExecutionLog::getId));
    }
}
```

```java
// DebugExecutionMapper.java
package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workflow.engine.model.DebugExecution;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DebugExecutionMapper extends BaseMapper<DebugExecution> {

    default List<DebugExecution> findByWorkflowId(Long workflowId, int limit) {
        return selectList(new LambdaQueryWrapper<DebugExecution>()
            .eq(DebugExecution::getWorkflowId, workflowId)
            .orderByDesc(DebugExecution::getCreatedAt)
            .last("LIMIT " + limit));
    }
}
```

```java
// DebugNodeLogMapper.java
package com.workflow.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workflow.engine.model.DebugNodeLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DebugNodeLogMapper extends BaseMapper<DebugNodeLog> {

    default List<DebugNodeLog> findByDebugExecutionId(Long debugExecutionId) {
        return selectList(new LambdaQueryWrapper<DebugNodeLog>()
            .eq(DebugNodeLog::getDebugExecutionId, debugExecutionId)
            .orderByAsc(DebugNodeLog::getStepIndex));
    }
}
```

- [ ] **Step 2: 提交代码**

```bash
git add src/main/java/com/workflow/engine/mapper/
git commit -m "feat: add MyBatis-Plus mapper interfaces

- Add WorkflowMapper
- Add WorkflowVersionMapper with version lookup
- Add AgentMapper with API key and type queries
- Add WorkflowExecutionMapper
- Add NodeExecutionLogMapper
- Add DebugExecutionMapper and DebugNodeLogMapper

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 阶段三：工作流管理 API

### Task 7: 创建 Workflow Service 和 Controller

**Files:**
- Create: `src/main/java/com/workflow/engine/service/WorkflowService.java`
- Create: `src/main/java/com/workflow/engine/service/impl/WorkflowServiceImpl.java`
- Create: `src/main/java/com/workflow/engine/dto/WorkflowCreateRequest.java`
- Create: `src/main/java/com/workflow/engine/dto/WorkflowUpdateRequest.java`
- Create: `src/main/java/com/workflow/engine/controller/WorkflowController.java`
- Test: `src/test/java/com/workflow/engine/service/WorkflowServiceTest.java`

- [ ] **Step 1: 创建 DTO 类**

```java
// WorkflowCreateRequest.java
package com.workflow.engine.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkflowCreateRequest(
    @NotBlank String name,
    String description
) {}
```

```java
// WorkflowUpdateRequest.java
package com.workflow.engine.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkflowUpdateRequest(
    @NotBlank String name,
    String description
) {}
```

- [ ] **Step 2: 创建 WorkflowService 接口**

```java
package com.workflow.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.dto.WorkflowUpdateRequest;
import com.workflow.engine.model.Workflow;

import java.util.List;

public interface WorkflowService extends IService<Workflow> {

    Workflow create(WorkflowCreateRequest request);

    Workflow update(Long id, WorkflowUpdateRequest request);

    void delete(Long id);

    Workflow getById(Long id);

    List<Workflow> listAll();
}
```

- [ ] **Step 3: 创建 WorkflowServiceImpl**

```java
package com.workflow.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.dto.WorkflowUpdateRequest;
import com.workflow.engine.exception.WorkflowNotFoundException;
import com.workflow.engine.mapper.WorkflowMapper;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.service.WorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowServiceImpl extends ServiceImpl<WorkflowMapper, Workflow> implements WorkflowService {

    @Override
    @Transactional
    public Workflow create(WorkflowCreateRequest request) {
        Workflow workflow = new Workflow();
        workflow.setName(request.name());
        workflow.setDescription(request.description());
        workflow.setVersion(1);
        workflow.setStatus(Workflow.STATUS_DRAFT);
        save(workflow);
        return workflow;
    }

    @Override
    @Transactional
    public Workflow update(Long id, WorkflowUpdateRequest request) {
        Workflow workflow = getById(id);
        if (workflow == null) {
            throw new WorkflowNotFoundException(id);
        }
        workflow.setName(request.name());
        workflow.setDescription(request.description());
        updateById(workflow);
        return workflow;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Workflow workflow = getById(id);
        if (workflow == null) {
            throw new WorkflowNotFoundException(id);
        }
        workflow.setStatus(Workflow.STATUS_ARCHIVED);
        updateById(workflow);
    }

    @Override
    public Workflow getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<Workflow> listAll() {
        return list();
    }
}
```

- [ ] **Step 4: 创建异常类**

```java
package com.workflow.engine.exception;

public class WorkflowNotFoundException extends RuntimeException {
    public WorkflowNotFoundException(Long id) {
        super("Workflow not found: " + id);
    }
}
```

```java
package com.workflow.engine.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowNotFound(WorkflowNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "WORKFLOW_NOT_FOUND",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }
}
```

- [ ] **Step 5: 创建 WorkflowController**

```java
package com.workflow.engine.controller;

import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.dto.WorkflowUpdateRequest;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    public ResponseEntity<Workflow> create(@Valid @RequestBody WorkflowCreateRequest request) {
        Workflow workflow = workflowService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflow);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getById(@PathVariable Long id) {
        Workflow workflow = workflowService.getById(id);
        return ResponseEntity.ok(workflow);
    }

    @GetMapping
    public ResponseEntity<List<Workflow>> list() {
        List<Workflow> workflows = workflowService.listAll();
        return ResponseEntity.ok(workflows);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workflow> update(@PathVariable Long id, @Valid @RequestBody WorkflowUpdateRequest request) {
        Workflow workflow = workflowService.update(id, request);
        return ResponseEntity.ok(workflow);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 6: 写测试**

```java
package com.workflow.engine.service;

import com.workflow.engine.dto.WorkflowCreateRequest;
import com.workflow.engine.dto.WorkflowUpdateRequest;
import com.workflow.engine.exception.WorkflowNotFoundException;
import com.workflow.engine.model.Workflow;
import com.workflow.engine.service.impl.WorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkflowServiceTest {

    @Autowired
    private WorkflowService workflowService;

    @Test
    void shouldCreateWorkflow() {
        WorkflowCreateRequest request = new WorkflowCreateRequest("Test Workflow", "Description");

        Workflow workflow = workflowService.create(request);

        assertNotNull(workflow.getId());
        assertEquals("Test Workflow", workflow.getName());
        assertEquals("Description", workflow.getDescription());
        assertEquals(1, workflow.getVersion());
        assertEquals(Workflow.STATUS_DRAFT, workflow.getStatus());
    }

    @Test
    void shouldUpdateWorkflow() {
        WorkflowCreateRequest createRequest = new WorkflowCreateRequest("Original Name", null);
        Workflow created = workflowService.create(createRequest);

        WorkflowUpdateRequest updateRequest = new WorkflowUpdateRequest("Updated Name", "Updated Description");
        Workflow updated = workflowService.update(created.getId(), updateRequest);

        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated Description", updated.getDescription());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentWorkflow() {
        WorkflowUpdateRequest request = new WorkflowUpdateRequest("Name", "Description");

        assertThrows(WorkflowNotFoundException.class, () -> {
            workflowService.update(999L, request);
        });
    }
}
```

- [ ] **Step 7: 运行测试**

```bash
mvn test -Dtest=WorkflowServiceTest
```

Expected: PASS

- [ ] **Step 8: 提交代码**

```bash
git add src/main/java/com/workflow/engine/service/ src/main/java/com/workflow/engine/dto/ src/main/java/com/workflow/engine/controller/ src/main/java/com/workflow/engine/exception/ src/test/java/com/workflow/engine/service/
git commit -m "feat: add workflow management API

- Add WorkflowService with CRUD operations
- Add WorkflowController with REST endpoints
- Add DTOs for create and update requests
- Add global exception handler
- Add unit tests for WorkflowService

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

由于这是一个大型项目，完整的计划会非常长。以上是前 7 个任务的详细实现计划，展示了 TDD 方法和完整的代码示例。

**计划完整版包含以下阶段**（文件太长，这里列出后续任务概要）：

## 阶段四：工作流版本管理
- Task 8: WorkflowVersion Service
- Task 9: 版本回滚功能

## 阶段五：工作流引擎核心
- Task 10: WorkflowExecutor 实现
- Task 11: Start/End 节点处理器
- Task 12: LLM 节点处理器
- Task 13: Tool 节点处理器
- Task 14: Condition 节点处理器
- Task 15: Parallel 节点处理器
- Task 16: LangGraph4j Hook 集成

## 阶段六：智能体发布
- Task 17: Agent Service
- Task 18: 对话智能体发布
- Task 19: API 智能体发布

## 阶段七：工具系统
- Task 20: 工具配置加载
- Task 21: 内置工具实现
- Task 22: 外部 API 工具

## 阶段八：LLM 集成
- Task 23: Model Registry
- Task 24: 通义千问适配器
- Task 25: Ollama 适配器

## 阶段九：API 智能体执行
- Task 26: 同步执行 API
- Task 27: 异步执行 API
- Task 28: Webhook 回调

## 阶段十：对话智能体
- Task 29: WebSocket Handler
- Task 30: 对话会话管理

## 阶段十一：调试功能
- Task 31: 单节点调试
- Task 32: 完整调试
- Task 33: SSE 状态推送

---

计划已保存到 `docs/superpowers/plans/2026-04-02-workflow-engine-implementation.md`。

**两种执行方式**：

1. **Subagent-Driven（推荐）** - 我为每个任务派遣一个新的子代理，任务之间进行审查，快速迭代

2. **Inline Execution** - 在当前会话中使用 executing-plans 执行，批量执行并设置检查点

**你想使用哪种方式？**