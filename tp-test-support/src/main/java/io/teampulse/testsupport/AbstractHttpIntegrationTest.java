package io.teampulse.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Shared support for black-box tests that send requests to a real HTTP server.
 */
public abstract class AbstractHttpIntegrationTest extends AbstractJpaIntegrationTest {

    @Autowired
    protected RestTestClient restClient;
}
