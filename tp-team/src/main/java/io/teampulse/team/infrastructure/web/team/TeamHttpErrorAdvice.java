package io.teampulse.team.infrastructure.web.team;

import io.teampulse.common.error.ApiError;
import io.teampulse.team.domain.team.error.TeamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TeamHttpErrorAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamHttpErrorAdvice.class);

    @ExceptionHandler(TeamException.class)
    public ResponseEntity<ApiError> handleTeamException(TeamException exception) {
        return switch (exception.getErrorCode()) {
            case NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "TEAM_NOT_FOUND",
                "Team was not found"
            );
            case INVALID_NAME -> error(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "INVALID_TEAM_NAME",
                "Team name is invalid"
            );
            case ORGANIZATION_NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "TEAM_ORGANIZATION_NOT_FOUND",
                "Team organization was not found"
            );
            case ORGANIZATION_UNAVAILABLE -> error(
                HttpStatus.CONFLICT,
                "TEAM_ORGANIZATION_UNAVAILABLE",
                "Team organization is not available"
            );
            case ORGANIZATION_DIRECTORY_UNAVAILABLE -> error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "TEAM_ORGANIZATION_DIRECTORY_UNAVAILABLE",
                "Team organization could not be validated"
            );
            case ADMINISTRATOR_NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "TEAM_ADMINISTRATOR_NOT_FOUND",
                "Team administrator was not found"
            );
            case ADMINISTRATOR_NOT_AVAILABLE -> error(
                HttpStatus.CONFLICT,
                "TEAM_ADMINISTRATOR_NOT_AVAILABLE",
                "Team administrator is not available"
            );
            case MANAGER_NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "TEAM_MANAGER_NOT_FOUND",
                "Team manager was not found"
            );
            case MANAGER_NOT_AVAILABLE -> error(
                HttpStatus.CONFLICT,
                "TEAM_MANAGER_NOT_AVAILABLE",
                "Team manager is not available"
            );
            case USER_DIRECTORY_UNAVAILABLE -> error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "TEAM_USER_DIRECTORY_UNAVAILABLE",
                "Team user could not be validated"
            );
            case INVALID_STATUS_TRANSITION -> error(
                HttpStatus.CONFLICT,
                "TEAM_TRANSITION_NOT_ALLOWED",
                "Team transition is not allowed"
            );
            case TEAM_UNAVAILABLE -> error(
                HttpStatus.CONFLICT,
                "TEAM_UNAVAILABLE",
                "Team is not available for this operation"
            );
            case MEMBER_NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "TEAM_MEMBER_NOT_FOUND",
                "Team member was not found"
            );
            case MEMBER_ALREADY_EXISTS -> error(
                HttpStatus.CONFLICT,
                "TEAM_MEMBER_ALREADY_EXISTS",
                "Team member already exists"
            );
            case MEMBER_USER_NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "TEAM_MEMBER_USER_NOT_FOUND",
                "Team member user was not found"
            );
            case MEMBER_USER_NOT_AVAILABLE -> error(
                HttpStatus.CONFLICT,
                "TEAM_MEMBER_USER_NOT_AVAILABLE",
                "Team member user is not available"
            );
            case INVALID_MEMBER_STATUS_TRANSITION -> error(
                HttpStatus.CONFLICT,
                "TEAM_MEMBER_TRANSITION_NOT_ALLOWED",
                "Team member transition is not allowed"
            );
            case REFERENCE_GENERATION_FAILED -> {
                LOGGER.error("Unable to generate a team reference", exception);
                yield error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "TEAM_REFERENCE_UNAVAILABLE",
                    "Team could not be created"
                );
            }
            case CONCURRENT_MODIFICATION -> error(
                HttpStatus.CONFLICT,
                "TEAM_CONCURRENT_MODIFICATION",
                "Team was modified concurrently"
            );
        };
    }

    private static ResponseEntity<ApiError> error(
        HttpStatus status,
        String code,
        String message
    ) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
