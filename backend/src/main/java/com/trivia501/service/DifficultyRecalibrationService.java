package com.trivia501.service;

import com.trivia501.engine.DifficultyCalculator;
import com.trivia501.engine.DifficultyConstants;
import com.trivia501.model.Question;
import com.trivia501.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Bulk recalculates {@code difficulty_score} and re-evaluates viability for all
 * unlocked questions using their stored zone counts.
 *
 * <h3>When to use</h3>
 * <ul>
 *   <li>After adjusting any constant in {@link DifficultyConstants} (formula weights,
 *       saturation thresholds, or viability minimums).</li>
 *   <li>An admin manually requests a global re-score via
 *       {@code POST /api/admin/questions/recalculate-difficulty}.</li>
 * </ul>
 *
 * <h3>What it does NOT do</h3>
 * <p>This service reads only the stored zone counts ({@code high_value_count},
 * {@code mid_range_count}, {@code checkout_count}, {@code total_valid_count},
 * {@code total_score_pool}). It never touches the {@code answers} table.
 * Changing zone <em>boundaries</em> (e.g. extending checkout from 1–19 to 1–25)
 * requires re-materialisation or a SQL backfill before recalibration is meaningful.
 *
 * <h3>Locked questions</h3>
 * <p>Questions with {@code difficulty_locked = true} are skipped entirely. Their
 * {@code difficulty_score} is preserved as-is.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DifficultyRecalibrationService {

    private final QuestionRepository questionRepository;

    /**
     * Recalculates difficulty and viability for every unlocked question.
     *
     * @return a summary of what changed
     */
    @Transactional
    public RecalibrationResult recalculateAll() {
        List<Question> questions = questionRepository.findByDifficultyLockedFalse();
        int updated              = 0;
        int reExcluded           = 0;
        int dailyEligibilityChanged = 0;

        for (Question q : questions) {
            double newScore = DifficultyCalculator.calculate(
                q.getHighValueCount(),
                q.getMidRangeCount(),
                q.getCheckoutCount(),
                q.getTotalValidCount()
            );

            boolean nowViable = q.getTotalScorePool()  >= DifficultyConstants.MIN_SCORE_POOL
                             && q.getTotalValidCount() >= DifficultyConstants.MIN_ANSWER_COUNT;

            boolean nowSuitableForDaily = nowViable
                && newScore >= DifficultyConstants.DAILY_MIN_DIFFICULTY
                && newScore <= DifficultyConstants.DAILY_MAX_DIFFICULTY;

            boolean scoreChanged         = Math.abs(newScore - q.getDifficultyScore()) > 0.005;
            boolean viabilityChanged     = nowViable != q.isSingleQuestionViable();
            boolean dailyChanged         = nowSuitableForDaily != q.isSuitableForDaily();

            if (scoreChanged || viabilityChanged || dailyChanged) {
                q.setDifficultyScore(newScore);
                q.setSingleQuestionViable(nowViable);
                q.setSuitableForDaily(nowSuitableForDaily);

                if (!nowViable) {
                    String reason = buildViabilityReason(q.getTotalScorePool(), q.getTotalValidCount());
                    q.setViabilityExclusionReason(reason);
                    if (!Question.STATUS_EXCLUDED.equals(q.getStatus())) {
                        q.setStatus(Question.STATUS_EXCLUDED);
                        reExcluded++;
                        log.warn("Question {} newly excluded during recalibration: {}", q.getId(), reason);
                    }
                } else {
                    q.setViabilityExclusionReason(null);
                }

                if (dailyChanged) {
                    dailyEligibilityChanged++;
                    log.debug("Question {} suitable_for_daily → {} (score={})",
                        q.getId(), nowSuitableForDaily, newScore);
                }

                questionRepository.save(q);
                updated++;
            }
        }

        log.info("Recalibration complete — total={} updated={} reExcluded={} dailyEligibilityChanged={}",
            questions.size(), updated, reExcluded, dailyEligibilityChanged);
        return new RecalibrationResult(questions.size(), updated, reExcluded, dailyEligibilityChanged);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String buildViabilityReason(int scorePool, int validCount) {
        List<String> reasons = new ArrayList<>();
        if (scorePool < DifficultyConstants.MIN_SCORE_POOL) {
            reasons.add("insufficient_score_pool: " + scorePool
                      + " < " + DifficultyConstants.MIN_SCORE_POOL);
        }
        if (validCount < DifficultyConstants.MIN_ANSWER_COUNT) {
            reasons.add("insufficient_answer_count: " + validCount
                      + " < " + DifficultyConstants.MIN_ANSWER_COUNT);
        }
        return String.join("; ", reasons);
    }

    // ── Result record ─────────────────────────────────────────────────────────

    /**
     * Summary returned from {@link #recalculateAll()}.
     *
     * @param total                  total questions processed (unlocked)
     * @param updated                questions whose difficulty score, viability, or daily eligibility changed
     * @param reExcluded             questions newly set to {@code status = 'excluded'}
     * @param dailyEligibilityChanged questions whose {@code suitable_for_daily} flag changed
     */
    public record RecalibrationResult(int total, int updated, int reExcluded, int dailyEligibilityChanged) {}
}
