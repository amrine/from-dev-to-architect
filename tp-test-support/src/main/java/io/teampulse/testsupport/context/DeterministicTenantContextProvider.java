package io.teampulse.testsupport.context;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Supplies a controllable tenant and records how many times it is resolved. */
public final class DeterministicTenantContextProvider implements TenantContextProvider {

    private final AtomicInteger calls = new AtomicInteger();
    private volatile TenantContext tenantContext;

    public DeterministicTenantContextProvider(TenantContext tenantContext) {
        this.tenantContext = Objects.requireNonNull(tenantContext, "tenantContext must not be null");
    }

    @Override
    public TenantContext current() {
        calls.incrementAndGet();
        return tenantContext;
    }

    public void setTenantContext(TenantContext tenantContext) {
        this.tenantContext = Objects.requireNonNull(tenantContext, "tenantContext must not be null");
    }

    public int calls() {
        return calls.get();
    }

    public void resetCalls() {
        calls.set(0);
    }
}
