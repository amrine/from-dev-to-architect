package io.teampulse.testsupport.persistence;

import io.teampulse.testsupport.transaction.JpaTransactionManagerProbeConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
    PostgreSQLTestConfiguration.class,
    JpaAuditingTestConfiguration.class,
    JpaTransactionManagerProbeConfiguration.class,
    PostgreSQLDatabaseCleanerConfiguration.class
})
public class PersistenceIntegrationTestConfiguration {}
