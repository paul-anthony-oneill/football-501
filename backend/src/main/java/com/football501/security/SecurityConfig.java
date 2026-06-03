package com.football501.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

/**
 * Spring Security configuration for Trivia 501.
 *
 * <h3>Design intent</h3>
 * <p>{@link OptionalJwtFilter} runs on every request — no dev/prod split.
 * If a valid Supabase JWT is present the user is authenticated with their real
 * identity; otherwise an anonymous session UUID is assigned via cookie.
 * Gameplay works identically for both paths.
 *
 * <h3>Endpoint access policy</h3>
 * <pre>
 *   /api/admin/**           → ROLE_ADMIN   (also @PreAuthorize at method level)
 *   /api/solo/**            → ROLE_USER / ROLE_ADMIN
 *   GET /api/daily-challenge/** → permitAll (public browsing of daily challenges)
 *   POST /api/daily-challenge/** → authenticated (game start, submit, abandon)
 *   /api/entities/**        → permitAll    (public autocomplete)
 *   /api/categories/**      → permitAll    (public category listing)
 *   /actuator/health        → permitAll    (liveness probe)
 *   everything else         → authenticated
 * </pre>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final OptionalJwtFilter optionalJwtFilter;

    public SecurityConfig(JwtDecoder jwtDecoder,
                          JwtAuthenticationConverter jwtConverter,
                          @Value("${SUPABASE_JWT_SECRET:#{null}}") String jwtSecret) {
        boolean jwtConfigured = jwtSecret != null && !jwtSecret.isEmpty();
        this.optionalJwtFilter = new OptionalJwtFilter(jwtDecoder, jwtConverter, jwtConfigured);
        log.info("SecurityConfig: OptionalJwtFilter configured (JWT {}configured)",
                jwtConfigured ? "" : "NOT ");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())

            // CSRF disabled — this is a stateless REST API with Bearer-token auth.
            // However, OptionalJwtFilter sets a cookie (X-Anonymous-Id) for anonymous
            // sessions. If real auth tokens are ever stored in cookies instead of
            // Bearer headers, CSRF protection must be re-enabled before shipping.
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/entities/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/daily-challenge/**").permitAll()
                .requestMatchers("/api/daily-challenge/**").authenticated()
                .requestMatchers("/api/solo/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );

        // Hybrid auth: validates JWT if present, falls back to anonymous session if not.
        // Runs BEFORE AuthorizationFilter so all requests have an identity.
        http.addFilterBefore(optionalJwtFilter,
            org.springframework.security.web.access.intercept.AuthorizationFilter.class);

        // Rate limiting — after auth filters so it can read the X-Auth-Type attribute
        http.addFilterBefore(new RateLimitFilter(),
            org.springframework.security.web.access.intercept.AuthorizationFilter.class);

        return http.build();
    }

    /**
     * Maps Supabase JWTs to Spring Security authorities.
     * Supabase JWTs have {@code role: "authenticated"} — we grant ROLE_USER to all
     * authenticated tokens. Admin elevation is done via the {@code app_metadata.role}
     * custom claim if present.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");

        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new java.util.ArrayList<>(
                grantedAuthoritiesConverter.convert(jwt));
            // Grant ROLE_USER to all authenticated tokens
            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("authenticated"))) {
                authorities.clear();
                authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
                // Check for admin claim in app_metadata
                var appMetadata = jwt.getClaimAsMap("app_metadata");
                if (appMetadata != null && "admin".equals(appMetadata.get("role"))) {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
                }
            }
            return authorities;
        });
        return converter;
    }

    /**
     * JWT decoder for Supabase-issued tokens (HMAC-SHA256).
     * When {@code SUPABASE_JWT_SECRET} is not set returns a no-op decoder —
     * {@link OptionalJwtFilter} catches the exception and falls back to anonymous.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${SUPABASE_JWT_SECRET:#{null}}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            log.info("SUPABASE_JWT_SECRET not set — returning no-op JwtDecoder (dev mode)");
            // Return a decoder that never succeeds — harmless because on dev
            // we never hit the oauth2ResourceServer path.
            return token -> {
                throw new org.springframework.security.oauth2.jwt.JwtException(
                    "No SUPABASE_JWT_SECRET configured");
            };
        }
        log.info("JwtDecoder configured with SUPABASE_JWT_SECRET (HMAC-SHA256)");
        var key = new SecretKeySpec(
            jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }
}
