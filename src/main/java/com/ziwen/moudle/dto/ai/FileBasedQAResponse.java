package com.ziwen.moudle.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FileBasedQAResponse {
    private String question;
    private String answer;
    private List<ReferencedFile> referencedFiles;
    private Integer filesCount;
    private String model;
    private Object usage;
    private LocalDateTime timestamp;
    private String error;

    @Data
    @Builder
    public static class ReferencedFile {
        private Long fileId;
        private String fileName;
        private String preview;
    }
}

