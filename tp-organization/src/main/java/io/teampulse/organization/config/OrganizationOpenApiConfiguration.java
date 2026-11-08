package io.teampulse.organization.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class OrganizationOpenApiConfiguration {

    /**
     * Sets the API title, version and relative server URL using the same policy as identity.
     *
     * @param apiVersion required product version supplied by Maven or application configuration
     * @return the customizer applied when the OpenAPI document is generated
     */
    @Bean
    OpenApiCustomizer organizationApiMetadata(@Value("${teampulse.openapi.info-version}") String apiVersion) {
        return openApi -> {
            openApi.setInfo(new Info().title("TeamPulse Organization API").version(apiVersion));
            openApi.setServers(List.of(new Server().url("/")));
        };
    }
}
