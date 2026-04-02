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