package io.teampulse.identity.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class IdentityOpenApiConfiguration {

    @Bean
    OpenApiCustomizer identityApiMetadata(@Value("${teampulse.openapi.info-version:v0}") String apiVersion) {
        return openApi -> {
            openApi.setInfo(new Info().title("TeamPulse Identity API").version(apiVersion));
            openApi.setServers(List.of(new Server().url("/")));
        };
    }
}
