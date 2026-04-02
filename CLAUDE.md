# 工作流画布引擎 - 项目规则

## 项目概述

工作流画布引擎，用于创建和发布智能体。支持两种发布模式：
- **对话智能体**：交互式对话接口
- **API 智能体**：对外 API 调用接口

## 技术栈

- **Java 17**：使用现代 Java 特性（Record、Sealed Classes、Pattern Matching、Virtual Threads）
- **Spring Boot 3.3.x**：主框架
- **MyBatis-Plus 3.5.x**：数据交互框架
- **LangChain4j 1.12.x**：AI/LLM 集成
- **LangGraph4j 1.6.x**：工作流编排引擎
- **PostgreSQL**：数据存储

## 架构原则

### 分层架构
```
controller/     - API 层，处理 HTTP 请求
service/        - 业务逻辑层
repository/     - 数据访问层
model/          - 领域模型（Entity、Value Object）
dto/            - 数据传输对象
config/         - 配置类
```

### 模块划分
- `workflow-engine`：核心工作流引擎
- `agent-runtime`：智能体运行时
- `publish-service`：发布服务（对话/API）
- `canvas-core`：画布核心逻辑

## 编码规范

### 命名约定

```java
// 类名：PascalCase
public class WorkflowExecutionService {}

// 方法名：camelCase，动词开头
public void executeWorkflow() {}
public Optional<Agent> findAgentById(Long id) {}

// 常量：UPPER_SNAKE_CASE
public static final String DEFAULT_MODEL = "gpt-4";

// 包名：单数形式
com.example.agent  // ✓
com.example.agents // ✗
```

### Java 17 最佳实践

```java
// 使用 Record 作为不可变数据载体
public record WorkflowNode(
    String id,
    NodeType type,
    Map<String, Object> config
) {}

// 使用 Sealed Classes 限定类型层次
public sealed interface Agent permits DialogAgent, ApiAgent {}
public final class DialogAgent implements Agent {}
public final class ApiAgent implements Agent {}

// 使用 Pattern Matching
public String processAgent(Agent agent) {
    return switch (agent) {
        case DialogAgent d -> "dialog:" + d.id();
        case ApiAgent a -> "api:" + a.endpoint();
    };
}

// 使用 Text Blocks 处理多行字符串
String prompt = """
    你是一个工作流智能体。
    请根据用户输入执行相应操作。
    """;
```

### 异常处理

```java
// 使用业务异常，避免使用通用 Exception
public class AgentNotFoundException extends RuntimeException {
    public AgentNotFoundException(Long agentId) {
        super("Agent not found: " + agentId);
    }
}

// 在 Service 层处理异常，Controller 层统一响应
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AgentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAgentNotFound(AgentNotFoundException e) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse("AGENT_NOT_FOUND", e.getMessage()));
    }
}
```

### 日志规范

```java
// 使用 Slf4j + Lombok
@Slf4j
@Service
public class WorkflowService {

    public void execute(Workflow workflow) {
        log.info("Executing workflow: id={}, nodes={}",
            workflow.id(), workflow.nodes().size());

        try {
            // 业务逻辑
        } catch (Exception e) {
            log.error("Workflow execution failed: id={}", workflow.id(), e);
            throw e;
        }
    }
}
```

## 工作流开发规范

### LangGraph4j 工作流定义

```java
// 工作流节点定义
@Component
public class AgentExecutionNode implements NodeHandler<WorkflowState> {

    @Override
    public WorkflowState execute(WorkflowState state) {
        // 清晰的状态转换
        return state.withStatus(Status.EXECUTING);
    }
}

// 工作流图构建
@Configuration
public class WorkflowGraphConfig {

    @Bean
    public StateGraph<WorkflowState> agentWorkflowGraph() {
        return new StateGraph<>(WorkflowState.class)
            .addNode("start", startNode())
            .addNode("execute", executeNode())
            .addNode("end", endNode())
            .addEdge("start", "execute")
            .addEdge("execute", "end")
            .setEntryPoint("start");
    }
}
```

### 智能体发布模式

```java
// 对话智能体发布
@Service
public class DialogAgentPublisher {

    public PublishedAgent publish(DialogAgentConfig config) {
        // 1. 验证配置
        validateConfig(config);
        // 2. 创建运行时
        // 3. 注册路由
        // 4. 返回发布信息
    }
}

// API 智能体发布
@Service
public class ApiAgentPublisher {

    public PublishedApi publish(ApiAgentConfig config) {
        // 1. 验证配置
        // 2. 创建 API 端点
        // 3. 注册到网关
        // 4. 返回 API 信息
    }
}
```

## 测试规范

### 测试命名

```java
// 测试类命名：被测类 + Test
class WorkflowServiceTest {}

// 测试方法命名：方法名_场景_预期结果
@Test
void executeWorkflow_withValidInput_shouldReturnSuccess() {}

@Test
void executeWorkflow_withEmptyNodes_shouldThrowException() {}
```

### 测试结构

```java
@Test
void executeWorkflow_withValidInput_shouldReturnSuccess() {
    // Given
    var workflow = WorkflowTestData.createDefaultWorkflow();

    // When
    var result = workflowService.execute(workflow);

    // Then
    assertThat(result.status()).isEqualTo(Status.COMPLETED);
    assertThat(result.output()).isNotEmpty();
}
```

### 测试原则

1. 每个测试只验证一个行为
2. 使用 Given-When-Then 结构
3. Mock 外部依赖（LLM API、数据库）
4. 测试覆盖率目标：80%+

