package com.football501.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration.
 *
 * <p>Enables Spring Data JPA auditing so that {@code @CreatedDate} and
 * {@code @LastModifiedDate} annotations on entity fields are populated
 * automatically by {@link org.springframework.data.jpa.domain.support.AuditingEntityListener}
 * on every persist/update lifecycle event.
 *
 * <p><strong>WebMvcTest note</strong>: {@code @WebMvcTest} slice tests do not
 * load a real JPA context.  Those test classes must add
 * {@code @MockitoBean JpaMetamodelMappingContext} to satisfy the dependency
 * that {@code @EnableJpaAuditing} registers.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // No bean declarations needed — @EnableJpaAuditing does the work.
}
