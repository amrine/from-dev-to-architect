package io.teampulse.organization;

import io.teampulse.organization.fixture.DeterministicUserDirectory;
import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.reference.ReferenceFactoryTestConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration
@SpringBootApplication
@Import({PersistenceIntegrationTestConfiguration.class, ReferenceFactoryTestConfiguration.class})
public class OrganizationTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrganizationTestApplication.class, args);
    }

    @Bean
    DeterministicUserDirectory userDirectory() {
        return new DeterministicUserDirectory();
    }
}
