package io.teampulse.context;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFactory;
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

    @Test
    void doesNotGenerateTenantBeforeFirstResolution() {
        AtomicInteger generationCount = new AtomicInteger();
        ReferenceFactory referenceFactory = _ -> {
            generationCount.incrementAndGet();
            return ORGANIZATION_REFERENCE;
        };

        new LocalTenantContextProvider(referenceFactory);

        assertEquals(0, generationCount.get());
    }

    @Test
    void generatesOrganizationReferenceOnFirstResolution() {
        AtomicInteger generationCount = new AtomicInteger();
        AtomicReference<String> generatedPrefix = new AtomicReference<>();
        ReferenceFactory referenceFactory = prefix -> {
            generatedPrefix.set(prefix);
            generationCount.incrementAndGet();
            return ORGANIZATION_REFERENCE;
        };
        LocalTenantContextProvider provider =
            new LocalTenantContextProvider(referenceFactory);

        TenantContext tenantContext = provider.current();

        assertEquals(
            ORGANIZATION_REFERENCE,
            tenantContext.tenantReference()
        );
        assertEquals("ORG", generatedPrefix.get());
        assertEquals(1, generationCount.get());
    }

    @Test
    void returnsTheSameContextForEveryResolution() {
        AtomicInteger generationCount = new AtomicInteger();
        ReferenceFactory referenceFactory = _ -> {
            generationCount.incrementAndGet();
            return ORGANIZATION_REFERENCE;
        };
        LocalTenantContextProvider provider =
            new LocalTenantContextProvider(referenceFactory);

        TenantContext firstContext = provider.current();
        TenantContext secondContext = provider.current();

        assertSame(firstContext, secondContext);
        assertEquals(1, generationCount.get());
    }

    @Test
    void generatesOnlyOneContextWhenResolvedConcurrently() throws Exception {
        int threadCount = 12;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch generationStarted = new CountDownLatch(1);
        CountDownLatch releaseGeneration = new CountDownLatch(1);
        AtomicInteger generationCount = new AtomicInteger();

        ReferenceFactory referenceFactory = _ -> {
            generationCount.incrementAndGet();
            generationStarted.countDown();

            try {
                if (!releaseGeneration.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                        "Timed out while waiting to complete tenant generation"
                    );
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Tenant generation was interrupted",
                    exception
                );
            }

            return ORGANIZATION_REFERENCE;
        };

        LocalTenantContextProvider provider =
            new LocalTenantContextProvider(referenceFactory);
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
            assertTrue(generationStarted.await(5, TimeUnit.SECONDS));
            releaseGeneration.countDown();

            TenantContext expectedContext = futures.getFirst()
                .get(5, TimeUnit.SECONDS);

            for (Future<TenantContext> future : futures) {
                assertSame(
                    expectedContext,
                    future.get(5, TimeUnit.SECONDS)
                );
            }
        } finally {
            releaseGeneration.countDown();
            executor.shutdownNow();
        }

        assertEquals(1, generationCount.get());
    }

    @Test
    void retriesResolutionAfterGenerationFailure() {
        RuntimeException generationFailure =
            new IllegalStateException("Reference generation failed");
        AtomicInteger generationCount = new AtomicInteger();
        ReferenceFactory referenceFactory = _ -> {
            if (generationCount.getAndIncrement() == 0) {
                throw generationFailure;
            }

            return ORGANIZATION_REFERENCE;
        };
        LocalTenantContextProvider provider =
            new LocalTenantContextProvider(referenceFactory);

        RuntimeException thrownException = assertThrows(
            RuntimeException.class,
            provider::current
        );
        TenantContext tenantContext = provider.current();

        assertSame(generationFailure, thrownException);
        assertEquals(
            ORGANIZATION_REFERENCE,
            tenantContext.tenantReference()
        );
        assertEquals(2, generationCount.get());
    }

    @Test
    void retriesResolutionAfterInvalidGeneratedReference() {
        AtomicInteger generationCount = new AtomicInteger();
        ReferenceFactory referenceFactory = _ ->
            generationCount.getAndIncrement() == 0
                ? " "
                : ORGANIZATION_REFERENCE;
        LocalTenantContextProvider provider =
            new LocalTenantContextProvider(referenceFactory);

        assertThrows(IllegalArgumentException.class, provider::current);

        TenantContext resolvedContext = provider.current();

        assertEquals(
            ORGANIZATION_REFERENCE,
            resolvedContext.tenantReference()
        );
        assertSame(resolvedContext, provider.current());
        assertEquals(2, generationCount.get());
    }

    @Test
    void rejectsNullReferenceFactory() {
        assertThrows(
            NullPointerException.class,
            () -> new LocalTenantContextProvider(null)
        );
    }
}
