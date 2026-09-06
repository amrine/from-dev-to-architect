package io.teampulse.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.common.mapping.CommonMapperConfig;
import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.modulith.core.ApplicationModules;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @ParameterizedTest
    @MethodSource("commonNamedInterfaces")
    void exposesExpectedTypesThroughCommonNamedInterfaces(
        String interfaceName,
        Set<String> expectedExposedTypes
    ) {
        var commonModule = modules.getModuleByName("common").orElseThrow();
        var interfaceContract = commonModule
            .getNamedInterfaces()
            .getByName(interfaceName)
            .orElseThrow();

        Set<String> exposedTypes = interfaceContract
            .asJavaClasses()
            .map(JavaClass::getName)
            .collect(Collectors.toUnmodifiableSet());

        assertEquals(expectedExposedTypes, exposedTypes);
    }

    private static Stream<Arguments> commonNamedInterfaces() {
        return Stream.of(
            Arguments.of("context", Set.of(
                TenantContext.class.getName(),
                TenantContextProvider.class.getName())),
            Arguments.of("mapping", Set.of(
                CommonMapperConfig.class.getName())),
            Arguments.of("reference", Set.of(
                ReferenceFactory.class.getName(),
                MonotonicReferenceFactory.class.getName()))
        );
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
