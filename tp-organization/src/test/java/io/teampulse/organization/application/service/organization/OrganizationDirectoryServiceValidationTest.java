package io.teampulse.organization.application.service.organization;

import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationDirectoryException;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig(
    OrganizationDirectoryServiceValidationTest.TestConfiguration.class
)
class OrganizationDirectoryServiceValidationTest {

    @Inject
    private OrganizationDirectory organizationDirectory;

    @Inject
    private OrganizationRepository organizationRepository;

    @ParameterizedTest
    @MethodSource("invalidOrganizationReferences")
    void rejectsInvalidOrganizationReferencesAsContractViolations(
        String organizationReference
    ) {
        RuntimeException exception = assertThrows(
            ConstraintViolationException.class,
            () -> organizationDirectory.check(organizationReference)
        );

        assertFalse(exception instanceof OrganizationDirectoryException);
        verifyNoInteractions(organizationRepository);
    }

    static Stream<String> invalidOrganizationReferences() {
        return Stream.of(null, "", "   ");
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

        @Bean
        static MethodValidationPostProcessor methodValidationPostProcessor() {
            return new MethodValidationPostProcessor();
        }

        @Bean
        OrganizationRepository organizationRepository() {
            return mock(OrganizationRepository.class);
        }

        @Bean
        OrganizationDirectoryService organizationDirectoryService(
            OrganizationRepository organizationRepository
        ) {
            return new OrganizationDirectoryService(organizationRepository);
        }
    }
}
