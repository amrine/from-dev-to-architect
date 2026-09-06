package io.teampulse.context;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.common.reference.ReferenceFactory;

import java.util.Objects;

/**
 * Provides one tenant context for the lifetime of a local application run.
 */
public final class LocalTenantContextProvider
    implements TenantContextProvider {

    private static final String ORGANIZATION_REFERENCE_PREFIX = "ORG";

    private final ReferenceFactory referenceFactory;

    private volatile TenantContext tenantContext;

    public LocalTenantContextProvider(ReferenceFactory referenceFactory) {
        this.referenceFactory = Objects.requireNonNull(
            referenceFactory,
            "referenceFactory must not be null"
        );
    }

    @Override
    public TenantContext current() {
        TenantContext resolvedContext = tenantContext;

        if (resolvedContext == null) {
            synchronized (this) {
                resolvedContext = tenantContext;

                if (resolvedContext == null) {
                    resolvedContext = new TenantContext(
                        referenceFactory.generate(
                            ORGANIZATION_REFERENCE_PREFIX
                        )
                    );
                    tenantContext = resolvedContext;
                }
            }
        }

        return resolvedContext;
    }
}
