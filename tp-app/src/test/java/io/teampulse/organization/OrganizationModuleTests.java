package io.teampulse.organization;

import io.teampulse.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest
@Import({TestcontainersConfiguration.class})
public class OrganizationModuleTests {

    @Test
    void bootstrapsModule() {}
}
