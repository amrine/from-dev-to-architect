package io.teampulse.organization;

import io.teampulse.testsupport.persistence.PostgreSQLTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest
@Import(PostgreSQLTestConfiguration.class)
public class OrganizationModuleTests {

    @Test
    void bootstrapsModule() {}
}
