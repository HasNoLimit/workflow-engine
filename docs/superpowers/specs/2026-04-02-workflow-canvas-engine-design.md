# 工作流画布引擎 - 设计文档

## 项目概述

工作流画布引擎，用于创建和发布智能体。前端使用 FlowGram.AI 技术，后端需要适配其 JSON 画布格式。

支持两种发布模式：
- **对话智能体**：聊天对话模式，用户通过聊天界面交互
- **API 智能体**：REST API 触发 + 异步执行 + Webhook 回调通知

## 技术栈

| 技术 | 用途 |
|------|------|
| Java 17 | 主语言，使用现代特性（Record、Sealed Classes、Pattern Matching、Virtual Threads） |
| Spring Boot 3.x | 主框架 |
| MyBatis-Plus | 数据交互框架 |
| LangChain4j | AI/LLM 集成 |
| LangGraph4j | 工作流编排引擎 |
| PostgreSQL | 数据存储（唯一选择） |
| SSE (Server-Sent Events) | 实时状态推送 |

---

## 第一部分：数据模型设计

### 核心实体（MyBatis-Plus Entity）

```java
// 工作流定义
@Data
@TableName("workflow")
public class Workflow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer version;          // 当前版本号
    private String status;            // DRAFT, PUBLISHED, ARCHIVED
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

// 工作流版本快照
@Data
@TableName("workflow_version")
public class WorkflowVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workflowId;
    private Integer version;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private WorkflowJSON canvas;      // FlowGram.AI JSON
    private String changeNote;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

// 智能体定义（使用 type 字段区分类型）
@Data
@TableName("agent")
public class Agent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workflowId;
    private Integer workflowVersion;
    private String name;
    private String type;              // DIALOG, API
    private String status;            // ACTIVE, INACTIVE
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;
    private String apiKey;            // API 智能体
    private String webhookUrl;        // API 智能体
    private Integer timeoutSeconds;   // API 智能体
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

// 工作流执行记录
@Data
@TableName("workflow_execution")
public class WorkflowExecution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workflowId;
    private Long agentId;
    private String status;            // RUNNING, SUCCESS, FAILED, TIMEOUT
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> input;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> output;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
}

// 节点执行日志
@Data
@TableName("node_execution_log")
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

### Mapper 层

```java
// 工作流 Mapper
public interface WorkflowMapper extends BaseMapper<Workflow> {
    // BaseMapper 已提供：insert, update, delete, selectById, selectList 等
}

// 工作流版本 Mapper
public interface WorkflowVersionMapper extends BaseMapper<WorkflowVersion> {
    default WorkflowVersion findByWorkflowIdAndVersion(Long workflowId, Integer version) {
        return selectOne(new LambdaQueryWrapper<WorkflowVersion>()
            .eq(WorkflowVersion::getWorkflowId, workflowId)
            .eq(WorkflowVersion::getVersion, version));
    }
}

// 智能体 Mapper
public interface AgentMapper extends BaseMapper<Agent> {}

// 工作流执行记录 Mapper
public interface WorkflowExecutionMapper extends BaseMapper<WorkflowExecution> {}

// 节点执行日志 Mapper
public interface NodeExecutionLogMapper extends BaseMapper<NodeExecutionLog> {
    default List<NodeExecutionLog> findByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapper<NodeExecutionLog>()
            .eq(NodeExecutionLog::getExecutionId, executionId)
            .orderByAsc(NodeExecutionLog::getId));
    }
}
```

### Service 层（继承 IService）

```java
// 工作流 Service
public interface WorkflowService extends IService<Workflow> {
    Workflow getByid(Long id);
    Workflow create(Workflow workflow);
    Workflow update(Long id, Workflow workflow);
    void delete(Long id);
}

@Service
public class WorkflowServiceImpl extends ServiceImpl<WorkflowMapper, Workflow> implements WorkflowService {
    // IService 已提供：save, updateById, removeById, getById, list 等
    
    @Override
    public Workflow create(Workflow workflow) {
        save(workflow);
        return workflow;
    }
}
```

### 版本管理规则

1. **版本号规则**：从 1 开始，每次保存新版本自动递增
2. **版本快照**：每个版本的画布数据独立存储，不可修改
3. **智能体绑定版本**：发布智能体时绑定特定版本，后续工作流修改不影响已发布智能体
4. **版本回滚**：可将工作流回滚到任意历史版本（创建新版本，复制旧版本数据）

### 数据库表结构

```
workflow           - 工作流定义（当前版本号）
workflow_version   - 版本快照（存储每个版本的画布 JSON）
agent              - 智能体定义（绑定特定版本号）
workflow_execution - 执行记录（记录执行时的版本号）
node_execution_log - 节点执行详细日志
tool_definition    - 工具定义
llm_provider       - LLM 提供者配置
debug_execution    - 调试执行记录
debug_node_log     - 调试节点日志
```

### 版本相关 API

| API | 说明 |
|-----|------|
| `POST /workflows/{id}/versions` | 保存新版本 |
| `GET /workflows/{id}/versions` | 获取版本列表 |
| `GET /workflows/{id}/versions/{v}` | 获取特定版本详情 |
| `POST /workflows/{id}/rollback/{v}` | 回滚到指定版本 |
| `GET /workflows/{id}/versions/{v}/compare/{v2}` | 版本对比 |

---

## 第二部分：工作流引擎核心设计

### FlowGram.AI JSON 解析与执行

```java
// FlowGram.AI JSON 结构适配（Free Layout）
public record WorkflowJSON(
    List<NodeJSON> nodes,
    List<EdgeJSON> edges
) {}

