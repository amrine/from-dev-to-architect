package io.teampulse.organization.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class OrganizationFlywayConfiguration {

    @Bean
    FlywayMigrationInitializer organizationFlywayInitializer(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .createSchemas(true)
            .schemas("tp_organization")
            .defaultSchema("tp_organization")
            .table("flyway_schema_history")
            .locations("classpath:db/migration/organization")
            .load();

        return new FlywayMigrationInitializer(flyway);
    }
}
