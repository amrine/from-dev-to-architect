package io.teampulse.administration;

import io.teampulse.AdministrationTestApplication;
import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.web.AbstractHttpIntegrationTest;
import io.teampulse.testsupport.web.HttpIntegrationTestConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = AdministrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@ActiveProfiles("local")
@Import({
    PersistenceIntegrationTestConfiguration.class,
    HttpIntegrationTestConfiguration.class
})
public abstract class AbstractAdministrationHttpIntegrationTest extends AbstractHttpIntegrationTest {
}
