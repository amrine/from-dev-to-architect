package io.teampulse.identity.fixture;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;

import java.util.concurrent.atomic.AtomicInteger;

/** Supplies a fixed tenant and records how many times it is resolved during a test. */
public class DeterministicTenantContextProvider implements TenantContextProvider {

    private final TenantContext tenantContext;
    private final AtomicInteger calls = new AtomicInteger();

    /** Creates a provider for the tenant chosen by the test application. */
    public DeterministicTenantContextProvider(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /** Returns the configured tenant and records this resolution. */
    @Override
    public TenantContext current() {
        calls.incrementAndGet();
        return tenantContext;
    }

    /** Returns the number of resolutions since the last reset. */
    public int calls() {
        return calls.get();
    }

    /** Clears the resolution count before the next scenario. */
    public void resetCalls() {
        calls.set(0);
    }
}
