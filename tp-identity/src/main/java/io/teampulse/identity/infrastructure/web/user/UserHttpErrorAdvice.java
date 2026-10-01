package io.teampulse.identity.infrastructure.web.user;

import io.teampulse.common.error.ApiError;
import io.teampulse.identity.api.lifecycle.UserLifecycleErrorCode;
import io.teampulse.identity.api.lifecycle.UserLifecycleException;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserHttpErrorAdvice {

    @ExceptionHandler(UserLifecycleException.class)
    public ResponseEntity<ApiError> handleUserLifecycleException(
        UserLifecycleException exception
    ) {
        if (
            exception.getErrorCode() == UserLifecycleErrorCode.INVALID_USER_DATA
                && exception.getCause() instanceof UserException userException
        ) {
            return switch (userException.getErrorCode()) {
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
                default -> error(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "USER_DATA_INVALID",
                    "User data is invalid"
                );
            };
        }

        if (
            exception.getErrorCode() == UserLifecycleErrorCode.OPERATION_FAILED
                && exception.getCause() instanceof UserException userException
                && userException.getErrorCode() == UserErrorCode.REFERENCE_GENERATION_FAILED
        ) {
            return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "USER_REFERENCE_UNAVAILABLE",
                "User could not be created"
            );
        }

        return switch (exception.getErrorCode()) {
            case USER_NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                "User was not found"
            );
            case INVALID_USER_DATA -> error(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "USER_DATA_INVALID",
                "User data is invalid"
            );
            case EMAIL_ALREADY_USED -> error(
                HttpStatus.CONFLICT,
                "USER_EMAIL_ALREADY_USED",
                "Email is already used in this organization"
            );
            case TRANSITION_NOT_ALLOWED -> error(
                HttpStatus.CONFLICT,
                "USER_TRANSITION_NOT_ALLOWED",
                "User transition is not allowed"
            );
            case CONCURRENT_MODIFICATION -> error(
                HttpStatus.CONFLICT,
                "USER_CONCURRENT_MODIFICATION",
                "User was modified concurrently"
            );
            case OPERATION_FAILED -> error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "USER_OPERATION_FAILED",
                "User lifecycle operation failed"
            );
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
