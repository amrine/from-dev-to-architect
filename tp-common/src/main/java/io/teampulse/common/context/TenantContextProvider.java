package io.teampulse.common.context;

/**
 * Resolves the tenant context associated with the current execution.
 */
@FunctionalInterface
public interface TenantContextProvider {

    /**
     * Returns the current tenant context.
     *
     * @return the resolved tenant context, never {@code null}
     */
    TenantContext current();
}
