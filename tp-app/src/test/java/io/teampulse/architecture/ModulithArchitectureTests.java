package io.teampulse.architecture;

import io.teampulse.TpAppApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

public class ModulithArchitectureTests {

    private final ApplicationModules modules =
        ApplicationModules.of(TpAppApplication.class);

    @Test
    void verifiesArchitecture() {
        modules.verify();
    }

    @Test
    void assertsExpectedModules() {
        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .containsExactlyInAnyOrder("common", "identity", "organization", "team");
    }
}
