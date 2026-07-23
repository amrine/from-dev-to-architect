package io.teampulse.architecture;

import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InternalArchitectureTests {

    @Test
    void discoversAtLeastOneBusinessModule() {
        assertFalse(ArchitectureModules.businessModules().isEmpty(), "Au moins un module métier doit être découvert");
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("moduleRules")
    void enforcesInternalArchitecture(String module, String ruleId, ArchRule rule) {
        rule.check(BusinessModuleArchitectureRules.PRODUCTION_CLASSES);
    }

    @Test
    void r10AppliesTheSameRulesToEveryBusinessModule() {
        List<List<String>> ruleIdsByModule =
            ArchitectureModules.businessModules()
                .stream()
                .map(module ->
                    BusinessModuleArchitectureRules.rulesFor(module)
                        .map(BusinessModuleArchitectureRules.ModuleRule::id)
                        .toList()
                )
                .toList();

        assertEquals(
            1,
            ruleIdsByModule.stream().distinct().count(),
            "R10 - tous les modules métier doivent utiliser le même catalogue"
        );

        assertEquals(
            List.of("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09"),
            ruleIdsByModule.getFirst(),
            "R10 - le catalogue doit contenir toutes les règles R01 à R09"
        );
    }

    private static Stream<Arguments> moduleRules() {
        return ArchitectureModules.businessModules()
            .stream()
            .flatMap(BusinessModuleArchitectureRules::rulesFor)
            .map(moduleRule -> Arguments.of(moduleRule.module(), moduleRule.id(), moduleRule.rule()));
    }
}
