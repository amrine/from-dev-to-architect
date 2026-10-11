package io.teampulse.identity.infrastructure.messaging;

import io.teampulse.identity.application.port.out.user.IdentityEventPublisher;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class IdentityEventPublisherAdapter implements IdentityEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public <E> void publish(E event) {
        applicationEventPublisher.publishEvent(event);
    }
}
