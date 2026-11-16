package io.teampulse.team;

import io.teampulse.config.ReferenceConfiguration;
import io.teampulse.context.DeterministicTenantContextTestConfiguration;
import io.teampulse.testsupport.persistence.PostgreSQLTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest
@Import({
    PostgreSQLTestConfiguration.class,
    ReferenceConfiguration.class,
    DeterministicTenantContextTestConfiguration.class,
    TeamModuleTestConfiguration.class
})
public class TeamModuleTests {

    @Test
    void bootstrapsModule() {
    }
}
