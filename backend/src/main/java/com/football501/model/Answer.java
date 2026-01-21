package com.football501.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
    name = "answers",
    uniqueConstraints = {
        @UniqueConstraint(name = "idx_answers_question_key", columnNames = {"question_id", "answer_key"})
    },
    indexes = {
        @Index(name = "idx_answers_question_score", columnList = "question_id, score DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "answer_key", nullable = false)
    private String answerKey;

    @Column(name = "display_text", nullable = false)
    private String displayText;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "is_valid_darts", nullable = false)
    private Boolean isValidDarts;

    @Column(name = "is_bust", nullable = false)
    private Boolean isBust;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
