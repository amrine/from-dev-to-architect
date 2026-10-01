package io.teampulse.testsupport.persistence;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Clears application data between real-HTTP integration tests while
 * preserving Flyway migration history.
 */
public final class PostgreSQLDatabaseCleaner {

    private static final String APPLICATION_TABLES_QUERY = """
        SELECT string_agg(
            format('%I.%I', schemaname, tablename),
            ', ' ORDER BY schemaname, tablename
        )
        FROM pg_tables
        WHERE schemaname <> 'information_schema'
          AND left(schemaname, 3) <> 'pg_'
          AND tablename <> 'flyway_schema_history'
        """;

    private final DataSource dataSource;

    public PostgreSQLDatabaseCleaner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Truncates all application tables, including rows written by the HTTP
     * server thread, without removing Flyway's migration history.
     *
     * @throws IllegalStateException if the database cannot be cleaned
     */
    public void clean() {
        try (Connection connection = dataSource.getConnection()) {
            String applicationTables;
            try (Statement query = connection.createStatement();
                 ResultSet result = query.executeQuery(APPLICATION_TABLES_QUERY)) {
                if (!result.next()) {
                    return;
                }
                applicationTables = result.getString(1);
            }

            if (applicationTables == null || applicationTables.isBlank()) {
                return;
            }

            try (Statement truncate = connection.createStatement()) {
                truncate.execute(
                    "TRUNCATE TABLE " + applicationTables + " RESTART IDENTITY CASCADE"
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                "Unable to isolate PostgreSQL HTTP integration test data",
                exception
            );
        }
    }
}
