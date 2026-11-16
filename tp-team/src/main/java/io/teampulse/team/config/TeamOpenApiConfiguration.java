package io.teampulse.team.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class TeamOpenApiConfiguration {

    @Bean
    OpenApiCustomizer teamApiMetadata(@Value("${teampulse.openapi.info-version}") String apiVersion) {
        return openApi -> {
            openApi.setInfo(new Info().title("TeamPulse Team API").version(apiVersion));
            openApi.setServers(List.of(new Server().url("/")));
        };
    }
}