public record NodeJSON(
    String id,
    String type,
    Map<String, Object> meta,
    Map<String, Object> data
) {}

public record EdgeJSON(
    String sourceNodeID,
    String targetNodeID,
    String sourcePortID,    // 可选
    String targetPortID     // 可选
) {}
```

### 节点处理器设计

```java
// 节点处理器接口
public interface NodeHandler<T extends WorkflowContext> {
    String nodeType();
    NodeExecutionResult execute(T context, NodeJSON node);
}

// 工作流执行上下文
public record WorkflowContext(
    String executionId,
    Map<String, Object> variables,   // 工作流变量
    Map<String, NodeExecutionResult> nodeResults,  // 已执行节点结果
    WorkflowJSON workflow
) {
    // 支持变量引用：{{node_0.output}}
    public Object resolveVariable(String expression);
}

// 节点执行结果
public record NodeExecutionResult(
    String nodeId,
    ExecutionStatus status,
    Map<String, Object> output,
    String errorMessage
) {}
```

### 四种核心节点处理器

| 节点类型 | 处理器 | 功能 |
|----------|--------|------|
| `LLM` | `LLMNodeHandler` | 调用 LLM，支持多模型、prompt 模板、变量注入 |
| `Tool` | `ToolNodeHandler` | 执行内置工具/外部API/MCP工具 |
| `Condition` | `ConditionNodeHandler` | 条件判断，决定下一节点走向 |
| `Parallel` | `ParallelNodeHandler` | 并行执行多个分支，等待所有完成或任一完成 |

### LangGraph4j 集成（充分利用框架特性）

```java
@Configuration
public class WorkflowEngineConfig {

    @Bean
    public StateGraph<WorkflowState> buildWorkflowGraph(WorkflowJSON workflow) {
        var graph = new StateGraph<>(WorkflowState.class);
        
        // 动态添加节点
        for (NodeJSON node : workflow.nodes()) {
            graph.addNode(node.id(), context -> executeNode(node, context));
        }
        
        // 动态添加边
        for (EdgeJSON edge : workflow.edges()) {
            graph.addEdge(edge.sourceNodeID(), edge.targetNodeID());
        }
        
        // 设置入口点（start 节点）
        graph.setEntryPoint(findStartNode(workflow).id());
        
        return graph;
    }
}
```

### 使用 Hook 实现执行拦截（替代手动日志记录）

LangGraph4j 提供 Hook 机制，用于节点执行前后的拦截，**不要在节点处理器中手动记录日志**。

```java
@Component
public class WorkflowExecutionHook implements NodeHook<WorkflowState> {

    private final DebugStreamService streamService;
    private final NodeExecutionLogMapper nodeExecutionLogMapper;

    @Override
    public void beforeNode(String nodeId, WorkflowState state) {
        // 节点开始执行时推送 SSE 状态
        streamService.notifyNodeStart(state.getExecutionId(), nodeId, state.getNodeType(nodeId), state.getStepIndex());
    }

    @Override
    public void afterNode(String nodeId, WorkflowState state, NodeExecutionResult result) {
        // 节点完成后自动记录日志（无需在节点处理器中手动记录）
        NodeExecutionLog log = new NodeExecutionLog();
        log.setExecutionId(state.getExecutionId());
        log.setNodeId(nodeId);
        log.setNodeType(state.getNodeType(nodeId));
        log.setInput(result.input());
        log.setOutput(result.output());
        log.setStatus(result.status().name());
        log.setDurationMs(result.durationMs());
        log.setErrorMessage(result.errorMessage());
        log.setExecutedAt(LocalDateTime.now());
        
        nodeExecutionLogMapper.insert(log);

        // 推送 SSE 状态
        streamService.notifyNodeComplete(state.getExecutionId(), nodeId, state.getNodeType(nodeId),
            state.getStepIndex(), result.output(), result.durationMs());
    }

    @Override
    public void onError(String nodeId, WorkflowState state, Exception error) {
        streamService.notifyNodeError(state.getExecutionId(), nodeId, state.getNodeType(nodeId),
            state.getStepIndex(), error.getMessage());
    }
}
```

### 使用 Checkpoint 实现状态持久化

LangGraph4j Checkpoint 自动保存工作流执行状态，支持暂停/恢复，**无需手动实现状态持久化**。

```java
@Configuration
public class CheckpointConfig {

    @Bean
    public CheckpointSaver<WorkflowState> checkpointSaver(WorkflowCheckpointMapper checkpointMapper) {
        // 使用 PostgreSQL 存储 checkpoint
        return new PostgresCheckpointSaver(checkpointMapper);
    }
}

