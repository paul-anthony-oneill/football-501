package com.football501.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PlayerSearchResponse {
    private UUID id;
    private String name;
    private String nationality;
}
