package io.teampulse.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.teampulse.TpAppApplication;

import java.util.Set;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

final class BusinessModuleArchitectureRules {

    static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages(TpAppApplication.class.getPackageName());

    private BusinessModuleArchitectureRules() {
    }

    static Stream<ModuleRule> rulesFor(ArchitectureModules.BusinessModule module) {
        ModulePackages packages = ModulePackages.from(module);
        return Stream.of(
            rule(module, "R01", r01(packages)),
            rule(module, "R02", r02(packages)),
            rule(module, "R03", r03(packages)),
            rule(module, "R04", r04(packages)),
            rule(module, "R05", r05(packages)),
            rule(module, "R06", r06(packages)),
            rule(module, "R07", r07(packages)),
            rule(module, "R08", r08(packages)),
            rule(module, "R09", r09(packages))
        );
    }

    private static ArchRule r01(ModulePackages packages) {
        return CompositeArchRule.of(noClasses()
                .that().resideInAPackage(packages.domain())
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence.."))
            .and(noClasses()
                .that().resideInAPackage(packages.domain())
                .should().dependOnClassesThat().resideInAnyPackage(packages.infrastructure(), packages.config(), packages.api(), packages.events()))
            .as("R01 - le domaine reste indépendant de Spring, JPA et des couches externes")
            .because("le cœur métier doit rester utilisable sans framework");
    }

    private static ArchRule r02(ModulePackages packages) {
        return CompositeArchRule.of(noClasses()
                .that().resideInAPackage(packages.domain())
                .should().dependOnClassesThat().resideInAPackage(packages.application()))
            .and(noClasses()
                .that().resideInAPackage(packages.application())
                .should().dependOnClassesThat().resideInAnyPackage(packages.infrastructure(), packages.config()))
            .as("R02 - les dépendances application-domaine sont orientées vers le domaine")
            .because("l'application orchestre le domaine sans connaître les adapters");
    }

    private static ArchRule r03(ModulePackages packages) {
        return CompositeArchRule.of(noClasses()
                .that().resideInAnyPackage(packages.portIn(), packages.portOut())
                .should().dependOnClassesThat()
                .resideInAPackage(packages.infrastructure()))
            .and(classes()
                .that().resideInAPackage(packages.web())
                .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should(dependOnAtLeastOneClassIn(packages.portIn())))
            .and(noClasses()
                .that().resideInAPackage(packages.web())
                .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().dependOnClassesThat()
                .resideInAPackage(packages.portOut()))
            .and(classes()
                .that().resideInAPackage(packages.persistence())
                .and(dependOnAtLeastOneClassInPredicate(packages.portOut()))
                .should(implementAtLeastOneInterfaceIn(packages.portOut())))
            .as("R03 - les ports appartiennent à l'application et pilotent les adapters")
            .because("les adapters dépendent des ports, jamais l'inverse");
    }

    private static ArchRule r04(ModulePackages packages) {
        return CompositeArchRule.of(classes()
                .that(resideInModule(packages))
                .and(annotatedWithAny(
                    "jakarta.persistence.Entity",
                    "jakarta.persistence.Embeddable",
                    "jakarta.persistence.MappedSuperclass",
                    "jakarta.persistence.Converter"
                ))
                .should().resideInAPackage(packages.persistence()))
            .and(classes()
                .that(resideInModule(packages))
                .and().areAssignableTo("org.springframework.data.repository.Repository")
                .should().resideInAPackage(packages.persistence()))
            .and(noClasses()
                .that().resideInAnyPackage(packages.domain(), packages.application(), packages.api(), packages.events())
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data..", packages.persistence()))
            .as("R04 - JPA et Spring Data restent confinés dans infrastructure.persistence")
            .because("la persistence ne doit traverser ni le cœur ni la surface publique");
    }

    private static ArchRule r05(ModulePackages packages) {
        return CompositeArchRule.of(classes()
                .that(resideInModule(packages))
                .and(annotatedWithAny(
                    "org.springframework.stereotype.Controller",
                    "org.springframework.web.bind.annotation.RestController"
                ))
                .should().resideInAPackage(packages.web()))
            .and(noClasses().that().resideInAnyPackage(packages.domain(), packages.application(), packages.api(), packages.events())
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "org.springframework.http..", "jakarta.servlet.."))
            .and(noClasses().that().resideInAPackage(packages.web())
                .should().dependOnClassesThat().resideInAnyPackage(packages.persistence(), "org.springframework.data.."))
            .as("R05 - le transport HTTP reste dans infrastructure.web")
            .because("un contrôleur passe par un port entrant et ne connaît pas la persistence");
    }

    private static ArchRule r06(ModulePackages packages) {
        return CompositeArchRule.of(noClasses()
                .that().resideInAPackage(packages.web())
                .should().dependOnClassesThat()
                .resideInAPackage(packages.messaging()))
            .and(noClasses()
                .that().resideInAPackage(packages.persistence())
                .should().dependOnClassesThat().resideInAnyPackage(packages.web(), packages.messaging()))
            .and(noClasses()
                .that().resideInAPackage(packages.messaging())
                .should().dependOnClassesThat().resideInAnyPackage(packages.web(), packages.persistence()))
            .as("R06 - les adapters web, persistence et messaging restent isolés")
            .because("la collaboration entre adapters doit passer par les ports de l'application");
    }

    private static ArchRule r07(ModulePackages packages) {
        return noClasses().that().resideInAnyPackage(packages.api(), packages.events())
            .should().dependOnClassesThat().resideInAnyPackage(packages.domain(), packages.application(), packages.infrastructure(), packages.config())
            .as("R07 - api et events forment une surface publique autonome")
            .because("les contrats inter-modules ne doivent exposer aucun type interne");
    }

    private static ArchRule r08(ModulePackages packages) {
        return CompositeArchRule.of(classes()
                .that(resideInModule(packages))
                .and().areAnnotatedWith("org.springframework.context.annotation.Configuration")
                .should().resideInAPackage(packages.config()))
            .and(classes().that(resideInModule(packages))
                .and().areAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")
                .should().resideInAPackage(packages.config()))
            .and(classes().that(resideInModule(packages))
                .and().areAnnotatedWith("org.springframework.stereotype.Repository")
                .should().resideInAPackage(packages.persistence()))
            .and(classes().that().resideInAPackage(packages.base())
                .should().haveSimpleName("package-info"))
            .as("R08 - les composants techniques et métier sont placés dans leur zone")
            .because("la racine d'un module ne doit contenir que sa déclaration package-info");
    }

    private static ArchRule r09(ModulePackages packages) {
        return CompositeArchRule.of(slices()
                .matching(packages.base() + ".(*)..")
                .should().beFreeOfCycles())
            .and(slices()
                .matching(packages.base() + ".infrastructure.(*)..")
                .should().beFreeOfCycles())
            .as("R09 - les zones internes et les adapters sont exempts de cycles")
            .because("la direction des dépendances vers le cœur doit rester acyclique");
    }

    private static ModuleRule rule(ArchitectureModules.BusinessModule module, String id, ArchRule rule) {
        return new ModuleRule(module.name(), id, rule.allowEmptyShould(true));
    }

    private static ArchCondition<JavaClass> dependOnAtLeastOneClassIn(String... packagePatterns) {
        Set<String> patterns = Set.of(packagePatterns);

        return new ArchCondition<>("dépendre d'au moins un type dans " + patterns) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = item.getDirectDependenciesFromSelf()
                    .stream()
                    .map(dependency -> dependency.getTargetClass().getPackageName())
                    .anyMatch(packageName -> patterns.stream().anyMatch(pattern -> matchesPackage(pattern, packageName)));

                events.add(new SimpleConditionEvent(
                    item,
                    satisfied,
                    item.getName() + " doit dépendre d'au moins un type dans " + patterns
                ));
            }
        };
    }

    private static DescribedPredicate<JavaClass> dependOnAtLeastOneClassInPredicate(String packagePattern) {
        return new DescribedPredicate<>("dépend d'un type dans " + packagePattern) {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getDirectDependenciesFromSelf()
                    .stream()
                    .map(dependency -> dependency.getTargetClass().getPackageName())
                    .anyMatch(packageName -> matchesPackage(packagePattern, packageName));
            }
        };
    }

    private static ArchCondition<JavaClass> implementAtLeastOneInterfaceIn(String packagePattern) {
        return new ArchCondition<>("implémenter un port sortant dans " + packagePattern) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied = item.getAllRawInterfaces()
                    .stream()
                    .map(JavaClass::getPackageName)
                    .anyMatch(packageName -> matchesPackage(packagePattern, packageName));

                events.add(new SimpleConditionEvent(
                    item,
                    satisfied,
                    item.getName() + " doit implémenter un port sortant de l'application"
                ));
            }
        };
    }

    private static DescribedPredicate<JavaClass> resideInModule(ModulePackages packages) {
        return new DescribedPredicate<>("réside dans le module " + packages.base()) {
            @Override
            public boolean test(JavaClass javaClass) {
                String packageName = javaClass.getPackageName();

                return packageName.equals(packages.base()) || packageName.startsWith(packages.base() + ".");
            }
        };
    }

    private static DescribedPredicate<JavaClass> annotatedWithAny(String... annotationNames) {
        Set<String> annotations = Set.of(annotationNames);

        return new DescribedPredicate<>("annotée avec l'une des annotations " + annotations) {
            @Override
            public boolean test(JavaClass javaClass) {
                return annotations.stream().anyMatch(javaClass::isAnnotatedWith);
            }
        };
    }

    private static boolean matchesPackage(String pattern, String packageName) {
        String prefix = pattern.endsWith("..")
            ? pattern.substring(0, pattern.length() - 2)
            : pattern;

        return packageName.equals(prefix) || packageName.startsWith(prefix + ".");
    }

    record ModuleRule(String module, String id, ArchRule rule) { }

    record ModulePackages(
        String base,
        String domain,
        String application,
        String portIn,
        String portOut,
        String infrastructure,
        String web,
        String persistence,
        String messaging,
        String config,
        String api,
        String events
    ) {

        static ModulePackages from(ArchitectureModules.BusinessModule module) {
            String base = module.basePackage();
            return new ModulePackages(
                base,
                base + ".domain..",
                base + ".application..",
                base + ".application.port.in..",
                base + ".application.port.out..",
                base + ".infrastructure..",
                base + ".infrastructure.web..",
                base + ".infrastructure.persistence..",
                base + ".infrastructure.messaging..",
                base + ".config..",
                base + ".api..",
                base + ".events.."
            );
        }
    }
}
