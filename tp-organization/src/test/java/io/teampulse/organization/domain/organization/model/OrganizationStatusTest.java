package io.teampulse.organization.domain.organization.model;

import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.teampulse.organization.domain.organization.model.OrganizationStatus.ACTIVE;
import static io.teampulse.organization.domain.organization.model.OrganizationStatus.ARCHIVED;
import static io.teampulse.organization.domain.organization.model.OrganizationStatus.CREATING;
import static io.teampulse.organization.domain.organization.model.OrganizationStatus.SUSPENDED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationStatusTest {

    private static final Set<Transition> ALLOWED_TRANSITIONS = Set.of(
        new Transition(CREATING, ACTIVE),
        new Transition(CREATING, ARCHIVED),
        new Transition(ACTIVE, SUSPENDED),
        new Transition(ACTIVE, ARCHIVED),
        new Transition(SUSPENDED, ACTIVE),
        new Transition(SUSPENDED, ARCHIVED)
    );

    @Test
    void enforcesCompleteTransitionMatrix() {
        for (OrganizationStatus source : OrganizationStatus.values()) {
            for (OrganizationStatus target : OrganizationStatus.values()) {
                Transition transition = new Transition(source, target);

                if (ALLOWED_TRANSITIONS.contains(transition)) {
                    assertDoesNotThrow(
                        () -> source.validateTransitionTo(target),
                        transition.toString()
                    );
                } else {
                    OrganizationException exception = assertThrows(
                        OrganizationException.class,
                        () -> source.validateTransitionTo(target),
                        transition.toString()
                    );
                    assertEquals(
                        OrganizationErrorCode.INVALID_STATUS_TRANSITION,
                        exception.getErrorCode()
                    );
                }
            }
        }
    }

    @Test
    void rejectsNullTargetFromEveryStatus() {
        for (OrganizationStatus source : OrganizationStatus.values()) {
            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> source.validateTransitionTo(null),
                source.toString()
            );

            assertEquals(
                OrganizationErrorCode.INVALID_STATUS_TRANSITION,
                exception.getErrorCode()
            );
        }
    }

    private record Transition(OrganizationStatus source, OrganizationStatus target) {
    }
}
