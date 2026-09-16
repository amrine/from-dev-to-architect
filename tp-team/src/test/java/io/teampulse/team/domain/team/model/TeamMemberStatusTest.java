package io.teampulse.team.domain.team.model;

import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.teampulse.team.domain.team.model.TeamMemberStatus.ACTIVE;
import static io.teampulse.team.domain.team.model.TeamMemberStatus.INVITED;
import static io.teampulse.team.domain.team.model.TeamMemberStatus.REMOVED;
import static io.teampulse.team.domain.team.model.TeamMemberStatus.SUSPENDED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamMemberStatusTest {

    private static final Set<Transition> ALLOWED_TRANSITIONS = Set.of(
            new Transition(INVITED, ACTIVE),
            new Transition(INVITED, REMOVED),
            new Transition(ACTIVE, SUSPENDED),
            new Transition(ACTIVE, REMOVED),
            new Transition(SUSPENDED, ACTIVE),
            new Transition(SUSPENDED, REMOVED));

    @Test
    void enforcesCompleteTransitionMatrix() {
        for (TeamMemberStatus source : TeamMemberStatus.values()) {
            for (TeamMemberStatus target : TeamMemberStatus.values()) {
                Transition transition = new Transition(source, target);

                if (ALLOWED_TRANSITIONS.contains(transition)) {
                    assertDoesNotThrow(() -> source.validateTransitionTo(target), transition.toString());
                } else {
                    TeamException exception = assertThrows(
                            TeamException.class, () -> source.validateTransitionTo(target), transition.toString());
                    assertEquals(TeamErrorCode.INVALID_MEMBER_STATUS_TRANSITION, exception.getErrorCode());
                }
            }
        }
    }

    @Test
    void rejectsNullTargetFromEveryStatus() {
        for (TeamMemberStatus source : TeamMemberStatus.values()) {
            TeamException exception =
                    assertThrows(TeamException.class, () -> source.validateTransitionTo(null), source.toString());

            assertEquals(TeamErrorCode.INVALID_MEMBER_STATUS_TRANSITION, exception.getErrorCode());
        }
    }

    private record Transition(TeamMemberStatus source, TeamMemberStatus target) {}
}
