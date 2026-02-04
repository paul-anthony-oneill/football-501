package com.football501.controller;

import com.football501.dto.PlayerSearchResponse;
import com.football501.model.Player;
import com.football501.repository.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/players")
@Slf4j
public class PlayerController {

    private final PlayerRepository playerRepository;

    public PlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlayerSearchResponse>> searchPlayers(@RequestParam String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Player> players = playerRepository.searchByName(query.trim(), PageRequest.of(0, 10));

        List<PlayerSearchResponse> response = players.stream()
            .map(p -> PlayerSearchResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .nationality(p.getNationality())
                .build())
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
