package io.teampulse.administration;

import io.teampulse.AdministrationTestApplication;
import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.persistence.PostgreSQLDatabaseCleaner;
import io.teampulse.testsupport.web.HttpIntegrationTestConfiguration;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    classes = AdministrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Import({
    PersistenceIntegrationTestConfiguration.class,
    HttpIntegrationTestConfiguration.class
})
public abstract class AbstractAdministrationIntegrationTest {

    @Inject
    private PostgreSQLDatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabaseBeforeTest() {
        databaseCleaner.clean();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {
        databaseCleaner.clean();
    }
}
