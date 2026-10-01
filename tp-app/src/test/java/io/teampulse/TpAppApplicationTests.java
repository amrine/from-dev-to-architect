package io.teampulse;

import io.teampulse.testsupport.persistence.PostgreSQLTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.flyway.enabled=true"
)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Import(PostgreSQLTestConfiguration.class)
class TpAppApplicationTests {

    @Autowired
    private RestTestClient restTestClient;

    @Value("${server.address}")
    private String serverAddress;

    @Test
    void contextLoads() {
    }

    @Test
    void configuresTheHttpServerForLoopback() {
        assertEquals("127.0.0.1", serverAddress);
    }

    @Test
    void doesNotExposeAnyUnauthenticatedApiWithoutALocalOrDevelopmentProfile() {
        restTestClient.get()
            .uri("/api/users")
            .exchange()
            .expectStatus().isNotFound();
        restTestClient.post()
            .uri("/api/organizations")
            .exchange()
            .expectStatus().isNotFound();
        restTestClient.post()
            .uri("/api/teams")
            .exchange()
            .expectStatus().isNotFound();
        restTestClient.post()
            .uri("/api/platform/organizations")
            .exchange()
            .expectStatus().isNotFound();
    }
}
