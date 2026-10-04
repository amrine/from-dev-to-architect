package io.teampulse.identity;

import io.teampulse.testsupport.AbstractJpaIntegrationTest;
import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = IdentityTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(PersistenceIntegrationTestConfiguration.class)
public abstract class AbstractIntegrationTest extends AbstractJpaIntegrationTest {}
