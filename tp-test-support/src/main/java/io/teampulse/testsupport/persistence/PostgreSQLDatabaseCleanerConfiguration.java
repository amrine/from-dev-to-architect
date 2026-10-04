package io.teampulse.testsupport.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class PostgreSQLDatabaseCleanerConfiguration {

    @Bean
    PostgreSQLDatabaseCleaner postgreSQLDatabaseCleaner(DataSource dataSource) {
        return new PostgreSQLDatabaseCleaner(dataSource);
    }
}
