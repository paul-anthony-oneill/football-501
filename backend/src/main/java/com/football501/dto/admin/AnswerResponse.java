package com.football501.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class AnswerResponse {
    private UUID id;
    private UUID questionId;
    private String answerKey;
    private String displayText;
    private Integer score;
    private Boolean isValidDarts;
    private Boolean isBust;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
