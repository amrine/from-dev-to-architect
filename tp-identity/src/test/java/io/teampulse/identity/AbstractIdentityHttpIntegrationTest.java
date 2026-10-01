package io.teampulse.identity;

import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.web.AbstractHttpIntegrationTest;
import io.teampulse.testsupport.web.HttpIntegrationTestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@SpringBootTest(
    classes = IdentityTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@Import({
    PersistenceIntegrationTestConfiguration.class,
    HttpIntegrationTestConfiguration.class
})
public abstract class AbstractIdentityHttpIntegrationTest extends AbstractHttpIntegrationTest {
}
