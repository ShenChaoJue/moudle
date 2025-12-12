package com.ziwen.moudle.service.file;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文本切割服务测试
 */
@SpringBootTest
public class TextChunkingServiceTest {

    private final TextChunkingService chunkingService = new TextChunkingService();

    @Test
    public void testNormalChunking() {
        // 测试正常文本切割
        String text = "a".repeat(6121); // 6121个字符
        var chunks = chunkingService.chunk(text, 500, 50);

        System.out.println("文本长度: " + text.length());
        System.out.println("片段数量: " + chunks.size());

        // 预期片段数量：(6121-50) / (500-50) ≈ 13.6，即14个片段
        assertTrue(chunks.size() > 0, "应该生成片段");
        assertTrue(chunks.size() < 20, "片段数量不应该超过20个");
        assertEquals(6121, chunks.stream()
            .mapToInt(chunk -> chunk.getText().length())
            .sum(), "所有片段长度之和应该等于原文长度");
    }

    @Test
    public void testEdgeCases() {
        // 测试边界情况
        assertEquals(0, chunkingService.chunk(null, 500, 50).size(), "空文本应该返回空列表");
        assertEquals(0, chunkingService.chunk("", 500, 50).size(), "空字符串应该返回空列表");

        // 测试小文本
        String smallText = "Hello World";
        var chunks = chunkingService.chunk(smallText, 500, 50);
        assertEquals(1, chunks.size(), "小文本应该只生成一个片段");
        assertEquals(smallText, chunks.get(0).getText(), "片段内容应该等于原文");
    }

    @Test
    public void testLargeOverlap() {
        // 测试大重叠情况
        String text = "a".repeat(1000);
        var chunks = chunkingService.chunk(text, 500, 400); // 80%重叠

        System.out.println("大重叠测试 - 文本长度: " + text.length());
        System.out.println("大重叠测试 - 片段数量: " + chunks.size());

        // 大重叠应该生成更多片段，但不应该无限循环
        assertTrue(chunks.size() > 0, "应该生成片段");
        assertTrue(chunks.size() < 50, "片段数量不应该超过50个");
    }

    @Test
    public void testZeroOverlap() {
        // 测试无重叠情况
        String text = "a".repeat(1000);
        var chunks = chunkingService.chunk(text, 500, 0);

        System.out.println("无重叠测试 - 文本长度: " + text.length());
        System.out.println("无重叠测试 - 片段数量: " + chunks.size());

        // 无重叠应该生成 1000/500 = 2个片段
        assertEquals(2, chunks.size(), "应该生成2个片段");
    }
}
