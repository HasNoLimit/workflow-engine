package com.workflow.engine.executor;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channels;

import java.util.*;

/**
 * 工作流智能体状态
 * <p>
 * 继承 LangGraph4j AgentState，添加工作流特定字段。
 * 用于在 LangGraph4j StateGraph 执行过程中传递和修改状态。
 * </p>
 */
public class WorkflowAgentState extends AgentState {

    // ==================== 状态字段常量 ====================

    /** 工作流ID */
    public static final String WORKFLOW_ID = "workflowId";

    /** 执行ID */
    public static final String EXECUTION_ID = "executionId";

    /** 当前节点ID */
    public static final String CURRENT_NODE = "currentNode";

    /** 输入数据 */
    public static final String INPUT = "input";

    /** 输出数据 */
    public static final String OUTPUT = "output";

    /** 执行状态 */
    public static final String STATUS = "status";

    /** 错误信息 */
    public static final String ERROR = "error";

    /** 对话历史 */
    public static final String CHAT_HISTORY = "chatHistory";

    /** 执行路径 */
    public static final String EXECUTION_PATH = "executionPath";

    /** 变量存储 */
    public static final String VARIABLES = "variables";

    /** 条件分支结果 */
    public static final String CONDITION_BRANCH = "conditionBranch";

    // ==================== 状态常量 ====================

    /** 执行状态：运行中 */
    public static final String STATUS_RUNNING = "RUNNING";

    /** 执行状态：成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /** 执行状态：失败 */
    public static final String STATUS_FAILED = "FAILED";

    /** 执行状态：暂停 */
    public static final String STATUS_PAUSED = "PAUSED";

    // ==================== SCHEMA 定义 ====================

    /**
     * 状态 Schema 定义
     * <p>
     * 只需要定义需要追加模式的列表字段。
     * 其他字段使用默认的替换模式，不需要在 Schema 中定义。
     * </p>
     */
    public static final Map<String, ?> SCHEMA = Map.of(
            // 列表类型：追加模式（新数据追加到现有列表末尾）
            CHAT_HISTORY, Channels.appender(ArrayList::new),
            EXECUTION_PATH, Channels.appender(ArrayList::new)
    );

    // ==================== 构造函数 ====================

    /**
     * 构造工作流状态
     * @param initData 初始数据
     */
    public WorkflowAgentState(Map<String, Object> initData) {
        super(initData);
    }

    // ==================== 便捷访问方法 ====================

    /**
     * 获取工作流ID
     * @return 工作流ID
     */
    public Optional<Long> workflowId() {
        return value(WORKFLOW_ID);
    }

    /**
     * 获取执行ID
     * @return 执行ID
     */
    public Optional<Long> executionId() {
        return value(EXECUTION_ID);
    }

    /**
     * 获取当前节点ID
     * @return 当前节点ID
     */
    public Optional<String> currentNode() {
        return value(CURRENT_NODE);
    }

    /**
     * 获取输入数据
     * @return 输入数据
     */
    public Optional<Map<String, Object>> input() {
        return value(INPUT);
    }

    /**
     * 获取输出数据
     * @return 输出数据
     */
    public Optional<Map<String, Object>> output() {
        return value(OUTPUT);
    }

    /**
     * 获取执行状态
     * @return 执行状态
     */
    public Optional<String> status() {
        return value(STATUS);
    }

    /**
     * 获取错误信息
     * @return 错误信息
     */
    public Optional<String> error() {
        return value(ERROR);
    }

    /**
     * 获取对话历史
     * @return 对话历史
     */
    public Optional<List<Map<String, String>>> chatHistory() {
        return value(CHAT_HISTORY);
    }

    /**
     * 获取执行路径
     * @return 执行路径
     */
    public Optional<List<String>> executionPath() {
        return value(EXECUTION_PATH);
    }

    /**
     * 获取变量存储
     * @return 变量存储
     */
    public Optional<Map<String, Object>> variables() {
        return value(VARIABLES);
    }

    /**
     * 获取变量值
     * @param key 变量名
     * @return 变量值
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getVariable(String key) {
        return variables()
                .map(vars -> (T) vars.get(key));
    }

    /**
     * 获取条件分支结果
     * @return 条件分支结果
     */
    public Optional<String> conditionBranch() {
        return value(CONDITION_BRANCH);
    }

    /**
     * 获取状态数据（用于 invoke 方法）
     * @return 状态数据 Map
     */
    public Map<String, Object> toMap() {
        return this.data();
    }

    /**
     * 创建初始状态数据
     * @param workflowId 工作流ID
     * @param executionId 执行ID
     * @param input 输入数据
     * @return 初始状态数据 Map
     */
    public static Map<String, Object> createInitialData(Long workflowId, Long executionId, Map<String, Object> input) {
        Map<String, Object> initData = new HashMap<>();
        initData.put(WORKFLOW_ID, workflowId);
        initData.put(EXECUTION_ID, executionId);
        initData.put(INPUT, input != null ? input : new HashMap<>());
        initData.put(OUTPUT, new HashMap<>());
        initData.put(STATUS, STATUS_RUNNING);
        initData.put(CHAT_HISTORY, new ArrayList<>());
        initData.put(EXECUTION_PATH, new ArrayList<>());
        initData.put(VARIABLES, new HashMap<>());

        // 将输入数据添加到变量存储
        if (input != null) {
            Map<String, Object> vars = new HashMap<>();
            vars.putAll(input);
            initData.put(VARIABLES, vars);
        }

        return initData;
    }

    /**
     * 创建初始状态对象
     * @param workflowId 工作流ID
     * @param executionId 执行ID
     * @param input 输入数据
     * @return 初始状态对象
     */
    public static WorkflowAgentState createInitialState(Long workflowId, Long executionId, Map<String, Object> input) {
        return new WorkflowAgentState(createInitialData(workflowId, executionId, input));
    }
}