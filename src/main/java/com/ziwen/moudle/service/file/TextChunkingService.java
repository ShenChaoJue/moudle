package com.ziwen.moudle.service.file;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切割服务
 * 将长文本切割成适合向量化的片段
 *
 * @author : zixiwen
 * @version : 1.0
 * @date : 2025-12-07
 */
@Slf4j
@Service
public class TextChunkingService {

    /**
     * 文本片段
     */
    @Data
    public static class TextChunk {
        private String text;        // 片段文本
        private int startPos;       // 起始位置
        private int endPos;         // 结束位置
        private int index;          // 片段索引
    }

    /**
     * 切割文本
     *
     * @param text 原始文本
     * @param chunkSize 片段大小（字符数）
     * @param overlap 重叠字符数
     * @return 文本片段列表
     */
    public List<TextChunk> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            log.warn("文本为空，跳过切割");
            return new ArrayList<>();
        }

        // 防止文本过大导致内存问题，现在支持最大50MB文本
        if (text.length() > 50 * 1024 * 1024) { // 50MB字符
            log.warn("文本过长（{} 字符），可能影响性能，建议分批处理", text.length());
        }

        log.info("开始切割文本，总长度: {}, 片段大小: {}, 重叠: {}", text.length(), chunkSize, overlap);

        // 更合理的预估容量计算，避免过度分配内存
        int estimatedChunks = Math.max(1, (int) Math.ceil((double) text.length() / (chunkSize - overlap)));
        ArrayList<TextChunk> chunks = new ArrayList<>(Math.min(estimatedChunks, 1000));

        int start = 0;
        int index = 0;

        try {
            while (start < text.length()) {
                // 计算结束位置
                int end = Math.min(start + chunkSize, text.length());

                // 如果不是最后一个片段，尝试在句子边界切割
                if (end < text.length()) {
                    end = findSentenceBoundary(text, end);
                }

                // 创建片段
                TextChunk chunk = new TextChunk();
                chunk.setText(text.substring(start, end));
                chunk.setStartPos(start);
                chunk.setEndPos(end);
                chunk.setIndex(index++);
                chunks.add(chunk);

                // 添加内存保护机制
                if (chunks.size() >= 50000) { // 降低阈值到5万个片段
                    log.error("生成的文本片段过多({}个)，可能存在逻辑错误或文本异常", chunks.size());
                    throw new RuntimeException("文本片段数量超出限制，请检查输入文本或调整参数");
                }

                // 移动到下一个起始位置（考虑重叠）
                // 关键修复：先检查是否还能继续，避免无限循环
                int nextStart = end - overlap;
                if (nextStart >= text.length() || nextStart <= start) {
                    // 如果下一个起始位置超出文本范围或倒退，结束循环
                    break;
                }
                start = nextStart;
            }
        } catch (OutOfMemoryError e) {
            log.error("内存不足！文本长度: {}, 片段大小: {}, 已生成片段: {}", text.length(), chunkSize, chunks.size(), e);
            throw new RuntimeException("文本切割时发生内存溢出，请检查文本大小或调整JVM内存配置", e);
        }

        log.info("切割完成，共 {} 个片段", chunks.size());
        return chunks;
    }

    /**
     * 查找句子边界（优化切割点）
     */
    private int findSentenceBoundary(String text, int position) {
        // 在position前后50个字符范围内查找句子结束符
        int searchStart = Math.max(0, position - 50);
        int searchEnd = Math.min(text.length(), position + 50);

        // 查找句子结束符：。！？\n
        int lastBoundary = -1;
        for (int i = searchStart; i < searchEnd; i++) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '\n' || c == '.' || c == '!' || c == '?') {
                if (i <= position) {
                    lastBoundary = i + 1;
                } else if (lastBoundary == -1) {
                    return i + 1;
                }
            }
        }

        // 如果找到了边界，使用它
        if (lastBoundary != -1) {
            return lastBoundary;
        }

        // 否则返回原位置
        return position;
    }

    /**
     * 智能切割（根据文本类型自动选择策略）
     */
    public List<TextChunk> smartChunk(String text) {
        // 优化策略：
        // 1. 增大片段大小到 800 字符（更完整的语义）
        // 2. 增加重叠到 100 字符（保留更多上下文）
        // 3. 适配 text-embedding-v4 的 8192 Token 上限
        return chunk(text, 800, 100);
    }
}

