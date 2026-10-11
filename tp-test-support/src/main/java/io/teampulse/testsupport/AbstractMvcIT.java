package io.teampulse.testsupport;

import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared support for Spring MVC integration tests using the application context.
 */
@AutoConfigureMockMvc
@Import(PersistenceIntegrationTestConfiguration.class)
public abstract class AbstractMvcIT extends AbstractJpaIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
