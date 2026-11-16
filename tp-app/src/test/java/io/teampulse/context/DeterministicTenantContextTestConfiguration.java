package io.teampulse.context;

import io.teampulse.common.context.TenantContext;
import io.teampulse.testsupport.context.DeterministicTenantContextProviderTestConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(DeterministicTenantContextProviderTestConfiguration.class)
public class DeterministicTenantContextTestConfiguration {

    private static final TenantContext TEST_TENANT = new TenantContext("ORG-2026-0908-00000ZA7B900");

    @Bean
    TenantContext testTenantContext() {
        return TEST_TENANT;
    }
}
