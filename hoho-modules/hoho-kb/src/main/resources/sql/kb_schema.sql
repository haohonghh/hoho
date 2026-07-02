CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS kb_category
(
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT      NOT NULL DEFAULT 0,
    name        VARCHAR(128) NOT NULL,
    sort        INTEGER     NOT NULL DEFAULT 0,
    status      VARCHAR(16) NOT NULL DEFAULT 'enabled',
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kb_qa
(
    id                BIGSERIAL PRIMARY KEY,
    category_id       BIGINT       NOT NULL,
    question          TEXT         NOT NULL,
    answer            TEXT         NOT NULL,
    similar_questions TEXT,
    status            VARCHAR(16)  NOT NULL DEFAULT 'draft',
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kb_qa_category ON kb_qa (category_id);
CREATE INDEX IF NOT EXISTS idx_kb_qa_status ON kb_qa (status);

CREATE TABLE IF NOT EXISTS kb_embedding
(
    id           BIGSERIAL PRIMARY KEY,
    qa_id        BIGINT       NOT NULL REFERENCES kb_qa (id) ON DELETE CASCADE,
    content_type VARCHAR(32)  NOT NULL,
    content      TEXT         NOT NULL,
    embedding    vector(1024) NOT NULL,
    model        VARCHAR(64)  NOT NULL,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kb_embedding_qa ON kb_embedding (qa_id);

-- MVP 阶段使用精确向量检索，不创建 HNSW 索引。
-- bge-m3 的 1024 维 vector 在部分 pgvector/PostgreSQL 组合下创建 HNSW 后，
-- 插入时可能触发 "index row size exceeds btree version 4 maximum"。
-- 数据量较小时精确检索足够稳定，后续可再切 halfvec / IVFFlat / 降维索引方案。
DROP INDEX IF EXISTS idx_kb_embedding_vector_hnsw;
