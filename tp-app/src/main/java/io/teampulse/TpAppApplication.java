package io.teampulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic(
    systemName = "TeamPulse",
    sharedModules = "common"
)
public class TpAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TpAppApplication.class, args);
    }

}
