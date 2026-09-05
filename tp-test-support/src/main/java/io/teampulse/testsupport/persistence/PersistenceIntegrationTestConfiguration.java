package io.teampulse.testsupport.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
    PostgreSQLTestConfiguration.class,
    JpaAuditingTestConfiguration.class
})
public class PersistenceIntegrationTestConfiguration {
}
