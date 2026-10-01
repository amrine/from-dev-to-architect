package io.teampulse.testsupport.web;

import io.teampulse.testsupport.persistence.PostgreSQLDatabaseCleaner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class HttpIntegrationTestConfiguration {

    @Bean
    PostgreSQLDatabaseCleaner postgreSQLDatabaseCleaner(DataSource dataSource) {
        return new PostgreSQLDatabaseCleaner(dataSource);
    }
}
