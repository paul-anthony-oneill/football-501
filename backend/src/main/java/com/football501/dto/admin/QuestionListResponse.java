package com.football501.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class QuestionListResponse {
    private List<QuestionResponse> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
