package io.teampulse.context;

import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.common.reference.ReferenceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configures the tenant context used for local application runs.
 */
@Configuration
@Profile("local")
public class LocalTenantConfiguration {

    @Bean
    TenantContextProvider tenantContextProvider(
        ReferenceFactory referenceFactory
    ) {
        return new LocalTenantContextProvider(referenceFactory);
    }
}
