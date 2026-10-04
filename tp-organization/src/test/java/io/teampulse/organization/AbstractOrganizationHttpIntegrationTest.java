package io.teampulse.organization;

import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.AbstractHttpIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = OrganizationTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PersistenceIntegrationTestConfiguration.class)
public abstract class AbstractOrganizationHttpIntegrationTest extends AbstractHttpIntegrationTest {}