## Git 工作流

### 分支命名

- `feature/xxx`：新功能
- `fix/xxx`：Bug 修复
- `refactor/xxx`：重构
- `docs/xxx`：文档更新

### 提交信息格式

```
<type>(<scope>): <subject>

type: feat|fix|refactor|docs|test|chore
scope: workflow|agent|publish|canvas

示例:
feat(workflow): add parallel execution support
fix(publish): resolve agent timeout issue
refactor(canvas): extract node validation logic
```

## 代码可维护性

### SOLID 原则

- **S**：每个类只负责一件事
- **O**：对扩展开放，对修改关闭
- **L**：子类可以替换父类
- **I**：接口职责单一
- **D**：依赖抽象而非具体实现

### 代码审查清单

- [ ] 代码是否遵循命名规范
- [ ] 是否有足够的注释和文档
- [ ] 是否有对应的单元测试
- [ ] 是否处理了边界情况和异常
- [ ] 是否有性能问题（N+1 查询、内存泄漏）
- [ ] 是否符合安全最佳实践

### 禁止事项

- 不要在循环中调用数据库或外部 API
- 不要使用 `System.out.println`，使用日志框架
- 不要在代码中硬编码配置，使用配置文件
- 不要忽略异常，必须处理或向上抛出
- 不要创建过大的类（超过 500 行），考虑拆分

## 框架利用原则

**不要重复造轮子**，充分利用框架和库的内置特性：

### LangGraph4j 特性利用

| 特性 | 用途 | 说明 |
|------|------|------|
| **Checkpoint** | 状态持久化 | 工作流执行状态自动保存，支持暂停/恢复，无需手动实现 |
| **Hook** | 执行拦截 | 节点执行前后拦截，用于日志记录、状态推送、错误处理 |
| **StateGraph** | 图构建 | 动态构建工作流图，支持条件边、并行执行 |
| **Subgraph** | 子工作流 | 复杂流程拆分为子图，提高可维护性 |
| **Memory** | 对话记忆 | 内置对话历史管理，无需手动维护 |

**必须使用的场景**：
- 执行日志记录 → 使用 Hook，不要在节点处理器中手动记录
- 状态保存/恢复 → 使用 Checkpoint，不要自己实现状态持久化
- 对话历史 → 使用 LangGraph4j Memory，不要手动管理
- 节点状态推送 → 使用 Hook 结合 SSE，不要在每个节点重复推送逻辑

### Spring Boot 特性利用

| 特性 | 用途 |
|------|------|
| **@Async** | 异步执行 |
| **@EventListener** | 事件驱动 |
| **@Transactional** | 事务管理 |
| **@Cacheable** | 缓存 |
| **@Scheduled** | 定时任务 |
| **@ConfigurationProperties** | 配置绑定 |

### MyBatis-Plus 最佳实践

**基础用法**：
```java
// Entity 定义
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
}

// Mapper 定义（继承 BaseMapper）
public interface WorkflowMapper extends BaseMapper<Workflow> {
    // BaseMapper 已提供：insert, update, delete, selectById, selectList 等
    // 自定义方法按需添加
}

// Service 定义（继承 IService）
public interface WorkflowService extends IService<Workflow> {
    // 自定义业务方法
}

@Service
public class WorkflowServiceImpl extends ServiceImpl<WorkflowMapper, Workflow> implements WorkflowService {
    // IService 已提供：save, update, remove, getById, list 等
}
```

**必须使用的特性**：
| 特性 | 说明 | 禁止行为 |
|------|------|----------|
| **BaseMapper** | 基础 CRUD | 不要手动写 insert/update/delete SQL |
| **IService** | 业务层封装 | 不要在 Service 中重复封装 BaseMapper 方法 |
| **Wrapper** | 条件构造器 | 不要拼接 SQL 字符串 |
| **Page** | 分页插件 | 不要手动实现分页 |
| **@TableLogic** | 逻辑删除 | 不要手动实现删除标记 |
| **@TableField(fill)** | 自动填充 | 不要手动设置 createdAt/updatedAt |
| **@Version** | 乐观锁 | 不要手动实现版本控制 |

**条件查询示例**：
```java
// 使用 LambdaQueryWrapper（推荐）
List<Workflow> list = workflowMapper.selectList(
    new LambdaQueryWrapper<Workflow>()
        .eq(Workflow::getStatus, "DRAFT")
        .orderByDesc(Workflow::getCreatedAt)
);

// 分页查询
Page<Workflow> page = workflowMapper.selectPage(
    new Page<>(1, 20),
    new LambdaQueryWrapper<Workflow>()
        .like(Workflow::getName, keyword)
);
```

### LangChain4j 特性利用

| 特性 | 用途 |
|------|------|
| **AiService** | 智能体服务构建 |
| **ChatMemory** | 对话记忆 |
| **Tool** | 工具定义 |
| **DocumentLoader** | 文档加载 |
| **EmbeddingModel** | 向量嵌入 |

**检查清单**：在实现任何功能前，先确认框架是否已提供：
- [ ] LangGraph4j 是否有现成的 Hook/Checkpoint 实现？
- [ ] Spring Boot 是否有现成的异步/事件机制？
- [ ] LangChain4j 是否有现成的模型适配？

## 依赖管理

```xml
<!-- 使用 BOM 管理版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 文档规范

- 每个公共 API 必须有 JavaDoc
- 复杂业务逻辑必须有注释说明
- README.md 包含项目介绍、快速开始、配置说明
- 架构决策记录（ADR）存放在 `docs/adr/` 目录