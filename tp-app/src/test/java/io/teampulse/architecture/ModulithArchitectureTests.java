package io.teampulse.architecture;

import io.teampulse.TpAppApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModulithArchitectureTests {

    private final ApplicationModules modules =
        ApplicationModules.of(TpAppApplication.class);

    @Test
    void verifiesArchitecture() {
        modules.verify();
    }

    @Test
    void printsModules() {
        modules.forEach(System.out::println);
    }
}
