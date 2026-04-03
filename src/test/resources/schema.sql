-- Schema for H2 test database

CREATE TABLE IF NOT EXISTS workflow (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 工作流版本表
CREATE TABLE IF NOT EXISTS workflow_version (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    version INTEGER NOT NULL,
    canvas TEXT,
    change_note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 智能体表
CREATE TABLE IF NOT EXISTS agent (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    workflow_version INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'INACTIVE',
    config TEXT,
    api_key VARCHAR(50),
    webhook_url VARCHAR(255),
    timeout_seconds INT DEFAULT 300,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 工作流执行记录表
CREATE TABLE IF NOT EXISTS workflow_execution (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    agent_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    input TEXT,
    output TEXT,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms BIGINT
);

-- LLM 提供商表
CREATE TABLE IF NOT EXISTS llm_provider (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    api_key VARCHAR(255),
    base_url VARCHAR(255) NOT NULL,
    timeout_seconds INT DEFAULT 60,
    options TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- LLM 模型配置表
CREATE TABLE IF NOT EXISTS llm_model (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    model_id VARCHAR(100) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    max_tokens INT,
    input_price DECIMAL(10, 4),
    output_price DECIMAL(10, 4),
    capabilities TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);