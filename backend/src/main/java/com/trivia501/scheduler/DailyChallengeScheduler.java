package com.trivia501.scheduler;

import com.trivia501.engine.DifficultyConstants;
import com.trivia501.model.Category;
import com.trivia501.model.DailyChallenge;
import com.trivia501.model.Question;
import com.trivia501.repository.AnswerRepository;
import com.trivia501.repository.CategoryRepository;
import com.trivia501.repository.DailyChallengeRepository;
import com.trivia501.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pre-selects daily challenges at midnight UTC so questions are ready when the
 * first player arrives.  Also serves as an early-warning system: if no question
 * is viable for a category, the error is logged well before a player hits it.
 */
@Component
@Slf4j
public class DailyChallengeScheduler {

    private final DailyChallengeRepository challengeRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final AnswerRepository answerRepository;

    public DailyChallengeScheduler(
            DailyChallengeRepository challengeRepository,
            QuestionRepository questionRepository,
            CategoryRepository categoryRepository,
            AnswerRepository answerRepository
    ) {
        this.challengeRepository = challengeRepository;
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.answerRepository = answerRepository;
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
                // The test category exists for development only — never produce a daily challenge.
                if ("test".equals(category.getSlug())) {
                    skipped++;
                    continue;
                }

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

                // Avoid repeating yesterday's starting score in the same category
                int yesterdayScore = challengeRepository
                    .findLatestStartingScoreBefore(category.getId(), today)
                    .orElse(-1);

                var result = findViableQuestionAndScore(category.getId(), yesterdayScore);
                if (result == null) {
                    log.warn("No viable daily question for category '{}' at any starting score", category.getSlug());
                    failed++;
                    continue;
                }

                DailyChallenge challenge = DailyChallenge.builder()
                        .challengeDate(today)
                        .categoryId(category.getId())
                        .questionId(result.question().getId())
                        .startingScore(result.score())
                        .status("active")
                        .build();

                challengeRepository.save(challenge);
                created++;
                log.info("Daily challenge created for '{}': questionId={}, startingScore={}",
                        category.getSlug(), result.question().getId(), result.score());

            } catch (Exception e) {
                log.error("Failed to create daily challenge for category '{}'", category.getSlug(), e);
                failed++;
            }
        }

        log.info("Daily challenge selection complete: created={}, skipped={}, failed={}", created, skipped, failed);
    }

    /**
     * Finds a (question, score) pair where the question's answer pool supports
     * the score. Tries candidate scores in random order, falling back through
     * the full pool if the first choice fails.
     *
     * @param yesterdayScore the score used yesterday for this category, or -1
     * @return a viable pair, or null if exhausted
     */
    public static QuestionScorePair findViableQuestionAndScore(
            QuestionRepository questionRepo, AnswerRepository answerRepo,
            UUID categoryId, int yesterdayScore
    ) {
        int score = DifficultyConstants.pickDailyStartingScore(yesterdayScore);

        Optional<Question> questionOpt = findViableQuestion(questionRepo, answerRepo, categoryId, score);
        if (questionOpt.isPresent()) {
            return new QuestionScorePair(questionOpt.get(), score);
        }

        // Fallback: try every score in the pool (the first pick might have been a
        // bad match for first-move viability)
        for (int fallback : DifficultyConstants.DAILY_STARTING_SCORES) {
            if (fallback == score) continue; // already tried
            questionOpt = findViableQuestion(questionRepo, answerRepo, categoryId, fallback);
            if (questionOpt.isPresent()) {
                return new QuestionScorePair(questionOpt.get(), fallback);
            }
        }

        return null;
    }

    private QuestionScorePair findViableQuestionAndScore(UUID categoryId, int yesterdayScore) {
        return findViableQuestionAndScore(questionRepository, answerRepository, categoryId, yesterdayScore);
    }

    /**
     * Finds a random daily-suitable question and verifies it has at least one
     * answer the player can play on the first turn (score ≤ startingScore +
     * FIRST_MOVE_MARGIN, to allow checkout-window moves).
     */
    private static Optional<Question> findViableQuestion(
            QuestionRepository questionRepo, AnswerRepository answerRepo,
            UUID categoryId, int startingScore
    ) {
        Optional<Question> questionOpt = questionRepo.findRandomDailyQuestion(
                categoryId, startingScore,
                DifficultyConstants.DAILY_MIN_DIFFICULTY,
                DifficultyConstants.DAILY_MAX_DIFFICULTY);

        if (questionOpt.isEmpty()) return Optional.empty();

        Question q = questionOpt.get();
        int firstMoveWindow = startingScore + DifficultyConstants.FIRST_MOVE_MARGIN;

        if (!answerRepo.hasViableFirstMove(q.getId(), firstMoveWindow)) {
            log.debug("Question {} (pool={}) has no first-move answers ≤ {} (startingScore={} + margin={})",
                    q.getId(), q.getTotalScorePool(), firstMoveWindow,
                    startingScore, DifficultyConstants.FIRST_MOVE_MARGIN);
            return Optional.empty();
        }

        return questionOpt;
    }

    /** Pair that bundles a question with its assigned starting score. */
    public record QuestionScorePair(Question question, int score) {}
}
