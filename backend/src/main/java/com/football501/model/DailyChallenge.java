package com.football501.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_challenges", indexes = {
    @Index(name = "idx_daily_challenges_date", columnList = "challenge_date DESC"),
    @Index(name = "idx_daily_challenges_category", columnList = "category_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "challenge_date", nullable = false)
    private LocalDate challengeDate;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "starting_score", nullable = false)
    private Integer startingScore;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
