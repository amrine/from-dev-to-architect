package io.teampulse.identity.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class IdentityFlywayConfiguration {

    @Bean
    FlywayMigrationInitializer identityFlywayInitializer(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .createSchemas(true)
            .schemas("tp_identity")
            .defaultSchema("tp_identity")
            .table("flyway_schema_history")
            .locations("classpath:db/migration/identity")
            .load();

        return new FlywayMigrationInitializer(flyway);
    }
}
