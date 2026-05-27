package com.football501.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for Football 501.
 *
 * <h3>Design intent</h3>
 * <ul>
 *   <li>This config wires the plumbing without blocking local development.
 *       A {@link DevModeAuthFilter} (active on every profile except {@code prod})
 *       injects a fixed authenticated principal so all endpoints remain reachable
 *       without real OAuth tokens.</li>
 *   <li>{@code @EnableMethodSecurity} activates {@code @PreAuthorize} on admin
 *       controllers.  The annotations are already in place; this config makes
 *       them effective.</li>
 *   <li>CSRF is disabled — this is a stateless REST API consumed by a SPA.
 *       When JWT cookies are introduced, re-evaluate (SameSite=Strict cookies
 *       plus a double-submit token provide equivalent protection).</li>
 *   <li>Session policy is STATELESS — the server holds no HTTP session state.</li>
 * </ul>
 *
 * <h3>Endpoint access policy</h3>
 * <pre>
 *   /api/admin/**           → ROLE_ADMIN   (enforced by @PreAuthorize at method level)
 *   /api/solo/**        → ROLE_USER    (enforced at URL level here)
 *   /api/entities/search    → permitAll    (public autocomplete)
 *   /api/categories         → permitAll    (public category listing)
 *   /actuator/health        → permitAll    (liveness probe)
 *   everything else         → authenticated (safe default)
 * </pre>
 *
 * <h3>Production path</h3>
 * Add a JWT validation filter (e.g. {@code JwtAuthFilter}) before
 * {@link UsernamePasswordAuthenticationFilter} and activate it on the
 * {@code prod} profile.  Remove or disable {@link DevModeAuthFilter}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final DevModeAuthFilter devModeAuthFilter;

    /**
     * {@code devModeAuthFilter} is {@code null} on the {@code prod} profile
     * because the bean is annotated {@code @Profile("!prod")}.
     * Spring injects {@code null} for optional constructor args — the filter
     * is only added to the chain when it is non-null.
     */
    public SecurityConfig(
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            DevModeAuthFilter devModeAuthFilter) {
        this.devModeAuthFilter = devModeAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS — delegates to the CorsConfigurationSource bean in CorsConfig
            .cors(Customizer.withDefaults())

            // CSRF — disabled for stateless REST API
            .csrf(csrf -> csrf.disable())

            // No HTTP session — every request must carry its own credentials
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL-level access rules
            .authorizeHttpRequests(auth -> auth
                // Public — autocomplete and category listing need no auth
                .requestMatchers("/api/entities/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                // Health / liveness probes
                .requestMatchers("/actuator/health").permitAll()
                // Solo game — requires an authenticated user
                .requestMatchers("/api/solo/**").hasAnyRole("USER", "ADMIN")
                // Admin — also enforced at method level via @PreAuthorize
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Deny everything else by default (safe)
                .anyRequest().authenticated()
            );

        // Inject the dev-mode filter when not running in production
        if (devModeAuthFilter != null) {
            http.addFilterBefore(devModeAuthFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
