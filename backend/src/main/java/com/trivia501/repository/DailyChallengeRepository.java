package com.trivia501.repository;

import com.trivia501.model.DailyChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, UUID> {

    Optional<DailyChallenge> findByChallengeDateAndCategoryId(LocalDate challengeDate, UUID categoryId);

    List<DailyChallenge> findByChallengeDate(LocalDate challengeDate);

    Optional<DailyChallenge> findTopByCategoryIdOrderByChallengeDateDesc(UUID categoryId);

    List<DailyChallenge> findByChallengeDateAndStatus(LocalDate challengeDate, String status);
}
