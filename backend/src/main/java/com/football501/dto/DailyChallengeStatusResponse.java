package com.football501.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyChallengeStatusResponse {

    private LocalDate date;
    private List<CategoryChallenge> challenges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryChallenge {
        private String categorySlug;
        private String categoryName;
        private int startingScore;
        private String questionText;
        private boolean hasChallenge; // false if no challenge exists for this category today
    }
}
