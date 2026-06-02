package com.football501.scheduler;

import com.football501.model.Category;
import com.football501.model.DailyChallenge;
import com.football501.model.Question;
import com.football501.repository.CategoryRepository;
import com.football501.repository.DailyChallengeRepository;
import com.football501.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pre-selects daily challenges at midnight UTC so questions are ready when the
 * first player arrives.  Also serves as an early-warning system: if no question
 * is viable for a category, the error is logged well before a player hits it.
 */
@Component
@Slf4j
public class DailyChallengeScheduler {

    private static final int[] STARTING_SCORES = {501, 401, 351, 301, 251, 201, 167, 125, 101};
    private static final double DAILY_MIN_DIFFICULTY = 0.0;
    private static final double DAILY_MAX_DIFFICULTY = 5.5;

    private final DailyChallengeRepository challengeRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;

    public DailyChallengeScheduler(
            DailyChallengeRepository challengeRepository,
            QuestionRepository questionRepository,
            CategoryRepository categoryRepository
    ) {
        this.challengeRepository = challengeRepository;
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Runs at midnight UTC every day.  For each category that has at least one
     * {@code suitable_for_daily} question, pre-selects the day's question and
     * starting score.
     *
     * <p>Idempotent — if today's challenge already exists for a category, it is
     * skipped.  This is safe to run multiple times (e.g. on app restart).
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void selectDailyChallenges() {
        LocalDate today = LocalDate.now();
        log.info("Daily challenge selection starting for {}", today);

        List<Category> categories = categoryRepository.findAll();
        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (Category category : categories) {
            try {
                // Skip if today's challenge already exists
                if (challengeRepository.findByChallengeDateAndCategoryId(today, category.getId()).isPresent()) {
                    skipped++;
                    continue;
                }

                // Check if this category has any suitable_for_daily questions
                if (!questionRepository.existsByCategoryIdAndSuitableForDailyTrueAndStatus(
                        category.getId(), Question.STATUS_ACTIVE)) {
                    log.debug("No suitable_for_daily questions for category '{}', skipping", category.getSlug());
                    skipped++;
                    continue;
                }

                // Pick a starting score and find a viable question
                int score = pickStartingScore();
                Optional<Question> questionOpt = findViableQuestion(category.getId(), score);

                if (questionOpt.isEmpty()) {
                    log.warn("No viable daily question for category '{}' at any starting score", category.getSlug());
                    failed++;
                    continue;
                }

                DailyChallenge challenge = DailyChallenge.builder()
                        .challengeDate(today)
                        .categoryId(category.getId())
                        .questionId(questionOpt.get().getId())
                        .startingScore(score)
                        .status("active")
                        .build();

                challengeRepository.save(challenge);
                created++;
                log.info("Daily challenge created for '{}': questionId={}, startingScore={}",
                        category.getSlug(), questionOpt.get().getId(), score);

            } catch (Exception e) {
                log.error("Failed to create daily challenge for category '{}'", category.getSlug(), e);
                failed++;
            }
        }

        log.info("Daily challenge selection complete: created={}, skipped={}, failed={}", created, skipped, failed);
    }

    private int pickStartingScore() {
        return STARTING_SCORES[ThreadLocalRandom.current().nextInt(STARTING_SCORES.length)];
    }

    private Optional<Question> findViableQuestion(UUID categoryId, int score) {
        Optional<Question> questionOpt = questionRepository.findRandomDailyQuestion(
                categoryId, score, DAILY_MIN_DIFFICULTY, DAILY_MAX_DIFFICULTY);

        if (questionOpt.isEmpty()) {
            for (int fallbackScore : STARTING_SCORES) {
                questionOpt = questionRepository.findRandomDailyQuestion(
                        categoryId, fallbackScore, DAILY_MIN_DIFFICULTY, DAILY_MAX_DIFFICULTY);
                if (questionOpt.isPresent()) {
                    return questionOpt;
                }
            }
        }
        return questionOpt;
    }
}
