package com.trivia501.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration.
 *
 * <p>Exposes a {@link CorsConfigurationSource} bean rather than a raw
 * {@code CorsFilter} so that Spring Security's filter chain can own CORS
 * processing via {@code .cors(Customizer.withDefaults())}.  This avoids
 * double-filter ordering issues that arise when a standalone {@code CorsFilter}
 * bean sits alongside the Spring Security filter chain.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${TRIVIA501_FRONTEND_ORIGIN:}")
    private String frontendOrigin;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // Always allow localhost for development
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("http://127.0.0.1:*");
        // Add production frontend origin when set
        if (frontendOrigin != null && !frontendOrigin.isEmpty()) {
            config.addAllowedOrigin(frontendOrigin);
            log.info("CORS: allowing frontend origin {}", frontendOrigin);
        }
        config.setAllowedHeaders(List.of(
            "Content-Type", "Authorization", "X-Requested-With", "Accept", "Origin"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
