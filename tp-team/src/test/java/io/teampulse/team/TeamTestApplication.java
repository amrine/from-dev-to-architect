package io.teampulse.team;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration
@SpringBootApplication
public class TeamTestApplication {

    @Bean
    Clock teamClock() {
        return Clock.systemUTC();
    }
}
