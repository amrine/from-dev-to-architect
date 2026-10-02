package io.teampulse.context;

import io.teampulse.common.context.TenantContext;
import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationProvisioning;
import io.teampulse.organization.api.organization.OrganizationProvisioningCommand;
import io.teampulse.organization.api.organization.OrganizationProvisioningResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTenantContextProviderTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-0609-00000ZA7B900";
    private static final String NEXT_PROCESS_ORGANIZATION_REFERENCE =
        "ORG-2026-0609-00000ZA7B901";

    @Test
    void provisionsTheDemoOrganizationLazilyAndUsesItsPersistedReference() {
        AtomicInteger provisioningCount = new AtomicInteger();
        AtomicReference<OrganizationProvisioningCommand> command = new AtomicReference<>();
        OrganizationProvisioning organizationProvisioning = requestedCommand -> {
            provisioningCount.incrementAndGet();
            command.set(requestedCommand);
            return organization(ORGANIZATION_REFERENCE, OrganizationLifecycleState.CREATING);
        };
        LocalTenantContextProvider provider =
            new LocalTenantContextProvider(organizationProvisioning);

        assertEquals(0, provisioningCount.get());
        TenantContext firstContext = provider.current();
        TenantContext secondContext = provider.current();

        assertEquals(ORGANIZATION_REFERENCE, firstContext.tenantReference());
        assertSame(firstContext, secondContext);
        assertEquals(1, provisioningCount.get());
        assertEquals("TeamPulse Local Demo", command.get().name());
        assertEquals("Europe/Paris", command.get().timezone());
    }

    @Test
    void provisionsAnIndependentDemoOrganizationForEachProviderInstance() {
        AtomicInteger provisioningCount = new AtomicInteger();
        OrganizationProvisioning organizationProvisioning = _ -> {
            String reference = provisioningCount.incrementAndGet() == 1
                ? ORGANIZATION_REFERENCE
                : NEXT_PROCESS_ORGANIZATION_REFERENCE;
            return organization(reference, OrganizationLifecycleState.CREATING);
        };
        LocalTenantContextProvider firstProcess =
            new LocalTenantContextProvider(organizationProvisioning);
        LocalTenantContextProvider nextProcess =
            new LocalTenantContextProvider(organizationProvisioning);

        assertEquals(ORGANIZATION_REFERENCE, firstProcess.current().tenantReference());
        assertEquals(NEXT_PROCESS_ORGANIZATION_REFERENCE, nextProcess.current().tenantReference());
        assertEquals(2, provisioningCount.get());
    }

    @Test
    void provisionsOnlyOneOrganizationWhenResolvedConcurrently() throws Exception {
        int threadCount = 12;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch provisioningStarted = new CountDownLatch(1);
        CountDownLatch releaseProvisioning = new CountDownLatch(1);
        AtomicInteger provisioningCount = new AtomicInteger();
        OrganizationProvisioning organizationProvisioning = _ -> {
            provisioningCount.incrementAndGet();
            provisioningStarted.countDown();
            try {
                if (!releaseProvisioning.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                        "Timed out while waiting to provision the local demo organization"
                    );
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Local demo organization provisioning was interrupted",
                    exception
                );
            }
            return organization(ORGANIZATION_REFERENCE, OrganizationLifecycleState.CREATING);
        };
        LocalTenantContextProvider provider =
            new LocalTenantContextProvider(organizationProvisioning);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<TenantContext>> futures = new ArrayList<>();

        try {
            for (int thread = 0; thread < threadCount; thread++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return provider.current();
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(provisioningStarted.await(5, TimeUnit.SECONDS));
            releaseProvisioning.countDown();

            TenantContext expectedContext = futures.getFirst().get(5, TimeUnit.SECONDS);
            for (Future<TenantContext> future : futures) {
                assertSame(expectedContext, future.get(5, TimeUnit.SECONDS));
            }
        } finally {
            releaseProvisioning.countDown();
            executor.shutdownNow();
        }

        assertEquals(1, provisioningCount.get());
    }

    @Test
    void retriesResolutionAfterProvisioningFailure() {
        RuntimeException provisioningFailure =
            new IllegalStateException("Organization provisioning failed");
        AtomicInteger provisioningCount = new AtomicInteger();
        OrganizationProvisioning organizationProvisioning = _ -> {
            if (provisioningCount.getAndIncrement() == 0) {
                throw provisioningFailure;
            }
            return organization(ORGANIZATION_REFERENCE, OrganizationLifecycleState.CREATING);
        };
        LocalTenantContextProvider provider =
            new LocalTenantContextProvider(organizationProvisioning);

        RuntimeException thrownException = assertThrows(
            RuntimeException.class,
            provider::current
        );
        TenantContext tenantContext = provider.current();

        assertSame(provisioningFailure, thrownException);
        assertEquals(ORGANIZATION_REFERENCE, tenantContext.tenantReference());
        assertEquals(2, provisioningCount.get());
    }

    @Test
    void refusesAndRetriesAnOrganizationThatIsNotCreating() {
        AtomicInteger provisioningCount = new AtomicInteger();
        OrganizationProvisioning organizationProvisioning = _ -> {
            OrganizationLifecycleState state = provisioningCount.getAndIncrement() == 0
                ? OrganizationLifecycleState.ACTIVE
                : OrganizationLifecycleState.CREATING;
            return organization(ORGANIZATION_REFERENCE, state);
        };
        LocalTenantContextProvider provider =
            new LocalTenantContextProvider(organizationProvisioning);

        assertThrows(IllegalStateException.class, provider::current);
        assertEquals(ORGANIZATION_REFERENCE, provider.current().tenantReference());
        assertEquals(2, provisioningCount.get());
    }

    @Test
    void rejectsANullProvisioningContract() {
        assertThrows(
            NullPointerException.class,
            () -> new LocalTenantContextProvider(null)
        );
    }

    private static OrganizationProvisioningResult organization(
        String reference,
        OrganizationLifecycleState status
    ) {
        return new OrganizationProvisioningResult(reference, status);
    }
}
