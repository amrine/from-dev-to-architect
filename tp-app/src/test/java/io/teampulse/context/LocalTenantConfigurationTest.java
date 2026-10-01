package io.teampulse.context;

import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationProvisioning;
import io.teampulse.organization.api.organization.OrganizationProvisioningResult;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTenantConfigurationTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-0609-00000ZA7B900";

    @Test
    void provisionsOnePersistedDemoTenantInLocalAndDevelopmentProfiles() {
        for (String profile : new String[] {"local", "development"}) {
            try (var context = new AnnotationConfigApplicationContext()) {
                context.getEnvironment().setActiveProfiles(profile);
                context.register(LocalTenantConfiguration.class);
                context.registerBean(
                    OrganizationProvisioning.class,
                    () -> _ -> new OrganizationProvisioningResult(
                        ORGANIZATION_REFERENCE,
                        OrganizationLifecycleState.CREATING
                    )
                );
                context.refresh();

                Map<String, TenantContextProvider> providers =
                    context.getBeansOfType(TenantContextProvider.class);

                assertEquals(1, providers.size());
                TenantContextProvider provider = context.getBean(TenantContextProvider.class);
                assertInstanceOf(LocalTenantContextProvider.class, provider);
                assertSame(provider, context.getBean(TenantContextProvider.class));
                assertEquals(ORGANIZATION_REFERENCE, provider.current().tenantReference());
            }
        }
    }

    @Test
    void doesNotRegisterTenantProviderWithoutAnAllowedProfile() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(LocalTenantConfiguration.class);
            context.refresh();

            assertTrue(context.getBeansOfType(TenantContextProvider.class).isEmpty());
        }
    }
}
