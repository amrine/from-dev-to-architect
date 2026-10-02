package io.teampulse.identity.infrastructure.messaging;

import io.teampulse.identity.application.port.out.user.UserInvitationEventPublisher;
import io.teampulse.identity.events.UserInvited;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SpringUserInvitationEventPublisher implements UserInvitationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringUserInvitationEventPublisher(
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.applicationEventPublisher = Objects.requireNonNull(
            applicationEventPublisher,
            "applicationEventPublisher must not be null"
        );
    }

    @Override
    public void publish(UserInvited event) {
        applicationEventPublisher.publishEvent(event);
    }
}
