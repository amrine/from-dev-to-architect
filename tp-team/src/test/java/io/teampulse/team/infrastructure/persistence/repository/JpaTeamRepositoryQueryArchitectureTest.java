package io.teampulse.team.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaTeamRepositoryQueryArchitectureTest {

    @Test
    void everyDeclaredQueryIsTenantScoped() {
        Arrays.stream(JpaTeamRepository.class.getDeclaredMethods())
            .forEach(method -> assertTrue(
                method.getName().contains("OrganizationReference"),
                method.getName() + " must be tenant-scoped"));

        Arrays.stream(JpaTeamMemberRepository.class.getDeclaredMethods())
            .forEach(method -> assertTrue(
                method.getName().contains("OrganizationReference"),
                method.getName() + " must be tenant-scoped"));
    }
}
