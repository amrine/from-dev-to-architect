package io.teampulse;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.teampulse.common.context.TenantContext;
import io.teampulse.testsupport.context.DeterministicTenantContextProviderTestConfiguration;
import io.teampulse.testsupport.persistence.PersistenceIntegrationTestConfiguration;
import io.teampulse.testsupport.reference.ReferenceFactoryTestConfiguration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

@TestConfiguration
@SpringBootApplication
@Import({
    PersistenceIntegrationTestConfiguration.class,
    DeterministicTenantContextProviderTestConfiguration.class,
    ReferenceFactoryTestConfiguration.class
})
public class AdministrationTestApplication {

    private static final TenantContext TEST_TENANT = new TenantContext("ORG-2026-1001-00000ZA7B900");

    public static void main(String[] args) {
        SpringApplication.run(AdministrationTestApplication.class, args);
    }

    @Bean
    TenantContext testTenantContext() {
        return TEST_TENANT;
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    OpenApiCustomizer platformAdministrationApiMetadata(
        @Value("${teampulse.openapi.info-version}") String apiVersion
    ) {
        return openApi -> {
            openApi.setInfo(new Info().title("TeamPulse Platform Administration API").version(apiVersion));
            openApi.setServers(List.of(new Server().url("/")));
        };
    }
}
