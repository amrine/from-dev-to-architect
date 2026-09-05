package io.teampulse.testsupport.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class PostgreSQLTestConfiguration {

    @Bean
    @ServiceConnection
    TeamPulsePostgreSQLContainer postgresContainer() {
        return new TeamPulsePostgreSQLContainer();
    }
}
