package io.teampulse.organization.infrastructure.web.organization;

import io.teampulse.common.error.ApiError;
import io.teampulse.organization.domain.organization.error.OrganizationException;
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
public class OrganizationHttpErrorAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        OrganizationHttpErrorAdvice.class
    );

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
                LOGGER.error("Unable to generate an organization reference", exception);
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

    private static ResponseEntity<ApiError> error(
        HttpStatus status,
        String code,
        String message
    ) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
