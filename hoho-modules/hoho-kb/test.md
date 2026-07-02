# hoho-kb 冒烟测试

这份文档用于验证 `hoho-kb` 第一版闭环：

```text
创建分类 -> 创建 FAQ -> 发布 FAQ 生成向量 -> 向量检索命中 FAQ
```

## 1. 前置条件

确保以下服务已经启动：

```text
Nacos
PostgreSQL + pgvector
Ollama
hoho-ai-proxy
hoho-kb
```

确认 `hoho-ai-proxy` 可用：

```bash
curl http://localhost:9205/ai/health
```

确认 embedding 可用：

```bash
curl -X POST http://localhost:9205/ai/embedding \
  -H "Content-Type: application/json" \
  -d '{"text":"电脑无法联网怎么办"}'
```

## 2. 初始化数据库

创建数据库，名称建议和 Nacos 配置保持一致：

```sql
CREATE DATABASE hoho_kb;
```

进入 `hoho_kb` 数据库后执行：

```sql
\i /Users/haohongguan/AGI/spring_ai/shsnc_ai/hoho/hoho-modules/hoho-kb/src/main/resources/sql/kb_schema.sql
```

如果不是用 `psql`，就直接复制 SQL 文件内容执行。

## 3. Nacos 配置

在 Nacos 新建配置：

```text
Data ID: hoho-kb-dev.yml
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
  typeAliasesPackage: com.hoho.kb.domain
  mapperLocations: classpath:mapper/**/*.xml

hoho:
  kb:
    ai-proxy:
      base-url: http://localhost:9205
    embedding:
      model: bge-m3
      dimension: 1024
    search:
      default-top-k: 5
```

## 4. 启动 hoho-kb

```bash
cd /Users/haohongguan/AGI/spring_ai/shsnc_ai/hoho
mvn -pl hoho-modules/hoho-kb spring-boot:run
```

健康检查：

```bash
curl http://localhost:9204/kb/health
```

期望返回：

```json
{
  "code": 200,
  "data": {
    "service": "hoho-kb",
    "status": "UP"
  }
}
```

## 5. 创建分类

```bash
curl -X POST http://localhost:9204/kb/category \
  -H "Content-Type: application/json" \
  -d '{
    "parentId": 0,
    "name": "网络问题",
    "sort": 1
  }'
```

记录返回的 `data`，下面假设分类 ID 是 `1`。

查看分类树：

```bash
curl http://localhost:9204/kb/category/tree
```

## 6. 创建 FAQ

```bash
curl -X POST http://localhost:9204/kb/qa \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": 1,
    "question": "电脑无法联网怎么办？",
    "answer": "请先检查网线或 Wi-Fi 是否连接，然后确认 IP 地址、DNS 配置是否正确。如果仍无法联网，可以重启路由器或联系 IT 运维。",
    "similarQuestions": "电脑上不了网|网络连接失败|无法访问互联网"
  }'
```

记录返回的 `data`，下面假设 FAQ ID 是 `1`。

查看 FAQ：

```bash
curl http://localhost:9204/kb/qa/1
```

## 7. 发布 FAQ 并生成向量

发布时会调用：

```text
hoho-ai-proxy /ai/embedding
```

然后写入 `kb_embedding`。

```bash
curl -X POST http://localhost:9204/kb/qa/1/publish
```

期望返回：

```json
{
  "code": 200,
  "data": true
}
```

确认 FAQ 状态：

```bash
curl http://localhost:9204/kb/qa/1
```

状态应为：

```text
published
```

## 8. 向量检索

使用相似问题检索：

```bash
curl -X POST http://localhost:9204/kb/search/vector \
  -H "Content-Type: application/json" \
  -d '{
    "query": "我的电脑突然不能上网了，怎么处理？",
    "topK": 5
  }'
```

期望返回结果中包含刚才发布的 FAQ：

```json
{
  "code": 200,
  "data": {
    "query": "我的电脑突然不能上网了，怎么处理？",
    "items": [
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

## 9. 关键词检索

使用关键词检索：

```bash
curl -X POST http://localhost:9204/kb/search/keyword \
  -H "Content-Type: application/json" \
  -d '{
    "query": "无法联网",
    "topK": 5
  }'
```

期望返回结果中包含刚才发布的 FAQ，`source` 为 `keyword`。

## 10. 混合检索

混合检索会同时使用关键词召回和向量召回，再用 RRF 融合排序：

```bash
curl -X POST http://localhost:9204/kb/search/hybrid \
  -H "Content-Type: application/json" \
  -d '{
    "query": "我的电脑突然不能上网了，怎么处理？",
    "topK": 5
  }'
```

期望返回结果中包含刚才发布的 FAQ，`source` 为 `hybrid`。

## 11. 常见问题

### DashScope 或 Ollama 调不通

先单独验证 `hoho-ai-proxy`：

```bash
curl http://localhost:9205/ai/health
curl -X POST http://localhost:9205/ai/embedding \
  -H "Content-Type: application/json" \
  -d '{"text":"测试向量"}'
```

### 提示 vector 类型不存在

说明 `pgvector` 扩展没有启用，进入 `hoho_kb` 数据库执行：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 提示向量维度不匹配

当前 schema 是：

```sql
embedding vector(1024)
```

这对应 Ollama `bge-m3`。如果换成其他 embedding 模型，需要同步修改：

```text
kb_schema.sql 里的 vector 维度
Nacos 里的 hoho.kb.embedding.dimension
已有历史向量数据
```

### 发布 FAQ 时报 index row size exceeds btree maximum

如果错误类似：

```text
ERROR: index row size 3088 exceeds btree version 4 maximum 2704 for index "idx_kb_embedding_vector_hnsw"
```

这是 `vector(1024)` 上的 HNSW 索引记录过大导致的。当前 MVP 已改为精确向量检索，不依赖 HNSW。进入 `hoho_kb` 数据库执行：

```sql
DROP INDEX IF EXISTS idx_kb_embedding_vector_hnsw;
```

或者重新执行：

```sql
\i /Users/haohongguan/AGI/spring_ai/shsnc_ai/hoho/hoho-modules/hoho-kb/src/main/resources/sql/kb_schema.sql
```

后续数据量变大时，再切换为 halfvec / IVFFlat / 降维索引方案。

### hoho-kb 无法调用 ai-proxy

确认 Nacos 配置：

```yaml
hoho:
  kb:
    ai-proxy:
      base-url: http://localhost:9205
```

如果 `hoho-kb` 在 Docker 容器里，而 `hoho-ai-proxy` 在宿主机，需要改成可从容器访问的地址。
