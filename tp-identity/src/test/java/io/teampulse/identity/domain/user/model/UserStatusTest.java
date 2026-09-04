package io.teampulse.identity.domain.user.model;

import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.teampulse.identity.domain.user.model.UserStatus.ACTIVE;
import static io.teampulse.identity.domain.user.model.UserStatus.CREATING;
import static io.teampulse.identity.domain.user.model.UserStatus.DEACTIVATED;
import static io.teampulse.identity.domain.user.model.UserStatus.INVITED;
import static io.teampulse.identity.domain.user.model.UserStatus.SUSPENDED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserStatusTest {

    private static final Set<Transition> ALLOWED_TRANSITIONS = Set.of(
        new Transition(INVITED, CREATING),
        new Transition(INVITED, DEACTIVATED),
        new Transition(CREATING, ACTIVE),
        new Transition(CREATING, DEACTIVATED),
        new Transition(ACTIVE, SUSPENDED),
        new Transition(ACTIVE, DEACTIVATED),
        new Transition(SUSPENDED, ACTIVE),
        new Transition(SUSPENDED, DEACTIVATED)
    );

    @Test
    void enforcesCompleteTransitionMatrix() {
        for (UserStatus source : UserStatus.values()) {
            for (UserStatus target : UserStatus.values()) {
                Transition transition = new Transition(source, target);

                if (ALLOWED_TRANSITIONS.contains(transition)) {
                    assertDoesNotThrow(
                        () -> source.validateTransitionTo(target),
                        transition.toString()
                    );
                } else {
                    UserException exception = assertThrows(
                        UserException.class,
                        () -> source.validateTransitionTo(target),
                        transition.toString()
                    );
                    assertEquals(
                        UserErrorCode.INVALID_STATUS_TRANSITION,
                        exception.getErrorCode()
                    );
                }
            }
        }
    }

    @Test
    void rejectsNullTarget() {
        UserException exception = assertThrows(
            UserException.class,
            () -> INVITED.validateTransitionTo(null)
        );

        assertEquals(
            UserErrorCode.INVALID_STATUS_TRANSITION,
            exception.getErrorCode()
        );
    }

    private record Transition(UserStatus source, UserStatus target) {
    }
}
