package io.teampulse.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import io.teampulse.TpAppApplication;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

final class ArchitectureModules {

    private static final ApplicationModules MODULES =
        ApplicationModules.of(TpAppApplication.class, new ImportOption.DoNotIncludeTests());

    private static final Set<ApplicationModule> SHARED_MODULES = MODULES.getSharedModules();

    private static final List<BusinessModule> BUSINESS_MODULES =
        MODULES.stream()
            .filter(module -> !SHARED_MODULES.contains(module))
            .map(ArchitectureModules::toBusinessModule)
            .sorted(Comparator.comparing(BusinessModule::name))
            .toList();

    private ArchitectureModules() {
    }

    static List<BusinessModule> businessModules() {
        return BUSINESS_MODULES;
    }

    private static BusinessModule toBusinessModule(ApplicationModule module) {
        return new BusinessModule(
            module.getIdentifier().toString(),
            module.getBasePackage().getName()
        );
    }

    record BusinessModule(String name, String basePackage) { }
}
