package io.teampulse.identity.infrastructure.web.user;

import io.teampulse.common.error.ApiError;
import io.teampulse.identity.domain.user.error.UserException;
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
public class UserHttpErrorAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        UserHttpErrorAdvice.class
    );

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiError> handleUserException(UserException exception) {
        return switch (exception.getErrorCode()) {
            case NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                "User was not found"
            );
            case INVALID_EMAIL -> error(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "INVALID_USER_EMAIL",
                "Email is invalid"
            );
            case INVALID_FIRST_NAME -> error(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "INVALID_USER_FIRST_NAME",
                "First name is invalid"
            );
            case INVALID_LAST_NAME -> error(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "INVALID_USER_LAST_NAME",
                "Last name is invalid"
            );
            case EMAIL_ALREADY_USED -> error(
                HttpStatus.CONFLICT,
                "USER_EMAIL_ALREADY_USED",
                "Email is already used in this organization"
            );
            case INVALID_STATUS_TRANSITION -> error(
                HttpStatus.CONFLICT,
                "USER_TRANSITION_NOT_ALLOWED",
                "User transition is not allowed"
            );
            case CONCURRENT_MODIFICATION -> error(
                HttpStatus.CONFLICT,
                "USER_CONCURRENT_MODIFICATION",
                "User was modified concurrently"
            );
            case REFERENCE_GENERATION_FAILED -> {
                LOGGER.error("Unable to generate a user reference", exception);
                yield error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "USER_REFERENCE_UNAVAILABLE",
                    "User could not be created"
                );
            }
        };
    }

    private ResponseEntity<ApiError> error(
        HttpStatus status,
        String code,
        String message
    ) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
