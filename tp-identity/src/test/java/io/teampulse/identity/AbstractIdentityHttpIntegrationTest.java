package io.teampulse.identity;

import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.AbstractHttpIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = IdentityTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PersistenceIntegrationTestConfiguration.class)
public abstract class AbstractIdentityHttpIntegrationTest extends AbstractHttpIntegrationTest {}
