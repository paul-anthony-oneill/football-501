package com.football501.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Development-mode authentication filter.
 *
 * <p>Active on every Spring profile <em>except</em> {@code prod}.
 * Injects a fixed, fully-authenticated principal into the {@link
 * org.springframework.security.core.context.SecurityContext} before
 * every request so that all endpoints — including admin-only ones —
 * are reachable without real OAuth tokens during local development
 * and automated tests.
 *
 * <h3>What it does</h3>
 * <ul>
 *   <li>Principal name: {@link #DEV_PLAYER_ID} — a fixed UUID string that
 *       acts as the logged-in player's identity.  Controllers read player
 *       identity via {@code principal.getName()} and parse it as a UUID.</li>
 *   <li>Authorities: {@code ROLE_USER} + {@code ROLE_ADMIN} — grants full
 *       access so both game endpoints and admin debug endpoints work out of
 *       the box during development.</li>
 *   <li>Idempotent: if a request already carries authentication (e.g. from
 *       a test that explicitly sets one), this filter is a no-op.</li>
 * </ul>
 *
 * <h3>Production</h3>
 * This bean is not created on the {@code prod} profile.  A JWT validation
 * filter provides real authentication in production.
 *
 * <h3>Tests</h3>
 * {@code @WebMvcTest} and {@code @SpringBootTest} tests running under the
 * {@code test} profile automatically get this filter, so no {@code @WithMockUser}
 * is required.  Tests that need to simulate a specific player ID should set the
 * player UUID to {@link #DEV_PLAYER_ID}.
 */
@Component
@Profile("!prod")
@Slf4j
public class DevModeAuthFilter extends OncePerRequestFilter {

    /**
     * Fixed player UUID injected by this filter.
     * Use this constant in tests that assert on player-scoped behaviour
     * (e.g. game ownership checks) so the value stays in sync with what
     * the filter actually injects.
     */
    public static final String DEV_PLAYER_ID = "00000000-0000-0000-0000-000000000001";

    private static final List<SimpleGrantedAuthority> DEV_AUTHORITIES = List.of(
        new SimpleGrantedAuthority("ROLE_USER"),
        new SimpleGrantedAuthority("ROLE_ADMIN")
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Don't override authentication already set by another filter or test harness
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(DEV_PLAYER_ID, null, DEV_AUTHORITIES);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.trace("DevModeAuthFilter: injected dev principal for {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
