package io.teampulse.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ModulithArchitectureTests {

    private final ApplicationModules modules =
        ArchitectureModules.modules();

    @Test
    void verifiesArchitecture() {
        modules.verify();
    }

    @Test
    void printsModules() {
        modules.forEach(System.out::println);
    }

    @Test
    void exposesIdentityUserContractThroughDedicatedNamedInterface() {
        var identityModule = modules.getModuleByName("identity").orElseThrow();
        var userInterface = identityModule
            .getNamedInterfaces()
            .getByName("user")
            .orElseThrow();

        Set<String> exposedTypes = userInterface
            .asJavaClasses()
            .map(JavaClass::getName)
            .collect(Collectors.toUnmodifiableSet());

        assertEquals(
            Set.of(
                UserDirectory.class.getName(),
                UserAvailability.class.getName(),
                UserDirectoryException.class.getName()
            ),
            exposedTypes
        );
        assertFalse(identityModule.getNamedInterfaces().getByName("api").isPresent());
    }
}
