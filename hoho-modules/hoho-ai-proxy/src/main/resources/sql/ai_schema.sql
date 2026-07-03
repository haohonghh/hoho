CREATE TABLE IF NOT EXISTS ai_long_term_memory
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    conversation_id VARCHAR(64),
    memory_type     VARCHAR(64)  NOT NULL,
    memory_key      VARCHAR(128) NOT NULL,
    memory_value    TEXT         NOT NULL,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_long_term_memory_user_type_key
    ON ai_long_term_memory (user_id, memory_type, memory_key);

CREATE INDEX IF NOT EXISTS idx_ai_long_term_memory_user
    ON ai_long_term_memory (user_id);

-- 该表用于 AI 域长期记忆，当前阶段与 kb_* 表共用 hoho_kb 数据库，
-- 通过 ai_* / kb_* 前缀区分职责，不单独拆分新数据库。
