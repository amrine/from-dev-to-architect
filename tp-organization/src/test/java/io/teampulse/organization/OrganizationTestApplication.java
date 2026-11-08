package io.teampulse.organization;

import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.organization.fixture.DeterministicUserDirectory;
import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;

@TestConfiguration
@SpringBootApplication
@Import(PersistenceIntegrationTestConfiguration.class)
public class OrganizationTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrganizationTestApplication.class, args);
    }

    @Bean
    ReferenceFactory referenceFactory() {
        return new MonotonicReferenceFactory(Clock.systemUTC());
    }

    @Bean
    DeterministicUserDirectory userDirectory() {
        return new DeterministicUserDirectory();
    }
}
