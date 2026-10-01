package io.teampulse.config;

import io.teampulse.common.context.ActorContext;
import io.teampulse.common.context.ActorContextProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActorContextConfiguration {

    @Bean
    @ConditionalOnMissingBean(ActorContextProvider.class)
    ActorContextProvider systemActorContextProvider() {
        return ActorContext::system;
    }
}
