package com.trivia501.repository;

import com.trivia501.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByName(String name);

    Optional<Team> findByNormalizedName(String normalizedName);

    /** All club teams, ordered by popularity rank (ascending = most popular first). */
    List<Team> findByTeamTypeOrderByPopularityRankAsc(String teamType);
}
