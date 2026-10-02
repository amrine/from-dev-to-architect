package io.teampulse.identity.application.port.out.user;

import io.teampulse.identity.events.UserInvited;

/** Publishes the reference-only fact that a user was invited. */
public interface UserInvitationEventPublisher {

    void publish(UserInvited event);
}
