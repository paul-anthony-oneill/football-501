package com.football501.repository;

import com.football501.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeasonRepository extends JpaRepository<Season, UUID> {

    Optional<Season> findByLabel(String label);

    Optional<Season> findByIsCurrentTrue();
}
