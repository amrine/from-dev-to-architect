package io.teampulse.team.domain.team.model;

import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.teampulse.team.domain.team.model.TeamStatus.ACTIVE;
import static io.teampulse.team.domain.team.model.TeamStatus.ARCHIVED;
import static io.teampulse.team.domain.team.model.TeamStatus.SUSPENDED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamStatusTest {

    private static final Set<Transition> ALLOWED_TRANSITIONS = Set.of(
            new Transition(ACTIVE, SUSPENDED),
            new Transition(ACTIVE, ARCHIVED),
            new Transition(SUSPENDED, ACTIVE),
            new Transition(SUSPENDED, ARCHIVED));

    @Test
    void enforcesCompleteTransitionMatrix() {
        for (TeamStatus source : TeamStatus.values()) {
            for (TeamStatus target : TeamStatus.values()) {
                Transition transition = new Transition(source, target);

                if (ALLOWED_TRANSITIONS.contains(transition)) {
                    assertDoesNotThrow(() -> source.validateTransitionTo(target), transition.toString());
                } else {
                    TeamException exception = assertThrows(
                            TeamException.class, () -> source.validateTransitionTo(target), transition.toString());
                    assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
                }
            }
        }
    }

    @Test
    void rejectsNullTargetFromEveryStatus() {
        for (TeamStatus source : TeamStatus.values()) {
            TeamException exception =
                    assertThrows(TeamException.class, () -> source.validateTransitionTo(null), source.toString());

            assertEquals(TeamErrorCode.INVALID_STATUS_TRANSITION, exception.getErrorCode());
        }
    }

    private record Transition(TeamStatus source, TeamStatus target) {}
}
