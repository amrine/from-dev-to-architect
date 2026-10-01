package io.teampulse.context;

import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.organization.api.organization.OrganizationProvisioning;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configures the tenant context used for local application runs.
 */
@Configuration
@Profile({"local", "development"})
public class LocalTenantConfiguration {

    @Bean
    TenantContextProvider tenantContextProvider(
        OrganizationProvisioning organizationProvisioning
    ) {
        return new LocalTenantContextProvider(organizationProvisioning);
    }
}
