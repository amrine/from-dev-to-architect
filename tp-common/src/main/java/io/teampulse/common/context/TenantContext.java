package io.teampulse.common.context;

/**
 * Carries the reference of the tenant in which a use case is executed.
 *
 * <p>A tenant represents an isolated client scope. The reference remains
 * generic so this shared type does not depend on a specific business module.
 * In TeamPulse W001, it contains the tenant reference.</p>
 *
 * @param tenantReference reference identifying the current tenant
 */
public record TenantContext(String tenantReference) {

    /**
     * Creates a tenant context without altering the supplied reference.
     *
     * @throws IllegalArgumentException if {@code tenantReference} is
     *                                  {@code null}, empty, or blank
     */
    public TenantContext {
        if (tenantReference == null || tenantReference.isBlank()) {
            throw new IllegalArgumentException(
                "tenantReference must not be null or blank"
            );
        }
    }
}
