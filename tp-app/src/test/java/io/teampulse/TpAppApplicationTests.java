package io.teampulse;

import io.teampulse.testsupport.persistence.PostgreSQLTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgreSQLTestConfiguration.class)
class TpAppApplicationTests {

    @Test
    void contextLoads() {
    }

}
