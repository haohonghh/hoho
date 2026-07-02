# hoho-bot 冒烟测试

这份文档用于验证 `hoho-bot` 第一版文本问答闭环：

```text
用户问题 -> hoho-bot -> hoho-kb 检索 -> 命中 FAQ 直接回答
用户问题 -> hoho-bot -> hoho-kb 未命中 -> hoho-ai-proxy 兜底回答
用户问题 -> hoho-bot -> MyBatis 保存会话和消息 -> 前端查询历史记录
```

## 1. 前置条件

确保以下服务已经启动：

```text
Nacos
hoho-ai-proxy
hoho-kb
hoho-bot
```

确认 `hoho-ai-proxy` 可用：

```bash
curl http://localhost:9205/ai/health
```

确认 `hoho-kb` 可用：

```bash
curl http://localhost:9204/kb/health
```

确认 `hoho-kb` 已经至少发布了一条 FAQ，并且混合检索能命中：

```bash
curl -X POST http://localhost:9204/kb/search/hybrid \
  -H "Content-Type: application/json" \
  -d '{
    "query": "电脑突然不能上网了怎么办？",
    "topK": 3
  }'
```

## 2. 初始化数据库表

在 AI 域数据库 `hoho_kb` 执行：

```sql
CREATE TABLE IF NOT EXISTS bot_conversation (
  id              BIGSERIAL    PRIMARY KEY,
  conversation_id VARCHAR(64)  NOT NULL,
  title           VARCHAR(100) NOT NULL,
  last_message    VARCHAR(500),
  message_count   INT          NOT NULL DEFAULT 0,
  create_time     TIMESTAMP    NOT NULL,
  update_time     TIMESTAMP    NOT NULL,
  CONSTRAINT uk_bot_conversation_id UNIQUE (conversation_id)
);

CREATE INDEX IF NOT EXISTS idx_bot_conversation_update_time
  ON bot_conversation (update_time);

COMMENT ON TABLE bot_conversation IS '机器人会话表';
COMMENT ON COLUMN bot_conversation.conversation_id IS '会话编号';
COMMENT ON COLUMN bot_conversation.title IS '会话标题';
COMMENT ON COLUMN bot_conversation.last_message IS '最后一条消息摘要';
COMMENT ON COLUMN bot_conversation.message_count IS '消息数量';

CREATE TABLE IF NOT EXISTS bot_message (
  id              BIGSERIAL    PRIMARY KEY,
  conversation_id VARCHAR(64)  NOT NULL,
  role            VARCHAR(20)  NOT NULL,
  content         TEXT         NOT NULL,
  source          VARCHAR(30),
  score           DOUBLE PRECISION,
  create_time     TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bot_message_conversation_id
  ON bot_message (conversation_id);

CREATE INDEX IF NOT EXISTS idx_bot_message_create_time
  ON bot_message (create_time);

COMMENT ON TABLE bot_message IS '机器人消息表';
COMMENT ON COLUMN bot_message.conversation_id IS '会话编号';
COMMENT ON COLUMN bot_message.role IS '消息角色';
COMMENT ON COLUMN bot_message.content IS '消息内容';
COMMENT ON COLUMN bot_message.source IS '回答来源';
COMMENT ON COLUMN bot_message.score IS '命中分数';
```

## 3. Nacos 配置

在 Nacos 新建配置：

```text
Data ID: hoho-bot-dev.yml
Group: DEFAULT_GROUP
Format: YAML
```

内容参考：

```yaml
spring:
  datasource:
    druid:
      stat-view-servlet:
        enabled: true
        loginUsername: hoho
        loginPassword: 123456
    dynamic:
      druid:
        initial-size: 5
        min-idle: 2
        maxActive: 10
        maxWait: 60000
        connectTimeout: 30000
        socketTimeout: 60000
        timeBetweenEvictionRunsMillis: 60000
        minEvictableIdleTimeMillis: 300000
        validationQuery: SELECT 1
        testWhileIdle: true
        testOnBorrow: false
        testOnReturn: false
        poolPreparedStatements: true
        maxPoolPreparedStatementPerConnectionSize: 20
        filters: stat,slf4j
        connectionProperties: druid.stat.mergeSql\=true;druid.stat.slowSqlMillis\=5000
      datasource:
        master:
          driver-class-name: org.postgresql.Driver
          url: jdbc:postgresql://localhost:5432/hoho_kb
          username: postgres
          password: postgres

mybatis:
  typeAliasesPackage: com.hoho.bot.domain
  mapperLocations: classpath:mapper/**/*.xml

hoho:
  bot:
    kb:
      base-url: http://localhost:9204
    ai-proxy:
      base-url: http://localhost:9205
    answer:
      direct-min-score: 0.65
      assist-min-score: 0.5
      top-k: 3
      fallback-system-prompt: 你是企业IT运维智能客服助手。知识库未命中时，请给出简洁、谨慎、可执行的建议，并提醒用户必要时联系人工运维。
```

