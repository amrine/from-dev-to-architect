package io.teampulse.websupport.error;

import io.teampulse.common.error.ApiError;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Order()
public class SharedHttpErrorAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(
        ConstraintViolationException exception
    ) {
        return ResponseEntity.badRequest().body(new ApiError(
            "VALIDATION_FAILED",
            "Request validation failed"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedFailure(Exception exception) {
        logger.error("Unhandled HTTP request failure", exception);
        return ResponseEntity.internalServerError().body(new ApiError(
            "INTERNAL_ERROR",
            "An unexpected error occurred"
        ));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        return response(
            exception,
            headers,
            status,
            new ApiError("VALIDATION_FAILED", "Request validation failed"),
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
        HandlerMethodValidationException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        return response(
            exception,
            headers,
            status,
            validationError(exception.isForReturnValue(), status),
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodValidationException(
        MethodValidationException exception,
        HttpHeaders headers,
        HttpStatus status,
        WebRequest request
    ) {
        return response(
            exception,
            headers,
            status,
            validationError(exception.isForReturnValue(), status),
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        return response(
            exception,
            headers,
            status,
            new ApiError("INVALID_REQUEST", "Request body is invalid"),
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception exception,
        Object body,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        HttpStatusCode responseStatus = status == null
            ? HttpStatus.INTERNAL_SERVER_ERROR
            : status;
        if (responseStatus.is5xxServerError()) {
            logger.error(
                "HTTP request failed with status " + responseStatus.value(),
                exception
            );
        }

        return super.handleExceptionInternal(
            exception,
            errorFor(responseStatus),
            headers,
            responseStatus,
            request
        );
    }

    private ResponseEntity<Object> response(
        Exception exception,
        HttpHeaders headers,
        HttpStatusCode status,
        ApiError apiError,
        WebRequest request
    ) {
        return super.handleExceptionInternal(
            exception,
            apiError,
            headers,
            status,
            request
        );
    }

    private ApiError errorFor(HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return new ApiError("INTERNAL_ERROR", "An unexpected error occurred");
        }

        return switch (status.value()) {
            case 400 -> new ApiError("INVALID_REQUEST", "Request is invalid");
            case 404 -> new ApiError(
                "ROUTE_NOT_FOUND",
                "The requested resource was not found"
            );
            case 405 -> new ApiError(
                "METHOD_NOT_ALLOWED",
                "The HTTP method is not supported for this resource"
            );
            case 406 -> new ApiError(
                "NOT_ACCEPTABLE",
                "The requested response format is not supported"
            );
            case 413 -> new ApiError(
                "REQUEST_TOO_LARGE",
                "The request exceeds the configured size limit"
            );
            case 415 -> new ApiError(
                "UNSUPPORTED_MEDIA_TYPE",
                "The request content type is not supported"
            );
            default -> new ApiError(
                "REQUEST_REJECTED",
                "The request could not be processed"
            );
        };
    }

    private ApiError validationError(boolean returnValue, HttpStatusCode status) {
        if (!returnValue && status.is4xxClientError()) {
            return new ApiError("VALIDATION_FAILED", "Request validation failed");
        }

        return errorFor(status);
    }
}
