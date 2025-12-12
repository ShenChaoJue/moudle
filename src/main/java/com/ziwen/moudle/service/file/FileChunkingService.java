package com.ziwen.moudle.service.file;

import com.ziwen.moudle.entity.file.FileChunkEntity;
import com.ziwen.moudle.entity.file.FileEntity;
import com.ziwen.moudle.mapper.file.FileChunkMapper;
import com.ziwen.moudle.service.embedding.EmbeddingService;
import com.ziwen.moudle.service.vector.MilvusService;
import com.ziwen.moudle.utils.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文件切片编排服务
 * 负责：文档解析 → 文本切割 → 向量化 → 存储
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-07
 */
@Slf4j
@Service
public class FileChunkingService {

    private final DocumentParserService documentParser;
    private final TextChunkingService textChunker;
    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;
    private final FileChunkMapper chunkMapper;
    private final SnowflakeIdGenerator idGenerator;

    public FileChunkingService(DocumentParserService documentParser,
                              TextChunkingService textChunker,
                              EmbeddingService embeddingService,
                              MilvusService milvusService,
                              FileChunkMapper chunkMapper,
                              SnowflakeIdGenerator idGenerator) {
        this.documentParser = documentParser;
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.milvusService = milvusService;
        this.chunkMapper = chunkMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 处理文件：解析 → 切割 → 向量化 → 存储
     */
    public void processFile(FileEntity file) {
        log.info("开始处理文件: {}", file.getOriginalName());

        try {
            // 1. 解析文档
            DocumentParserService.ParseResult parseResult = documentParser.parse(file);

            if (!parseResult.isCanChunk()) {
                log.warn("文件不支持切片: {}", parseResult.getWarning());
                return;
            }

            // 2. 使用流式方式处理文本，避免一次性加载整个文件到内存
            String text = parseResult.getText();
            
            // 如果文本过大，直接抛出异常，避免后续处理导致内存溢出，现在支持最大50MB文本
            if (text.length() > 50 * 1024 * 1024) { // 50MB字符
                log.error("文件过大，无法处理: {} 字符", text.length());
                throw new RuntimeException("文件过大，无法处理，请使用较小的文件");
            }
            
            List<TextChunkingService.TextChunk> chunks = textChunker.smartChunk(text);

            log.info("开始处理 {} 个文本片段", chunks.size());

            // 3. 处理每个片段
            for (TextChunkingService.TextChunk chunk : chunks) {
                processChunk(file, chunk.getText(), chunk.getIndex(), chunk.getStartPos(), chunk.getEndPos());
                
                // 每处理50个片段输出一次进度日志
                if (chunk.getIndex() % 50 == 0) {
                    log.info("已处理 {} 个片段，共 {} 个", chunk.getIndex(), chunks.size());
                }
            }

            log.info("文件处理完成: {}, 共处理 {} 个片段", file.getOriginalName(), chunks.size());

        } catch (Exception e) {
            log.error("文件处理失败: {}", file.getOriginalName(), e);
            throw new RuntimeException("文件处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理单个文本片段
     */
    private void processChunk(FileEntity file, String chunkText, int index, int startPos, int endPos) {
        try {
            // 保存到MySQL
            FileChunkEntity chunkEntity = new FileChunkEntity();
            chunkEntity.setId(idGenerator.nextId());
            chunkEntity.setFileId(file.getId());
            chunkEntity.setChunkIndex(index);
            chunkEntity.setChunkText(chunkText);
            chunkEntity.setStartPos(startPos);
            chunkEntity.setEndPos(endPos);
            chunkMapper.insert(chunkEntity);

            // 向量化并存储到Milvus（使用 document 类型）
            List<Float> vector = embeddingService.embedText(chunkText, "document").block();
            milvusService.insertVector(chunkEntity.getId(), vector);

            log.debug("片段 {} 处理完成，长度: {}", index, chunkText.length());
        } catch (Exception e) {
            log.error("片段 {} 处理失败", index, e);
            throw new RuntimeException("片段处理失败: " + e.getMessage(), e);
        }
    }


    /**
     * 删除文件的所有片段
     */
    public void deleteFileChunks(Long fileId) {
        log.info("删除文件片段: {}", fileId);
        
        // 1. 查询所有片段ID
        List<FileChunkEntity> chunks = chunkMapper.selectByFileId(fileId);
        
        // 2. 从Milvus删除向量
        for (FileChunkEntity chunk : chunks) {
            milvusService.deleteVector(chunk.getId());
        }
        
        // 3. 从MySQL删除片段
        chunkMapper.deleteByFileId(fileId);
        
        log.info("文件片段删除完成，共 {} 个", chunks.size());
    }
}

