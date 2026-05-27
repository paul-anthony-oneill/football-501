package com.football501.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for starting a solo (single-player) game.
 *
 * <p>Player identity is never supplied by the client — it is derived from
 * the authenticated {@link java.security.Principal} in the controller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSoloGameRequest {

    private String categorySlug;

    private Integer difficulty;
}
