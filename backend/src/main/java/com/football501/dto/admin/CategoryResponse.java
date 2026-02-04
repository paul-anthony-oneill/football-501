package com.football501.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CategoryResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private long questionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