// Checkpoint Entity
@Data
@TableName("workflow_checkpoint")
public class WorkflowCheckpoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String threadId;       // 执行 ID
    private String checkpointId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> state;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

// Checkpoint Mapper
public interface WorkflowCheckpointMapper extends BaseMapper<WorkflowCheckpoint> {
    default WorkflowCheckpoint findByThreadId(String threadId) {
        return selectOne(new LambdaQueryWrapper<WorkflowCheckpoint>()
            .eq(WorkflowCheckpoint::getThreadId, threadId)
            .orderByDesc(WorkflowCheckpoint::getCreatedAt)
            .last("LIMIT 1"));
    }
}

// Checkpoint 存储表（LangGraph4j 自动管理）
CREATE TABLE workflow_checkpoint (
    id BIGSERIAL PRIMARY KEY,
    thread_id VARCHAR(100) NOT NULL,      -- 执行 ID
    checkpoint_id VARCHAR(100) NOT NULL,
    state JSONB NOT NULL,                 -- 工作流状态
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_checkpoint_thread ON workflow_checkpoint(thread_id, created_at DESC);
```

**Checkpoint 应用场景**：
- 长时间执行的工作流：暂停后可恢复
- 对话智能体：保存对话状态，支持多轮对话
- 调试执行：保存中间状态，便于分析

### 执行流程（利用框架特性）

```
1. 接收 FlowGram.AI JSON
2. 解析节点和边
3. 构建 LangGraph4j StateGraph（动态节点和边）
4. 注册 Hook（自动日志记录 + SSE 推送）
5. 配置 Checkpoint（状态持久化）
6. 执行工作流
7. Hook 自动处理日志和状态推送，无需手动干预
```

---

## 第三部分：智能体发布服务设计

### 发布流程

```
1. 选择工作流 + 版本
2. 选择发布类型（对话/API）
3. 配置发布参数
4. 创建智能体实例
5. 注册路由/生成API Key
6. 激活智能体
```

### 对话智能体发布

```java
@Service
public class DialogAgentPublisher {

    private final WorkflowVersionMapper workflowVersionMapper;
    private final AgentMapper agentMapper;

    public PublishedDialogAgent publish(Long workflowId, Integer version, DialogPublishRequest request) {
        // 1. 加载工作流版本快照
        WorkflowVersion workflowVersion = workflowVersionMapper.findByWorkflowIdAndVersion(workflowId, version);
        
        // 2. 创建对话智能体
        Agent agent = new Agent();
        agent.setWorkflowId(workflowId);
        agent.setWorkflowVersion(version);
        agent.setName(request.name());
        agent.setType("DIALOG");
        agent.setStatus("ACTIVE");
        agent.setConfig(Map.of(
            "welcomeMessage", request.welcomeMessage(),
            "dialogConfig", request.dialogConfig()
        ));
        
        agentMapper.insert(agent);
        
        // 3. 注册 WebSocket 路由: /ws/agent/{agentId}/dialog
        
        return new PublishedDialogAgent(agent.getId(), "/ws/agent/" + agent.getId() + "/dialog");
    }
}

// 对话配置
public record DialogConfig(
    Integer maxHistoryLength,   // 最大对话历史长度
    Boolean enableMemory,       // 是否启用对话记忆
    String systemPrompt         // 系统提示词（覆盖工作流中的）
) {}
```

### API 智能体发布

```java
@Service
public class ApiAgentPublisher {

    private final WorkflowVersionMapper workflowVersionMapper;
    private final AgentMapper agentMapper;

    public PublishedApiAgent publish(Long workflowId, Integer version, ApiPublishRequest request) {
        // 1. 加载工作流版本快照
        // 2. 生成 API Key: sk-{uuid}
        String apiKey = "sk-" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        
        // 3. 创建 API 智能体
        Agent agent = new Agent();
        agent.setWorkflowId(workflowId);
        agent.setWorkflowVersion(version);
        agent.setName(request.name());
        agent.setType("API");
        agent.setStatus("ACTIVE");
        agent.setApiKey(apiKey);
        agent.setWebhookUrl(request.webhookUrl());
        agent.setTimeoutSeconds(request.timeoutSeconds());
        
        agentMapper.insert(agent);
        
        // 4. 注册 REST API 路由: POST /api/agent/{agentId}/execute
        
        return new PublishedApiAgent(agent.getId(), apiKey, "/api/agent/" + agent.getId() + "/execute");
    }
}

// API 发布请求
public record ApiPublishRequest(
    String name,
    String webhookUrl,
    Integer timeoutSeconds     // 默认 300 秒
) {}
```

### API 智能体执行接口

| API | 说明 |
|-----|------|
| `POST /api/agent/{agentId}/execute` | 同步执行，直接返回结果 |
| `POST /api/agent/{agentId}/execute-async` | 异步执行，返回 executionId |
| `GET /api/agent/{agentId}/execution/{execId}` | 查询异步执行结果 |

**认证方式**：请求头携带 `X-API-Key`

### Webhook 回调

```java
@Service
public class WebhookNotifier {

    public void notifyCompletion(WorkflowExecution execution, ApiAgent agent) {
        if (agent.webhookUrl() == null) return;
        
        WebhookPayload payload = new WebhookPayload(
            execution.id(),
            agent.id(),
            execution.status(),
            execution.output(),
            execution.durationMs()
        );
        
        // 异步发送回调
        httpClient.post(agent.webhookUrl(), payload)
            .timeout(10, TimeUnit.SECONDS)
            .execute();
    }
}

public record WebhookPayload(
    Long executionId,
    Long agentId,
    ExecutionStatus status,
    Map<String, Object> output,
    Long durationMs
) {}
```

---

## 第四部分：工具系统设计

### 工具定义结构

```java
public record ToolDefinition(
    String id,                   // 工具唯一标识
    String name,                 // 工具名称
    String description,          // 工具描述（LLM 可见）
    ToolCategory category,       // BUILTIN, EXTERNAL_API, MCP
    ToolConfig config,           // 工具配置
    List<ToolParameter> parameters  // 输入参数定义
) {}

public record ToolParameter(
    String name,
    String type,                 // string, number, boolean, object, array
    String description,
    Boolean required,
    Object defaultValue
) {}

public enum ToolCategory {
    BUILTIN,      // 内置工具
    EXTERNAL_API, // 外部 API
    MCP           // MCP 工具
}
```

### 配置文件格式（tools.yml）

```yaml
tools:
  # 内置工具
  - id: http_request
    name: HTTP Request
    description: 发送 HTTP 请求到指定 URL
    category: BUILTIN
    parameters:
      - name: url
        type: string
        description: 请求 URL
        required: true
      - name: method
        type: string
        description: HTTP 方法
        required: true
        default: GET
      - name: headers
        type: object
        description: 请求头
        required: false
      - name: body
        type: object
        description: 请求体
        required: false
    config:
      timeout_seconds: 30

  # 外部 API 工具
  - id: external_weather
    name: Weather API
    description: 查询天气信息
    category: EXTERNAL_API
    parameters:
      - name: city
        type: string
        description: 城市名称
        required: true
    config:
      url: https://api.weather.com/v1/query
      method: POST
      auth_type: api_key
      auth_key: X-API-Key
      auth_value: ${WEATHER_API_KEY}

  # MCP 工具
  - id: mcp_file_read
    name: Read File
    description: 读取文件内容
    category: MCP
    parameters:
      - name: path
        type: string
        description: 文件路径
        required: true
    config:
      mcp_server: filesystem
      mcp_tool: read_file
```

### 工具执行器

```java
@Service
public class ToolExecutor {

    private final Map<ToolCategory, ToolHandler> handlers;

    public ToolResult execute(ToolDefinition tool, Map<String, Object> params) {
        ToolHandler handler = handlers.get(tool.category());
        return handler.execute(tool, params);
    }
}
```

| 类别 | 处理器 | 执行方式 |
|------|--------|----------|
| BUILTIN | `BuiltinToolHandler` | 调用内置执行器（HttpRequestExecutor 等） |
| EXTERNAL_API | `ExternalApiToolHandler` | 发送 HTTP 请求到配置的 URL |
| MCP | `McpToolHandler` | 通过 MCP 客户端调用 |

### FlowGram.AI 工具节点数据结构

```json
{
  "id": "tool_0",
  "type": "Tool",
  "meta": { "position": { "x": 400, "y": 0 } },
  "data": {
    "toolId": "http_request",
    "name": "HTTP Request",
    "params": {
      "url": "{{start.input.apiUrl}}",
      "method": "POST",
      "body": { "query": "{{llm_0.output}}" }
    }
  }
}
```

---

## 第五部分：LLM 集成设计

### LLM 提供者配置

```java
public record LlmProvider(
    Long id,
    String name,                 // 提供者名称（如 "通义千问"、"本地Ollama"）
    String providerType,         // qwen, baidu, zhipu, ollama, custom
    String apiKey,               // API Key（加密存储）
    String baseUrl,              // API 地址
    Integer timeoutSeconds,
    Map<String, Object> options  // 额外配置
) {}

public record LlmModel(
    Long id,
    Long providerId,
    String modelId,              // 模型标识（如 qwen-max, llama3）
    String modelName,            // 模型显示名称
    Integer maxTokens,
    Double inputPrice,           // 输入价格（每千 Token）
    Double outputPrice,          // 输出价格（每千 Token）
    Map<String, Object> capabilities  // 能力标签：chat, completion, embedding
) {}
```

### 配置文件格式（llm-providers.yml）

```yaml
providers:
  # 通义千问
  - name: 通义千问
    provider_type: qwen
    api_key: ${QWEN_API_KEY}
    base_url: https://dashscope.aliyuncs.com/api/v1
    timeout_seconds: 60
    models:
      - model_id: qwen-max
        model_name: Qwen Max
        max_tokens: 6000
        input_price: 0.02
        output_price: 0.06

  # 文心一言
  - name: 文心一言
    provider_type: baidu
    api_key: ${BAIDU_API_KEY}
    base_url: https://aip.baidubce.com/rpc/2.0/ai_custom/v1
    models:
      - model_id: ernie-bot-4
        model_name: ERNIE Bot 4.0
        max_tokens: 2000

  # 本地 Ollama
  - name: Ollama
    provider_type: ollama
    base_url: http://localhost:11434
    timeout_seconds: 120
    models:
      - model_id: llama3
        model_name: Llama 3
        max_tokens: 4096
      - model_id: qwen2.5
        model_name: Qwen 2.5 Local
        max_tokens: 8192
```

### LangChain4j 模型适配

```java
@Configuration
public class LlmModelConfig {

    @Bean
    public ModelRegistry modelRegistry(LlmProviderMapper providerMapper, LlmModelMapper modelMapper) {
        List<LlmProvider> providers = providerMapper.selectList(null);
        Map<String, ChatLanguageModel> models = new HashMap<>();
        
        for (LlmProvider provider : providers) {
            List<LlmModel> providerModels = modelMapper.selectList(
                new LambdaQueryWrapper<LlmModel>()
                    .eq(LlmModel::getProviderId, provider.getId())
            );
            for (LlmModel model : providerModels) {
                ChatLanguageModel chatModel = createChatModel(provider, model);
                models.put(model.getModelId(), chatModel);
            }
        }
        
        return new ModelRegistry(models);
    }

    private ChatLanguageModel createChatModel(LlmProvider provider, LlmModel model) {
        return switch (provider.getProviderType()) {
            case "qwen" -> createQwenModel(provider, model);
            case "baidu" -> createBaiduModel(provider, model);
            case "ollama" -> createOllamaModel(provider, model);
            default -> createCustomModel(provider, model);
        };
    }
}
```

### FlowGram.AI LLM 节点数据结构

```json
{
  "id": "llm_0",
  "type": "LLM",
  "meta": { "position": { "x": 200, "y": 0 } },
  "data": {
    "modelId": "qwen-max",
    "systemPrompt": "你是一个智能助手，请根据用户输入进行分析。",
    "userPrompt": "{{start.input.question}}",
    "temperature": 0.7,
    "maxTokens": 2000
  }
}
```

---

## 第六部分：条件分支与并行执行设计

### 条件分支节点

#### FlowGram.AI 条件节点数据结构

```json
{
  "id": "condition_0",
  "type": "Condition",
  "meta": { "position": { "x": 300, "y": 0 } },
  "data": {
    "conditions": [
      {
        "id": "branch_0",
        "name": "分支A",
        "expression": "{{llm_0.output.score}} >= 80",
        "targetNodeID": "node_success"
      },
      {
        "id": "branch_1",
        "name": "分支B",
        "expression": "{{llm_0.output.score}} < 80",
        "targetNodeID": "node_retry"
      }
    ],
    "defaultTargetNodeID": "node_default"
  }
}
```

#### 条件表达式解析

支持的比较运算：`==`, `!=`, `>`, `<`, `>=`, `<=`
支持的逻辑运算：`&&`, `||`, `!`
支持变量引用：`{{node_id.output.field}}`

### 并行执行节点

#### FlowGram.AI 并行节点数据结构

```json
{
  "id": "parallel_0",
  "type": "Parallel",
  "meta": { "position": { "x": 400, "y": 0 } },
  "data": {
    "branches": [
      { "id": "parallel_branch_0", "name": "任务A", "startNodeId": "task_a" },
      { "id": "parallel_branch_1", "name": "任务B", "startNodeId": "task_b" }
    ],
    "waitStrategy": "ALL",          // ALL: 等待所有完成, ANY: 任一完成即返回
    "timeoutSeconds": 60
  }
}
```

#### 并行执行实现

使用 Java 21 Virtual Threads 并行执行各分支：
- `waitStrategy = ALL`：等待所有分支完成，收集全部结果
- `waitStrategy = ANY`：任一分支完成即返回，可取消其他任务

---

## 第七部分：对话智能体聊天交互设计

### WebSocket 通信协议

路径：`ws://{host}/ws/agent/{agentId}/dialog`

### 对话消息格式

```java
// 客户端发送消息
public record DialogInputMessage(
    String type,                // text, event, command
    String content,             // 消息内容
    Map<String, Object> metadata // 元数据（可选）
) {}

// 服务端响应消息
public record DialogOutputMessage(
    String type,                // text, workflow_start, workflow_node, workflow_end, error
    String content,             // 消息内容
    String nodeId,              // 当前节点 ID（可选）
    Map<String, Object> data,   // 附加数据
    LocalDateTime timestamp
) {}
```

### WebSocket 消息示例

```json
// 客户端发送用户消息
{
  "type": "text",
  "content": "请帮我分析这段代码的性能问题"
}

// 服务端响应 - 工作流开始
{
  "type": "workflow_start",
  "content": "开始处理您的请求...",
  "data": { "executionId": "exec_123" }
}

// 服务端响应 - LLM 流式内容
{
  "type": "text",
  "content": "根据分析，我发现以下性能问题...",
  "nodeId": "llm_0",
  "data": { "streaming": true }
}

// 服务端响应 - 工作流完成
{
  "type": "workflow_end",
  "data": { 
    "executionId": "exec_123",
    "durationMs": 5000,
    "output": { "result": "..." }
  }
}
```

### 对话会话管理

```java
public record DialogSession(
    String sessionId,
    Long agentId,
    Long workflowId,
    Integer workflowVersion,
    List<DialogHistory> history,
    Map<String, Object> variables,
    LocalDateTime createdAt
) {}

public record DialogHistory(
    String role,      // user, assistant, system
    String content,
    LocalDateTime timestamp
) {}
```

---

## 第八部分：调试功能设计

### 调试数据模型

```java
public record DebugExecution(
    Long id,
    Long workflowId,
    Integer workflowVersion,
    DebugMode mode,              // SINGLE_NODE, FULL
    String targetNodeId,         // 单节点调试时的目标节点
    Map<String, Object> input,
    Map<String, Object> output,
    ExecutionStatus status,
    Long durationMs,
    LocalDateTime createdAt
) {}

public record DebugNodeLog(
    Long id,
    Long debugExecutionId,
    String nodeId,
    String nodeType,
    Integer stepIndex,           // 执行步骤序号
    Map<String, Object> input,
    Map<String, Object> output,
    ExecutionStatus status,
    Long durationMs,
    String errorMessage,
    LocalDateTime executedAt
) {}
```

### 调试 API

| API | 说明 |
|-----|------|
| `POST /workflows/{id}/debug/node/{nodeId}` | 单节点调试 |
| `POST /workflows/{id}/debug/full` | 完整调试 |
| `GET /workflows/{id}/debug/history` | 调试历史列表 |
| `GET /workflows/{id}/debug/execution/{execId}` | 单次调试详情 |
| `GET /workflows/{id}/debug/execution/{execId}/nodes` | 调试节点日志 |

### SSE 实时状态推送

订阅路径：`GET /workflows/{workflowId}/debug/execution/{debugExecutionId}/stream`

#### SSE 事件类型

| 事件类型 | 说明 |
|----------|------|
| `EXECUTION_START` | 执行开始 |
| `NODE_START` | 节点开始执行 |
| `NODE_COMPLETE` | 节点执行完成 |
| `NODE_ERROR` | 节点执行错误 |
| `EXECUTION_COMPLETE` | 执行完成 |
| `EXECUTION_ERROR` | 执行错误 |

#### SSE 事件数据结构

```java
public record SseEvent(
    SseEventType type,
    Long debugExecutionId,
    String nodeId,
    String nodeType,
    Integer stepIndex,
    ExecutionStatus status,
    Map<String, Object> data,
    Long durationMs,
    String message,
    LocalDateTime timestamp
) {}
```

#### SSE 消息示例

```
event: EXECUTION_START
data: {"debugExecutionId":123,"status":"RUNNING","message":"已连接"}

event: NODE_START
data: {"nodeId":"llm_0","nodeType":"LLM","stepIndex":1,"status":"RUNNING"}

event: NODE_COMPLETE
data: {"nodeId":"llm_0","nodeType":"LLM","stepIndex":1,"status":"SUCCESS","output":{"content":"..."},"durationMs":2000}

event: EXECUTION_COMPLETE
data: {"status":"SUCCESS","output":{"result":"..."},"durationMs":3000}
```

#### 前端订阅示例

```javascript
const eventSource = new EventSource(`/workflows/${workflowId}/debug/execution/${debugExecutionId}/stream`);

eventSource.addEventListener('NODE_START', (e) => {
    const data = JSON.parse(e.data);
    // 高亮当前节点
});

eventSource.addEventListener('NODE_COMPLETE', (e) => {
    const data = JSON.parse(e.data);
    // 显示节点输出
});

eventSource.addEventListener('EXECUTION_COMPLETE', (e) => {
    const data = JSON.parse(e.data);
    eventSource.close();
});
```

---

## 第九部分：项目结构与技术细节

### 项目目录结构

```
workflow-engine/
├── src/main/java/com/example/workflow/
│   ├── WorkflowEngineApplication.java
│   │
│   ├── controller/                     # API 层
│   │   ├── WorkflowController.java
│   │   ├── WorkflowVersionController.java
│   │   ├── AgentController.java
│   │   ├── ApiAgentExecuteController.java
│   │   ├── DebugController.java
│   │   └── ToolController.java
│   │
│   ├── service/                        # 业务逻辑层
│   │   ├── WorkflowService.java
│   │   ├── WorkflowVersionService.java
│   │   ├── AgentService.java
│   │   ├── DialogAgentPublisher.java
│   │   ├── ApiAgentPublisher.java
│   │   ├── DialogSessionManager.java
│   │   ├── DebugService.java
│   │   ├── DebugStreamService.java
│   │   ├── ToolRegistry.java
│   │   └── WebhookNotifier.java
│   │
│   ├── engine/                         # 工作流引擎核心
│   │   ├── WorkflowExecutor.java
│   │   ├── WorkflowRouter.java
│   │   ├── WorkflowContext.java
│   │   ├── NodeExecutionResult.java
│   │   └── WorkflowExecutionListener.java
│   │   │
│   │   ├── node/                       # 节点处理器
│   │   │   ├── NodeHandler.java
│   │   │   ├── LLMNodeHandler.java
│   │   │   ├── ToolNodeHandler.java
│   │   │   ├── ConditionNodeHandler.java
│   │   │   ├── ParallelNodeHandler.java
│   │   │   ├── StartNodeHandler.java
│   │   │   └── EndNodeHandler.java
│   │   │
│   │   └── expression/                 # 表达式解析
│   │       ├── ConditionExpressionParser.java
│   │       └── VariableResolver.java
│   │
│   ├── llm/                            # LLM 集成
│   │   ├── ModelRegistry.java
│   │   ├── LlmProvider.java
│   │   ├── LlmModel.java
│   │   └── provider/
│   │       ├── QwenModelAdapter.java
│   │       ├── BaiduModelAdapter.java
│   │       ├── OllamaModelAdapter.java
│   │       └── CustomModelAdapter.java
│   │
│   ├── tool/                           # 工具系统
│   │   ├── ToolDefinition.java
│   │   ├── ToolExecutor.java
│   │   ├── ToolHandler.java
│   │   ├── handler/
│   │   │   ├── BuiltinToolHandler.java
│   │   │   ├── ExternalApiToolHandler.java
│   │   │   └── McpToolHandler.java
│   │   └── builtin/
│   │       ├── HttpRequestExecutor.java
│   │       ├── JsonParseExecutor.java
│   │       └── TextProcessExecutor.java
│   │
│   ├── websocket/                      # WebSocket 对话
│   │   ├── DialogAgentHandler.java
│   │   ├── DialogInputMessage.java
│   │   ├── DialogOutputMessage.java
│   │   └── DialogExecutionListener.java
│   │
│   ├── model/                          # 领域模型（MyBatis-Plus Entity）
│   ├── mapper/                         # 数据访问层（MyBatis-Plus Mapper）
│   │   ├── WorkflowMapper.java
│   │   ├── WorkflowVersionMapper.java
│   │   ├── WorkflowCheckpointMapper.java
│   │   ├── AgentMapper.java
│   │   ├── WorkflowExecutionMapper.java
│   │   ├── NodeExecutionLogMapper.java
│   │   ├── DebugExecutionMapper.java
│   │   ├── DebugNodeLogMapper.java
│   │   ├── LlmProviderMapper.java
│   │   └── LlmModelMapper.java
│   ├── dto/                            # 数据传输对象
│   ├── config/                         # 配置类
│   └── exception/                      # 异常处理
│
├── src/main/resources/
│   ├── application.yml
│   ├── tools.yml
│   ├── llm-providers.yml
│   └── db/migration/                   # Flyway 数据库迁移
│
└── src/test/java/                      # 测试代码
```

### 核心依赖

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.3.0</spring-boot.version>
    <langchain4j.version>1.12.2</langchain4j.version>
    <langgraph4j.version>1.6.2</langgraph4j.version>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
</properties>

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
    
    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>
    
    <!-- LangChain4j BOM -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>dev.langchain4j</groupId>
                <artifactId>langchain4j-bom</artifactId>
                <version>${langchain4j.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <!-- LangChain4j Core -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
    </dependency>
    
    <!-- LangGraph4j BOM -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.bsc.langgraph4j</groupId>
                <artifactId>langgraph4j-bom</artifactId>
                <version>${langgraph4j.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <!-- LangGraph4j Core -->
    <dependency>
        <groupId>org.bsc.langgraph4j</groupId>
        <artifactId>langgraph4j-core</artifactId>
    </dependency>
    
    <!-- 数据库 -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- Flyway 数据库迁移 -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    
    <!-- 其他工具 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-yaml</artifactId>
    </dependency>
</dependencies>
```

| 依赖 | 版本 | 用途 |
|------|------|------|
| spring-boot-starter-web | 3.3.0 | Web API |
| spring-boot-starter-websocket | 3.3.0 | WebSocket 对话 |
| mybatis-plus-spring-boot3-starter | 3.5.7 | 数据交互（MyBatis-Plus） |
| langchain4j | 1.12.2 | LLM 集成 |
| langgraph4j-core | 1.6.2 | 工作流编排 |
| postgresql | - | 数据库 |
| flyway-core | - | 数据库迁移 |
| jackson-dataformat-yaml | - | YAML 配置解析 |
| lombok | - | 代码简化 |

### MyBatis-Plus 配置

```java
@Configuration
@MapperScan("com.example.workflow.mapper")
public class MybatisPlusConfig {

    // 分页插件
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    // 自动填充处理器
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

### API 汇总

| 模块 | API | 说明 |
|------|-----|------|
| **工作流** | `POST /workflows` | 创建工作流 |
| | `GET /workflows/{id}` | 获取工作流详情 |
| | `PUT /workflows/{id}` | 更新工作流（自动创建新版本） |
| | `DELETE /workflows/{id}` | 删除工作流 |
| | `POST /workflows/{id}/versions` | 保存新版本 |
| | `GET /workflows/{id}/versions` | 获取版本列表 |
| | `POST /workflows/{id}/rollback/{v}` | 回滚到指定版本 |
| **智能体** | `POST /agents/dialog` | 发布对话智能体 |
| | `POST /agents/api` | 发布 API 智能体 |
| | `GET /agents/{id}` | 获取智能体详情 |
| | `PUT /agents/{id}/status` | 更新智能体状态 |
| | `DELETE /agents/{id}` | 删除智能体 |
| **API智能体执行** | `POST /api/agent/{agentId}/execute` | 同步执行 |
| | `POST /api/agent/{agentId}/execute-async` | 异步执行 |
| | `GET /api/agent/{agentId}/execution/{execId}` | 查询执行结果 |
| **对话智能体** | `WS /ws/agent/{agentId}/dialog` | WebSocket 对话通道 |
| **调试** | `POST /workflows/{id}/debug/node/{nodeId}` | 单节点调试 |
| | `POST /workflows/{id}/debug/full` | 完整调试 |
| | `GET /workflows/{id}/debug/history` | 调试历史列表 |
| | `GET /workflows/{id}/debug/execution/{execId}` | 单次调试详情 |
| | `GET /workflows/{id}/debug/execution/{execId}/nodes` | 调试节点日志 |
| | `GET /workflows/{id}/debug/execution/{execId}/stream` | SSE 实时状态推送 |
| **工具** | `GET /tools` | 获取工具列表 |
| | `GET /tools/{id}` | 获取工具详情 |

---

## 数据库表结构

```sql
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

-- 工作流版本快照
CREATE TABLE workflow_version (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL REFERENCES workflow(id),
    version INT NOT NULL,
    canvas JSONB NOT NULL,          -- FlowGram.AI JSON
    change_note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_workflow_version ON workflow_version(workflow_id, version);

-- 智能体定义
CREATE TABLE agent (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL REFERENCES workflow(id),
    workflow_version INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,      -- DIALOG, API
    status VARCHAR(20) DEFAULT 'ACTIVE',
    config JSONB,                   -- 类型特定配置
    api_key VARCHAR(50),            -- API 智能体的 key
    webhook_url VARCHAR(255),       -- API 智能体的回调地址
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 工作流执行记录
CREATE TABLE workflow_execution (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    agent_id BIGINT REFERENCES agent(id),
    status VARCHAR(20) NOT NULL,
    input JSONB,
    output JSONB,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms BIGINT
);

-- 节点执行日志
CREATE TABLE node_execution_log (
    id BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL REFERENCES workflow_execution(id),
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

-- 调试执行记录
CREATE TABLE debug_execution (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    workflow_version INT NOT NULL,
    mode VARCHAR(20) NOT NULL,      -- SINGLE_NODE, FULL
    target_node_id VARCHAR(50),
    input JSONB,
    output JSONB,
    status VARCHAR(20) NOT NULL,
    duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_debug_execution_workflow ON debug_execution(workflow_id, created_at DESC);

-- 调试节点日志
CREATE TABLE debug_node_log (
    id BIGSERIAL PRIMARY KEY,
    debug_execution_id BIGINT NOT NULL REFERENCES debug_execution(id),
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

-- LLM 模型配置
CREATE TABLE llm_model (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES llm_provider(id),
    model_id VARCHAR(100) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    max_tokens INT,
    input_price DECIMAL(10, 4),
    output_price DECIMAL(10, 4),
    capabilities JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 执行流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                        工作流执行流程                            │
└─────────────────────────────────────────────────────────────────┘

用户请求 → API层 → Service层 → WorkflowExecutor
                                    │
                                    ▼
                         ┌──────────────────┐
                         │ 解析 FlowGram.AI │
                         │     JSON         │
                         └──────────────────┘
                                    │
                                    ▼
                         ┌──────────────────┐
                         │ 构建 StateGraph  │
                         │   (LangGraph4j)  │
                         └──────────────────┘
                                    │
                                    ▼
                         ┌──────────────────┐
                         │ 初始化 Context   │
                         │  (变量、输入)    │
                         └──────────────────┘
                                    │
                                    ▼
              ┌─────────────────────────────────────┐
              │         按图结构执行节点             │
              │                                     │
              │  ┌─────┐   ┌─────┐   ┌─────┐       │
              │  │Start│ → │ LLM │ → │Tool │ ...  │
              │  └─────┘   └─────┘   └─────┘       │
              │                                     │
              │  每个节点:                          │
              │  1. NodeHandler.execute()          │
              │  2. 记录执行日志                    │
              │  3. 推送 SSE 状态                   │
              └─────────────────────────────────────┘
                                    │
                                    ▼
                         ┌──────────────────┐
                         │ 返回最终结果     │
                         │ (output + logs)  │
                         └──────────────────┘
```

---

## 下一步：实现计划

本设计文档已完成，下一步将调用 `writing-plans` 技能创建详细的实现计划。