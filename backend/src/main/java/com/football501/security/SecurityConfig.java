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
 * Spring Security configuration for Football 501.
 *
 * <h3>Design intent</h3>
 * <ul>
 *   <li>On dev/test profiles: {@link DevModeAuthFilter} injects a fixed authenticated
 *       principal so all endpoints work without real OAuth tokens.</li>
 *   <li>On prod profile: OAuth2 Resource Server validates Supabase-issued JWTs.
 *       Configure via {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}
 *       and {@code SUPABASE_JWT_SECRET} (for HMAC-SHA256 verification).</li>
 * </ul>
 *
 * <h3>Endpoint access policy</h3>
 * <pre>
 *   /api/admin/**           → ROLE_ADMIN   (also @PreAuthorize at method level)
 *   /api/practice/**        → ROLE_USER / ROLE_ADMIN
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

    private final DevModeAuthFilter devModeAuthFilter;
    private final String jwtIssuerUri;

    public SecurityConfig(
            org.springframework.beans.factory.ObjectProvider<DevModeAuthFilter> provider,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String jwtIssuerUri) {
        this.devModeAuthFilter = provider.getIfAvailable();
        this.jwtIssuerUri = jwtIssuerUri;
        log.info("SecurityConfig: devModeAuthFilter = {}, jwtIssuerUri = {}",
                devModeAuthFilter != null ? "PRESENT" : "NULL",
                jwtIssuerUri.isEmpty() ? "NOT SET" : jwtIssuerUri);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())

            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/entities/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/daily-challenge/**").permitAll()
                .requestMatchers("/api/daily-challenge/**").authenticated()
                .requestMatchers("/api/practice/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );

        // Prod: validate Supabase JWTs via OAuth2 Resource Server
        if (!jwtIssuerUri.isEmpty()) {
            log.info("Configuring OAuth2 Resource Server for JWT validation");
            http.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        }

        // Dev/test: inject a fixed authenticated principal BEFORE rate limiting
        // so the rate limiter sees the authenticated user and applies the
        // higher limit (100 req/min vs 10 req/min for unauthenticated).
        if (devModeAuthFilter != null) {
            log.info("Adding DevModeAuthFilter");
            http.addFilterBefore(devModeAuthFilter,
                org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        }

        // Rate limiting — after auth filters so request.getRemoteUser() works
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
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
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
     * Only effective when SUPABASE_JWT_SECRET is set (required on prod).
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
