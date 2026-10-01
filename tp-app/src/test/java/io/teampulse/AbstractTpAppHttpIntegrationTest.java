package io.teampulse;

import io.teampulse.testsupport.persistence.PostgreSQLTestConfiguration;
import io.teampulse.testsupport.web.AbstractHttpIntegrationTest;
import io.teampulse.testsupport.web.HttpIntegrationTestConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = TpAppApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@ActiveProfiles("local")
@Import({
    PostgreSQLTestConfiguration.class,
    HttpIntegrationTestConfiguration.class
})
public abstract class AbstractTpAppHttpIntegrationTest extends AbstractHttpIntegrationTest {
}
