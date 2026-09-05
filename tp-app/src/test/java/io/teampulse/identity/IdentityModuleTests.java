package io.teampulse.identity;

import io.teampulse.testsupport.persistence.PostgreSQLTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest
@Import({PostgreSQLTestConfiguration.class})
public class IdentityModuleTests {

    @Test
    void bootstrapsModule() {
    }
}