## 4. 启动 hoho-bot

```bash
cd /Users/haohongguan/AGI/spring_ai/shsnc_ai/hoho
mvn -pl hoho-modules/hoho-bot spring-boot:run
```

健康检查：

```bash
curl http://localhost:9201/bot/health
```

期望返回：

```json
{
  "code": 200,
  "data": {
    "service": "hoho-bot",
    "status": "UP"
  }
}
```

## 5. 测试知识库命中

请求：

```bash
curl -X POST http://localhost:9201/bot/chat \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "test-session-001",
    "message": "我的电脑突然不能上网了，怎么处理？",
    "topK": 3
  }'
```

如果知识库分数大于等于 `hoho.bot.answer.direct-min-score`，期望返回：

```json
{
  "code": 200,
  "data": {
    "conversationId": "test-session-001",
    "answer": "请先检查网线或 Wi-Fi 是否连接...",
    "source": "kb",
    "score": 0.8,
    "references": [
      {
        "qaId": 1,
        "question": "电脑无法联网怎么办？",
        "answer": "请先检查网线或 Wi-Fi 是否连接...",
        "score": 0.8,
        "source": "vector"
      }
    ]
  }
}
```

## 6. 测试知识库辅助生成

如果知识库分数低于 `direct-min-score` 且大于等于 `assist-min-score`，`hoho-bot` 会把最相近的知识库问答传给大模型生成答案。

这种情况期望返回：

```json
{
  "code": 200,
  "data": {
    "conversationId": "test-session-001",
    "answer": "请按以下步骤排查...",
    "source": "ai_with_kb",
    "score": 0.62,
    "references": [
      {
        "qaId": 1,
        "question": "电脑无法联网怎么办？",
        "answer": "请先检查网线或 Wi-Fi 是否连接...",
        "score": 0.62,
        "source": "hybrid"
      }
    ]
  }
}
```

你之前测到的 `score=0.6385`，如果按示例阈值配置，会进入这个分支。

## 7. 测试 AI 兜底

使用一个知识库大概率没有的问题：

```bash
curl -X POST http://localhost:9201/bot/chat \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "test-session-002",
    "message": "帮我解释一下量子纠缠和运维工单有什么关系？",
    "topK": 3
  }'
```

如果知识库未命中，期望返回：

```json
{
  "code": 200,
  "data": {
    "conversationId": "test-session-002",
    "answer": "...",
    "source": "ai",
    "score": null,
    "references": []
  }
}
```

## 8. 查询历史会话

查询会话列表：

```bash
curl http://localhost:9201/bot/conversation/list
```

查询某个会话的消息列表：

```bash
curl http://localhost:9201/bot/conversation/test-session-001/messages
```

期望返回用户消息和机器人消息，机器人消息中会包含 `source` 和 `score`。

## 9. 常见问题

### 一直返回 source=ai

说明知识库检索分数低于 `assist-min-score`，可以临时调低：

```yaml
hoho:
  bot:
    answer:
      assist-min-score: 0.4
```

### 想让 0.6 左右的结果直接返回知识库答案

可以调低直接命中阈值：

```yaml
hoho:
  bot:
    answer:
      direct-min-score: 0.6
```

兼容说明：旧配置 `min-score` 仍然可用，含义等同于 `direct-min-score`。

### hoho-bot 调不到 hoho-kb

确认 Nacos 配置：

```yaml
hoho:
  bot:
    kb:
      base-url: http://localhost:9204
```

如果服务运行在 Docker 容器内，需要改成容器可访问的地址。

### hoho-bot 调不到 hoho-ai-proxy

确认 Nacos 配置：

```yaml
hoho:
  bot:
    ai-proxy:
      base-url: http://localhost:9205
```

也可以单独验证：

```bash
curl -X POST http://localhost:9205/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "测试一下"
  }'
```
