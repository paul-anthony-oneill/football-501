package com.football501.repository;

import com.football501.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for Player entities.
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {
}
