package io.teampulse.testsupport.persistence;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TeamPulsePostgreSQLContainer extends PostgreSQLContainer {

    private static final String DEFAULT_POSTGRESQL_IMAGE = "postgres:18-bookworm";

    private final Map<String, String> postgresConfig = new LinkedHashMap<>();

    public TeamPulsePostgreSQLContainer() {
        super(DockerImageName.parse(DEFAULT_POSTGRESQL_IMAGE).asCompatibleSubstituteFor("postgres"));
        this.withTmpFs(Collections.singletonMap("/var/lib/postgresql/18/docker", "rw"));
        this.withDatabaseName("teampulse");
        this.withUsername("teampulse");
        this.withPassword("teampulse");
    }

    public TeamPulsePostgreSQLContainer withPostgresConfig(String key, String value) {
        postgresConfig.put(key, value);
        return this;
    }

    public TeamPulsePostgreSQLContainer withPostgresConfigs(Map<String, String> configs) {
        postgresConfig.putAll(configs);
        return this;
    }

    @Override
    public void start() {
        applyPostgresCommand();
        super.start();
    }

    private void applyPostgresCommand() {
        List<String> commands = new ArrayList<>();
        commands.add("postgres");
        postgresConfig.forEach((key, value) -> {
            commands.add("-c");
            commands.add(key + "=" + value);
        });
        this.withCommand(commands.toArray(new String[0]));
    }
}
