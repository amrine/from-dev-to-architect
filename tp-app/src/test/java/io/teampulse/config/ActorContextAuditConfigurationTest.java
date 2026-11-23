package io.teampulse.config;

import io.teampulse.common.context.ActorContext;
import io.teampulse.common.context.ActorContextProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActorContextAuditConfigurationTest {

    @Test
    void defaultsToTheSystemActorBeforeAuthenticatedIdentityExists() {
        ActorContextProvider provider = new ActorContextConfiguration()
            .systemActorContextProvider();

        assertEquals(ActorContext.SYSTEM_ACTOR_REFERENCE, provider.current().actorReference());
    }

    @Test
    void suppliesTheCurrentActorReferenceToJpaAuditing() {
        ActorContextProvider provider = () -> new ActorContext("ACT-2026-1001-00000ZA7B900");

        assertEquals(
            "ACT-2026-1001-00000ZA7B900",
            new JpaAuditingConfiguration()
                .actorContextAuditorAware(provider)
                .getCurrentAuditor()
                .orElseThrow()
        );
    }
}
