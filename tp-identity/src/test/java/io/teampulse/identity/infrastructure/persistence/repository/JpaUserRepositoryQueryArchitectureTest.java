package io.teampulse.identity.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaUserRepositoryQueryArchitectureTest {

    @Test
    void everyDeclaredQueryIsTenantScoped() {
        Arrays.stream(JpaUserRepository.class.getDeclaredMethods())
            .forEach(method -> assertTrue(
                method.getName().contains("OrganizationReference"),
                method.getName() + " must be tenant-scoped"));
    }
}
