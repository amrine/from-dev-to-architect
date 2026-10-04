package io.teampulse.team;

import io.teampulse.testsupport.AbstractJpaIntegrationTest;
import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.transaction.JpaTransactionManagerProbeConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TeamTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({PersistenceIntegrationTestConfiguration.class, JpaTransactionManagerProbeConfiguration.class})
public abstract class AbstractIntegrationTest extends AbstractJpaIntegrationTest {}
