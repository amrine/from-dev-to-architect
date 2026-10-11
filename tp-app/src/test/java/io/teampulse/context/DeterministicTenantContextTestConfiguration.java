package io.teampulse.context;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class DeterministicTenantContextTestConfiguration {

    private static final TenantContext TEST_TENANT = new TenantContext("ORG-2026-0908-00000ZA7B900");

    @Bean
    TenantContextProvider tenantContextProvider() {
        return () -> TEST_TENANT;
    }
}
