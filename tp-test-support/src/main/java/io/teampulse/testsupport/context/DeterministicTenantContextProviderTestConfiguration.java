package io.teampulse.testsupport.context;

import io.teampulse.common.context.TenantContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class DeterministicTenantContextProviderTestConfiguration {

    @Bean
    DeterministicTenantContextProvider tenantContextProvider(TenantContext tenantContext) {
        return new DeterministicTenantContextProvider(tenantContext);
    }
}
