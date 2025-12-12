# 多模态搜索系统使用指南

本系统基于Milvus向量数据库和通义千问API实现了完整的多模态搜索功能，支持以文搜图、以文搜文、以图搜图、以图搜文四种搜索方式。

## 架构概述

### 核心技术栈
- **Milvus**: 高效的向量数据库，用于存储和检索向量
- **通义千问VL**: 提取图片描述和关键词
- **DashScope Embedding API**: 多模态向量化服务，将图片和文本转换为向量

### 支持的功能
1. **以文搜图**: 输入文本查询，搜索最相似的图片
2. **以文搜文**: 输入文本查询，搜索最相似的图片描述
3. **以图搜图**: 输入图片查询，搜索最相似的图片
4. **以图搜文**: 输入图片查询，搜索最相似的图片描述

## 配置说明

### 1. application.yml配置

```yaml
# Milvus 向量数据库配置
milvus:
  host: localhost
  port: 19530
  collection-name: multimodal_search
  vector-dim: 1024
  index-type: IVF_FLAT
  nlist: 1024
  m: 64
  ef-construction: 100

# 通义千问 DashScope API 配置
dashscope:
  api-key: YOUR_DASHSCOPE_API_KEY
  base-url: https://dashscope.aliyuncs.com/api/v1
  qwen-vl-model: qwen-vl-plus
  embedding-model: multimodal-embedding-v1
```

### 2. 环境准备

确保以下服务已启动：
- Milvus服务 (默认端口: 19530)
- 应用服务 (默认端口: 8080)

## API接口说明

### 1. 初始化集合

**接口**: `POST /api/multimodal/init`

**功能**: 初始化Milvus集合，仅需调用一次

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "集合初始化成功"
}
```

### 2. 上传并索引图片

**接口**: `POST /api/multimodal/index-image`

**请求体**:
```json
{
  "origin": "/path/to/image.jpg",
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQ..."
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "图片索引成功"
}
```

### 3. 以文搜图

**接口**: `POST /api/multimodal/search/text-to-image`

**请求体**:
```json
{
  "query": "棕色的狗",
  "limit": 5
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "distance": 0.123,
      "origin": "/path/to/image1.jpg",
      "imageDescription": "一只棕色的狗在草地上奔跑"
    },
    ...
  ]
}
```

### 4. 以文搜文

**接口**: `POST /api/multimodal/search/text-to-text`

**请求体**:
```json
{
  "query": "棕色的狗",
  "limit": 5
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "distance": 0.098,
      "origin": "/path/to/image1.jpg",
      "imageDescription": "一只棕色的狗在草地上奔跑"
    },
    ...
  ]
}
```

### 5. 以图搜图

**接口**: `POST /api/multimodal/search/image-to-image`

**请求体**:
```json
{
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQ...",
  "limit": 5
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "distance": 0.045,
      "origin": "/path/to/similar_image.jpg",
      "imageDescription": "一只相似的棕色狗"
    },
    ...
  ]
}
```

### 6. 以图搜文

**接口**: `POST /api/multimodal/search/image-to-text`

**请求体**:
```json
{
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQ...",
  "limit": 5
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "distance": 0.067,
      "origin": "/path/to/image1.jpg",
      "imageDescription": "一只棕色的狗在草地上奔跑"
    },
    ...
  ]
}
```

## 使用流程

### 1. 初始化系统
```bash
curl -X POST http://localhost:8080/api/multimodal/init
```

### 2. 索引图片
```bash
# 准备base64编码的图片数据
curl -X POST http://localhost:8080/api/multimodal/index-image \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "/path/to/your/image.jpg",
    "imageBase64": "data:image/jpeg;base64,..."
  }'
```

### 3. 执行搜索

**以文搜图**:
```bash
curl -X POST http://localhost:8080/api/multimodal/search/text-to-image \
  -H "Content-Type: application/json" \
  -d '{
    "query": "棕色的狗",
    "limit": 5
  }'
```

**以图搜图**:
```bash
curl -X POST http://localhost:8080/api/multimodal/search/image-to-image \
  -H "Content-Type: application/json" \
  -d '{
    "imageBase64": "data:image/jpeg;base64,...",
    "limit": 5
  }'
```

## 核心组件说明

### 1. EmbeddingService
负责文本和图片的向量化：
- `embedText(String text)`: 文本向量化
- `embedImage(String imageBase64)`: 图片向量化
- `embedTexts(List<String> texts)`: 批量文本向量化
- `embedImages(List<String> imageBase64List)`: 批量图片向量化

### 2. VisionLanguageService
负责图片内容理解：
- `describeImage(String imageBase64)`: 提取图片描述
- `extractKeywords(String imageBase64)`: 提取图片关键词

### 3. MultiModalSearchService
负责多模态搜索的核心逻辑：
- `insertMultimodalData()`: 插入多模态数据
- `searchByText()`: 以文搜图
- `searchTextByText()`: 以文搜文
- `searchByImage()`: 以图搜图
- `searchTextByImage()`: 以图搜文

### 4. QwenService
负责与通义千问API交互：
- `chat(ChatRequest request)`: 文本对话生成

## 技术细节

### 向量维度
- 通义千问多模态Embedding模型输出的向量维度为1024

### 索引类型
- **IVF_FLAT**: 适用于中等规模数据，查询速度较快
- **HNSW**: 适用于大规模数据，查询速度更快

### 相似度计算
- 使用L2距离计算向量相似度
- 距离越小，表示相似度越高

## 注意事项

1. **API Key**: 请确保正确配置通义千问的API Key
2. **Milvus连接**: 确保Milvus服务正常运行
3. **图片格式**: 支持常见的图片格式（JPEG、PNG等）
4. **Base64编码**: 图片需要以base64格式传输，格式为 `data:image/{format};base64,{data}`
5. **性能考虑**: 批量索引时可考虑增加API调用间隔，避免超出频率限制

## 错误处理

系统会返回标准的AjaxResult格式：
- `code`: 状态码（200表示成功）
- `message`: 响应消息
- `data`: 响应数据

当出现错误时，请检查：
1. API Key是否有效
2. Milvus服务是否正常
3. 请求参数格式是否正确
4. 网络连接是否正常

## 扩展说明

系统采用模块化设计，可以轻松扩展：
- 支持更多向量数据库（如FAISS、Weaviate等）
- 支持更多向量化模型
- 支持更多图片格式
- 支持更复杂的搜索策略