package com.trivia501.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Produces the {@link JwtDecoder} and {@link JwtAuthenticationConverter} beans used by
 * {@link com.trivia501.security.SecurityConfig}. Kept separate to avoid a circular
 * dependency: SecurityConfig consumes these beans, so they cannot be defined inside it.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    /**
     * JWT decoder for Supabase-issued tokens (HMAC-SHA256).
     * When {@code SUPABASE_JWT_SECRET} is not set returns a no-op decoder —
     * {@link com.trivia501.security.OptionalJwtFilter} catches the exception and falls
     * back to anonymous.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${SUPABASE_JWT_SECRET:#{null}}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            log.info("SUPABASE_JWT_SECRET not set — returning no-op JwtDecoder (dev mode)");
            return token -> {
                throw new org.springframework.security.oauth2.jwt.JwtException(
                    "No SUPABASE_JWT_SECRET configured");
            };
        }
        log.info("JwtDecoder configured with SUPABASE_JWT_SECRET (HMAC-SHA256)");
        var key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    /**
     * Maps Supabase JWTs to Spring Security authorities.
     * Supabase JWTs have {@code role: "authenticated"} — we grant ROLE_USER to all
     * authenticated tokens.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");

        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new ArrayList<>(grantedAuthoritiesConverter.convert(jwt));
            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("authenticated"))) {
                authorities.clear();
                authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
                var appMetadata = jwt.getClaimAsMap("app_metadata");
                if (appMetadata != null && "admin".equals(appMetadata.get("role"))) {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
                }
            }
            return authorities;
        });
        return converter;
    }
}
