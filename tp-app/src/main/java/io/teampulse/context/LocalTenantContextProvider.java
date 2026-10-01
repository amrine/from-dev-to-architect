package io.teampulse.context;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationProvisioning;
import io.teampulse.organization.api.organization.OrganizationProvisioningCommand;
import io.teampulse.organization.api.organization.OrganizationProvisioningResult;

import java.util.Objects;

/**
 * Provides one tenant context for the lifetime of a local application run.
 */
public final class LocalTenantContextProvider
    implements TenantContextProvider {

    private static final String DEMO_ORGANIZATION_NAME = "TeamPulse Local Demo";
    private static final String DEMO_ORGANIZATION_TIMEZONE = "Europe/Paris";

    private final OrganizationProvisioning organizationProvisioning;

    private volatile TenantContext tenantContext;

    public LocalTenantContextProvider(OrganizationProvisioning organizationProvisioning) {
        this.organizationProvisioning = Objects.requireNonNull(
            organizationProvisioning,
            "organizationProvisioning must not be null"
        );
    }

    @Override
    public TenantContext current() {
        TenantContext resolvedContext = tenantContext;

        if (resolvedContext == null) {
            synchronized (this) {
                resolvedContext = tenantContext;

                if (resolvedContext == null) {
                    resolvedContext = createDemoOrganizationContext();
                    tenantContext = resolvedContext;
                }
            }
        }

        return resolvedContext;
    }

    private TenantContext createDemoOrganizationContext() {
        OrganizationProvisioningResult organization = organizationProvisioning.createOrganization(
            new OrganizationProvisioningCommand(
                DEMO_ORGANIZATION_NAME,
                DEMO_ORGANIZATION_TIMEZONE
            )
        );
        if (organization.status() != OrganizationLifecycleState.CREATING) {
            throw new IllegalStateException(
                "The local demo organization must remain in CREATING"
            );
        }
        return new TenantContext(organization.organizationReference());
    }
}
