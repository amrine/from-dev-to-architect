package io.teampulse.testsupport.web;

import io.teampulse.testsupport.persistence.PostgreSQLDatabaseCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Shared support for black-box tests that send requests to a real HTTP server.
 */
public abstract class AbstractHttpIntegrationTest {

    @Autowired
    protected RestTestClient restTestClient;

    @Autowired
    private PostgreSQLDatabaseCleaner databaseCleaner;

    @BeforeEach
    final void cleanDatabaseBeforeTest() {
        databaseCleaner.clean();
    }

    @AfterEach
    final void cleanDatabaseAfterTest() {
        databaseCleaner.clean();
    }
}
