package io.teampulse.team.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class TeamFlywayConfiguration {

    @Bean
    FlywayMigrationInitializer teamFlywayInitializer(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .createSchemas(true)
            .schemas("tp_team")
            .defaultSchema("tp_team")
            .table("flyway_schema_history")
            .locations("classpath:db/migration/team")
            .load();

        return new FlywayMigrationInitializer(flyway);
    }
}
