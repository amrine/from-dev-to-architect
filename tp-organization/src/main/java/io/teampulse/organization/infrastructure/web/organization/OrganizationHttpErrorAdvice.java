package io.teampulse.organization.infrastructure.web.organization;

import io.teampulse.common.error.ApiError;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleException;
import io.teampulse.organization.domain.organization.error.OrganizationException;
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
public class OrganizationHttpErrorAdvice {

    /**
     * Maps a public lifecycle failure to the module's HTTP status and safe error payload.
     * The underlying diagnostic cause is never serialized into the response.
     *
     * @param exception failure raised by the public lifecycle contract
     * @return the corresponding HTTP error response
     */
    @ExceptionHandler(OrganizationLifecycleException.class)
    public ResponseEntity<ApiError> handleOrganizationLifecycleException(OrganizationLifecycleException exception) {
        return switch (exception.getErrorCode()) {
            case ORGANIZATION_NOT_FOUND -> error(HttpStatus.NOT_FOUND,
                "ORGANIZATION_NOT_FOUND", exception.getMessage());
            case INVALID_NAME -> error(HttpStatus.UNPROCESSABLE_CONTENT,
                "INVALID_ORGANIZATION_NAME", exception.getMessage());
            case INVALID_TIMEZONE -> error(HttpStatus.UNPROCESSABLE_CONTENT,
                "INVALID_ORGANIZATION_TIMEZONE", exception.getMessage());
            case TRANSITION_NOT_ALLOWED -> error(HttpStatus.CONFLICT,
                "ORGANIZATION_TRANSITION_NOT_ALLOWED", exception.getMessage());
            case CONCURRENT_MODIFICATION -> error(HttpStatus.CONFLICT,
                "ORGANIZATION_CONCURRENT_MODIFICATION", exception.getMessage());
            case REFERENCE_GENERATION_FAILED -> error(HttpStatus.INTERNAL_SERVER_ERROR,
                "ORGANIZATION_REFERENCE_UNAVAILABLE", exception.getMessage());
            case OPERATION_FAILED -> error(HttpStatus.INTERNAL_SERVER_ERROR,
                "ORGANIZATION_OPERATION_FAILED", exception.getMessage());
        };
    }

    /**
     * Maps internal organization business failures, including responsibility refusals,
     * to safe HTTP errors. Reference-generation failures are logged for diagnosis.
     *
     * @param exception domain failure raised by an internal use case
     * @return the module-owned HTTP status, error code and safe message
     */
    @ExceptionHandler(OrganizationException.class)
    public ResponseEntity<ApiError> handleOrganizationException(
        OrganizationException exception
    ) {
        return switch (exception.getErrorCode()) {
            case NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "ORGANIZATION_NOT_FOUND",
                "Organization was not found"
            );
            case INVALID_NAME -> error(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "INVALID_ORGANIZATION_NAME",
                "Organization name is invalid"
            );
            case INVALID_TIMEZONE -> error(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "INVALID_ORGANIZATION_TIMEZONE",
                "Organization timezone is invalid"
            );
            case INVALID_STATUS_TRANSITION -> error(
                HttpStatus.CONFLICT,
                "ORGANIZATION_TRANSITION_NOT_ALLOWED",
                "Organization transition is not allowed"
            );
            case ACTIVATION_REQUIREMENTS_NOT_MET -> error(
                HttpStatus.CONFLICT,
                "ORGANIZATION_ACTIVATION_REQUIREMENTS_NOT_MET",
                "Organization activation requirements are not met"
            );
            case LIFECYCLE_TRANSITION_DEFERRED -> error(
                HttpStatus.CONFLICT,
                "ORGANIZATION_LIFECYCLE_DEFERRED",
                "Completing organization responsibilities is deferred"
            );
            case ADMINISTRATOR_NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "ORGANIZATION_ADMINISTRATOR_NOT_FOUND",
                "Organization administrator was not found"
            );
            case ADMINISTRATOR_NOT_AVAILABLE -> error(
                HttpStatus.CONFLICT,
                "ORGANIZATION_ADMINISTRATOR_NOT_AVAILABLE",
                "Organization administrator is not available"
            );
            case MANAGER_NOT_FOUND -> error(
                HttpStatus.NOT_FOUND,
                "ORGANIZATION_MANAGER_NOT_FOUND",
                "Organization manager was not found"
            );
            case MANAGER_NOT_AVAILABLE -> error(
                HttpStatus.CONFLICT,
                "ORGANIZATION_MANAGER_NOT_AVAILABLE",
                "Organization manager is not available"
            );
            case USER_DIRECTORY_UNAVAILABLE -> error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ORGANIZATION_USER_DIRECTORY_UNAVAILABLE",
                "Organization responsibilities could not be validated"
            );
            case REFERENCE_GENERATION_FAILED -> {
                log.error("Unable to generate an organization reference", exception);
                yield error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ORGANIZATION_REFERENCE_UNAVAILABLE",
                    "Organization could not be created"
                );
            }
            case CONCURRENT_MODIFICATION -> error(
                HttpStatus.CONFLICT,
                "ORGANIZATION_CONCURRENT_MODIFICATION",
                "Organization was modified concurrently"
            );
        };
    }

    /**
     * Builds a response containing only the public error code and message.
     *
     * @param status HTTP status chosen by the local mapping
     * @param code stable public HTTP error code
     * @param message safe message without technical details
     * @return the JSON error response
     */
    private static ResponseEntity<ApiError> error(
        HttpStatus status,
        String code,
        String message
    ) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
