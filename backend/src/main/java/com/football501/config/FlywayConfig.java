package com.football501.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import javax.sql.DataSource;

/**
 * Flyway configuration for Spring Boot 4.
 *
 * <h3>Why this class exists</h3>
 * Spring Boot 4 removed Flyway autoconfiguration from {@code spring-boot-autoconfigure}.
 * Without it, there is no guarantee that Flyway runs before the JPA
 * {@code EntityManagerFactory} initialises and validates the schema, causing:
 * <pre>
 *   SchemaManagementException: Schema validation: missing table [entities]
 * </pre>
 *
 * <h3>How ordering is enforced</h3>
 * {@link FlywayOrderEnforcer} is a {@link BeanFactoryPostProcessor} that adds
 * {@code "flyway"} as a dependency of {@code "entityManagerFactory"} at
 * container-construction time — before any beans are instantiated.  This
 * guarantees that Flyway migrates the database before Hibernate validates it,
 * regardless of declaration order.
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    /**
     * Creates and migrates the database schema.
     *
     * <p>All Flyway settings (locations, baseline-on-migrate, etc.) are read
     * from {@code spring.flyway.*} in {@code application.yml} via the
     * {@link Flyway} fluent API.  The bean is named {@code "flyway"} so that
     * {@link FlywayOrderEnforcer} can reference it by name.
     */
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("4")
                .load();
        flyway.migrate();
        return flyway;
    }

    /**
     * Registers {@code entityManagerFactory → flyway} as an explicit bean
     * dependency at post-processing time, ensuring JPA never validates the
     * schema before Flyway has run the pending migrations.
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static FlywayOrderEnforcer flywayOrderEnforcer() {
        return new FlywayOrderEnforcer();
    }

    static class FlywayOrderEnforcer implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
                throws BeansException {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                BeanDefinition emf = beanFactory.getBeanDefinition("entityManagerFactory");
                String[] existing = emf.getDependsOn();
                if (existing == null) {
                    emf.setDependsOn("flyway");
                } else {
                    // Append only if not already present.
                    for (String dep : existing) {
                        if ("flyway".equals(dep)) return;
                    }
                    String[] updated = new String[existing.length + 1];
                    System.arraycopy(existing, 0, updated, 0, existing.length);
                    updated[existing.length] = "flyway";
                    emf.setDependsOn(updated);
                }
            }
        }
    }
}
