package io.teampulse.identity.application.port.in.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.lifecycle.UserLifecycle;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TenantedInputPortsArchitectureTest {

    @Test
    void portsStartWithTenantContext() {
        Set<Class<?>> ports = Set.of(
            ListUsersUseCase.class,
            UserLifecycle.class
        );

        ports.forEach(port -> Arrays.stream(port.getDeclaredMethods()).forEach(method ->
            assertEquals(TenantContext.class, method.getParameterTypes()[0],
                port.getSimpleName() + "." + method.getName())));
    }

    @Test
    void commandsDoNotCarryOrganizationReference() {
        Set<Class<?>> commands = Set.of(
            UserProvisioningCommand.class
        );

        commands.forEach(command -> assertFalse(
            Arrays.stream(command.getRecordComponents())
                .anyMatch(component -> component.getName().equals("organizationReference")),
                command.getSimpleName() + " must not carry organizationReference"));
    }

    @Test
    void publicLifecycleContractDoesNotExposeIdentityInternals() {
        Arrays.stream(UserLifecycle.class.getDeclaredMethods()).forEach(method -> {
            assertFalse(isIdentityInternalType(method.getReturnType()),
                method.getName() + " must not return an identity internal type");
            Arrays.stream(method.getParameterTypes()).forEach(parameterType ->
                assertFalse(isIdentityInternalType(parameterType),
                    method.getName() + " must not accept an identity internal type"));
            Arrays.stream(method.getExceptionTypes()).forEach(exceptionType ->
                assertFalse(isIdentityInternalType(exceptionType),
                    method.getName() + " must not declare an identity internal exception"));
        });
    }

    private static boolean isIdentityInternalType(Class<?> type) {
        String packageName = type.getPackageName();
        return packageName.startsWith("io.teampulse.identity.application")
            || packageName.startsWith("io.teampulse.identity.domain")
            || packageName.startsWith("io.teampulse.identity.infrastructure");
    }
}
