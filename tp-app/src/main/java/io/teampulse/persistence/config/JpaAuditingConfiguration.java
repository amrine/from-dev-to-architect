package io.teampulse.persistence.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfiguration {

    @Bean("auditorAware")
    @ConditionalOnMissingBean(name = "auditorAware")
    AuditorAware<String> systemAuditorAware() {
        return () -> Optional.of("SYSTEM");
    }
}