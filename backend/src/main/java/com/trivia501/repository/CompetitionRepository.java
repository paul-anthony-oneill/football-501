package com.trivia501.repository;

import com.trivia501.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, UUID> {

    Optional<Competition> findByName(String name);

    Optional<Competition> findByNormalizedName(String normalizedName);

    /** All competitions of the given type, e.g. {@code "domestic_league"}. */
    List<Competition> findByCompetitionType(String competitionType);

    /**
     * Top-flight domestic leagues only (tier = 1).
     * Used by the template generator when enumerating eligible competitions.
     */
    List<Competition> findByTier(Short tier);
}
