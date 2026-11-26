package io.teampulse.administration.infrastructure.web;

import io.teampulse.administration.application.error.PlatformAdministrationException;
import io.teampulse.common.error.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PlatformAdministrationHttpErrorAdvice {

    @ExceptionHandler(PlatformAdministrationException.class)
    public ResponseEntity<ApiError> handlePlatformAdministrationException(
        PlatformAdministrationException exception
    ) {
        return switch (exception.getErrorCode()) {
            case ORGANIZATION_NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "ORGANIZATION_NOT_FOUND",
                "Organization was not found"
            );
            case ORGANIZATION_LOOKUP_UNAVAILABLE -> error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ORGANIZATION_LOOKUP_UNAVAILABLE",
                "Organization could not be checked"
            );
            case USER_DATA_INVALID -> error(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "INVALID_USER_DATA",
                "User data is invalid"
            );
            case USER_EMAIL_ALREADY_USED -> error(
                HttpStatus.CONFLICT,
                "USER_EMAIL_ALREADY_USED",
                "Email is already used in this organization"
            );
            case USER_NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                "User was not found"
            );
            case USER_TRANSITION_NOT_ALLOWED -> error(
                HttpStatus.CONFLICT,
                "USER_TRANSITION_NOT_ALLOWED",
                "User transition is not allowed"
            );
            case USER_HAS_ACTIVE_RESPONSIBILITIES -> error(
                HttpStatus.CONFLICT,
                "USER_HAS_ACTIVE_RESPONSIBILITIES",
                "User has active responsibilities"
            );
            case RESPONSIBILITY_CHECK_UNAVAILABLE -> error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "RESPONSIBILITY_CHECK_UNAVAILABLE",
                "User responsibilities could not be checked"
            );
            case USER_LIFECYCLE_FAILED -> internalError(
                exception,
                "Platform user lifecycle operation failed",
                "USER_LIFECYCLE_FAILED",
                "User lifecycle operation could not be completed"
            );
        };
    }

    private static ResponseEntity<ApiError> internalError(
        PlatformAdministrationException exception,
        String logMessage,
        String code,
        String publicMessage
    ) {
        log.error(logMessage, exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, code, publicMessage);
    }

    private static ResponseEntity<ApiError> error(
        HttpStatus status,
        String code,
        String message
    ) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
