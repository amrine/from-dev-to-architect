package io.teampulse.context;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.common.reference.MonotonicReferenceFactory;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.persistence.config.ReferenceConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTenantConfigurationTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-0609-00000ZA7B900";

    @Test
    void registersOneLocalTenantProviderWithTheRealReferenceFactory() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("local");
            context.register(
                ReferenceConfiguration.class,
                LocalTenantConfiguration.class
            );
            context.refresh();

            Map<String, TenantContextProvider> providers =
                context.getBeansOfType(TenantContextProvider.class);

            assertEquals(1, providers.size());

            TenantContextProvider provider =
                context.getBean(TenantContextProvider.class);
            assertInstanceOf(LocalTenantContextProvider.class, provider);
            assertSame(
                context.getBean(TenantContextProvider.class),
                provider
            );
            assertInstanceOf(
                MonotonicReferenceFactory.class,
                context.getBean(ReferenceFactory.class)
            );

            TenantContext firstContext = provider.current();
            TenantContext secondContext = provider.current();

            assertSame(firstContext, secondContext);
            assertTrue(firstContext.tenantReference().startsWith("ORG-"));
        }
    }

    @Test
    void delegatesTenantResolutionToTheSuppliedReferenceFactory() {
        AtomicInteger generationCount = new AtomicInteger();
        AtomicReference<String> generatedPrefix = new AtomicReference<>();
        ReferenceFactory referenceFactory = prefix -> {
            generatedPrefix.set(prefix);
            generationCount.incrementAndGet();
            return ORGANIZATION_REFERENCE;
        };

        TenantContextProvider provider = new LocalTenantConfiguration()
            .tenantContextProvider(referenceFactory);

        TenantContext tenantContext = provider.current();

        assertEquals(ORGANIZATION_REFERENCE, tenantContext.tenantReference());
        assertEquals("ORG", generatedPrefix.get());
        assertEquals(1, generationCount.get());
    }

    @Test
    void doesNotRegisterTenantProviderWithoutLocalProfile() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(
                ReferenceConfiguration.class,
                LocalTenantConfiguration.class
            );
            context.refresh();

            assertTrue(
                context.getBeansOfType(TenantContextProvider.class).isEmpty()
            );
        }
    }
}
