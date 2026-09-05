package io.teampulse.testsupport.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@TestConfiguration
@EnableJpaAuditing(
    auditorAwareRef = "testAuditorAware",
    dateTimeProviderRef = "testAuditDateTimeProvider"
)
public class JpaAuditingTestConfiguration {

    @Bean
    AuditorAware<String> testAuditorAware() {
        return () -> Optional.of("SYSTEM");
    }

    @Bean
    MutableAuditDateTimeProvider testAuditDateTimeProvider() {
        return new MutableAuditDateTimeProvider();
    }
}
