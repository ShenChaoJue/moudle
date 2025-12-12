# RAG 文件问答系统实现方案

## 📋 系统架构

```
离线处理阶段（文件上传时）
  文件上传 → 文档解析 → 文本切割 → 向量转换 → 向量库存储

在线交互阶段（用户提问时）
  用户Query → Query向量化 → 相似性检索 → Prompt构建 → LLM生成 → 返回答案
```

---

## 🔧 核心组件

### 1. 离线处理层

#### 1.1 文档解析服务 `DocumentParserService`
```java
// 支持格式：PDF, Word, TXT, Markdown
- parsePDF(File file) → String
- parseWord(File file) → String
- parseTxt(File file) → String
```

#### 1.2 文本切割服务 `TextChunkingService`
```java
// 切割策略
- 固定长度切割：每500字符一个片段，重叠50字符
- 语义切割：按段落、句子边界切割
- 递归切割：大段落递归拆分

chunkText(String text, int chunkSize, int overlap) → List<TextChunk>
```

#### 1.3 向量化服务 `EmbeddingService`（已有）
```java
// 使用 MiniMax Embedding API
embedText(String text) → List<Float>
embedTexts(List<String> texts) → List<List<Float>>
```

#### 1.4 向量存储服务 `MilvusService`（已有）
```java
// 存储片段向量
insertChunkVector(Long chunkId, List<Float> vector)
searchSimilarChunks(List<Float> queryVector, int topK) → List<Long>
```

---

### 2. 在线检索层

#### 2.1 查询处理服务 `QueryProcessingService`
```java
// Query 向量化
processQuery(String query) → List<Float>
```

#### 2.2 检索服务 `ChunkRetrievalService`
```java
// 相似性检索
retrieveSimilarChunks(String query, int topK) → List<FileChunk>

// 重排序（可选）
rerankChunks(List<FileChunk> chunks, String query) → List<FileChunk>
```

#### 2.3 Prompt 构建服务 `PromptBuilderService`
```java
// 构建 RAG Prompt
buildPrompt(String query, List<FileChunk> chunks) → String

// 模板示例：
// "根据以下文档片段回答问题：\n\n"
// "[片段1] ...\n[片段2] ...\n\n"
// "问题：{query}\n答案："
```

#### 2.4 答案生成服务 `AnswerGenerationService`
```java
// 调用 LLM 生成答案
generateAnswer(String prompt) → String

// 带来源标注
generateAnswerWithSource(String prompt, List<FileChunk> chunks) → AnswerWithSource
```

---

## 📊 数据库设计

### MySQL 表结构

```sql
-- 文件表（已有）
CREATE TABLE file_entity (
    id BIGINT PRIMARY KEY,
    original_name VARCHAR(255),
    file_path VARCHAR(500),
    content_type VARCHAR(100),
    file_size BIGINT,
    upload_time DATETIME
);

-- 文件片段表（新增）
CREATE TABLE file_chunk_entity (
    id BIGINT PRIMARY KEY,
    file_id BIGINT,              -- 所属文件
    chunk_index INT,             -- 片段索引
    chunk_text TEXT,             -- 片段文本
    start_pos INT,               -- 起始位置
    end_pos INT,                 -- 结束位置
    created_time DATETIME,
    INDEX idx_file_id (file_id)
);
```

### Milvus 集合设计

```python
# 片段向量集合
collection_name = "file_chunks"
schema = {
    "chunk_id": INT64,           # 片段ID
    "file_id": INT64,            # 文件ID
    "vector": FLOAT_VECTOR(1536) # 向量（维度根据模型）
}
index_type = "IVF_FLAT"         # 索引类型
metric_type = "COSINE"          # 相似度度量
```

---

## 🔄 完整流程

### 离线阶段：文件上传处理

```java
public void processUploadedFile(Long fileId) {
    // 1. 读取文件内容
    FileEntity file = fileMapper.selectById(fileId);
    String content = documentParser.parse(file);
    
    // 2. 文本切割
    List<TextChunk> chunks = textChunker.chunk(content, 500, 50);
    
    // 3. 保存片段到 MySQL
    for (int i = 0; i < chunks.size(); i++) {
        FileChunkEntity chunk = new FileChunkEntity();
        chunk.setFileId(fileId);
        chunk.setChunkIndex(i);
        chunk.setChunkText(chunks.get(i).getText());
        chunkMapper.insert(chunk);
        
        // 4. 向量化并存储到 Milvus
        List<Float> vector = embeddingService.embedText(chunk.getChunkText()).block();
        milvusService.insertChunkVector(chunk.getId(), vector);
    }
}
```

### 在线阶段：用户提问

```java
public String answerQuestion(String query) {
    // 1. Query 向量化
    List<Float> queryVector = embeddingService.embedText(query).block();
    
    // 2. 检索相似片段（Top 5）
    List<Long> chunkIds = milvusService.searchSimilarChunks(queryVector, 5);
    List<FileChunkEntity> chunks = chunkMapper.selectByIds(chunkIds);
    
    // 3. 构建 Prompt
    String prompt = promptBuilder.build(query, chunks);
    
    // 4. 调用 LLM 生成答案
    String answer = miniMaxService.simpleChat(prompt).block();
    
    return answer;
}
```

---

## 🎯 优化策略

### 1. 切割优化
- **固定长度 + 重叠**：避免语义断裂
- **语义边界**：按段落、句子切割
- **动态调整**：根据文档类型调整策略

### 2. 检索优化
- **混合检索**：向量检索 + 关键词检索
- **重排序**：使用 Cross-Encoder 重排
- **过滤**：根据文件类型、时间过滤

### 3. Prompt 优化
- **上下文窗口**：控制片段总长度
- **来源标注**：标记片段来源文件
- **指令优化**：明确回答要求

---

## 📦 实现清单

### 新增文件
- [ ] `DocumentParserService.java` - 文档解析
- [ ] `TextChunkingService.java` - 文本切割
- [ ] `ChunkRetrievalService.java` - 片段检索
- [ ] `PromptBuilderService.java` - Prompt 构建
- [ ] `FileChunkEntity.java` - 片段实体（已创建）
- [ ] `FileChunkMapper.java` - 片段 Mapper

### 修改文件
- [ ] `FileVectorizationService.java` - 改为片段级向量化
- [ ] `MilvusService.java` - 支持片段向量存储
- [ ] `FileBasedQAService.java` - 使用新的检索流程

### 数据库
- [ ] 创建 `file_chunk_entity` 表
- [ ] Milvus 创建 `file_chunks` 集合

---

## 🚀 实施步骤

1. **Phase 1: 基础设施**
   - 创建数据库表
   - 实现文档解析服务
   - 实现文本切割服务

2. **Phase 2: 离线处理**
   - 文件上传时自动切割
   - 片段向量化存储

3. **Phase 3: 在线检索**
   - 实现片段检索
   - 实现 Prompt 构建
   - 集成到问答流程

4. **Phase 4: 优化迭代**
   - 混合检索
   - 重排序
   - 性能优化

