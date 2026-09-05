package io.teampulse.architecture;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestSupportArchitectureTests {

    @Test
    void excludesTestSupportFromTheApplicationModuleModel() {
        assertTrue(
            ArchitectureModules.modules()
                .getModuleForPackage(ArchitectureModules.TEST_SUPPORT_BASE_PACKAGE)
                .isEmpty(),
            "tp-test-support must not be part of the Spring Modulith application model"
        );
    }

    @Test
    void preventsProductionCodeFromDependingOnTestSupport() {
        noClasses()
            .that().resideOutsideOfPackage(ArchitectureModules.TEST_SUPPORT_PACKAGE)
            .should().dependOnClassesThat()
            .resideInAPackage(ArchitectureModules.TEST_SUPPORT_PACKAGE)
            .as("production code does not depend on shared test infrastructure")
            .because("tp-test-support must remain a test-scoped dependency")
            .check(BusinessModuleArchitectureRules.PRODUCTION_CLASSES);
    }
}
