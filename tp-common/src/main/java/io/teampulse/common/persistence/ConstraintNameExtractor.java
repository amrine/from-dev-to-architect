package io.teampulse.common.persistence;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Extracts a value from the first matching exception in a cause chain.
 *
 * <p>The exception type and extraction function are supplied by the caller so
 * this shared utility remains independent from persistence frameworks.</p>
 */
public final class ConstraintNameExtractor {

    private ConstraintNameExtractor() {
    }

    /**
     * Searches the exception and its causes for a matching exception type.
     *
     * @param exception root exception to inspect
     * @param exceptionType exception type carrying the value to extract
     * @param valueExtractor function extracting the value from the matching exception
     * @param <T> matching exception type
     * @return the first non-null extracted value, or empty when none exists
     */
    public static <T extends Throwable> Optional<String> extract(
        Throwable exception,
        Class<T> exceptionType,
        Function<T, String> valueExtractor
    ) {
        Objects.requireNonNull(exception, "exception must not be null");
        Objects.requireNonNull(exceptionType, "exceptionType must not be null");
        Objects.requireNonNull(valueExtractor, "valueExtractor must not be null");

        Throwable current = exception;
        while (current != null) {
            if (exceptionType.isInstance(current)) {
                String value = valueExtractor.apply(exceptionType.cast(current));
                if (value != null) {
                    return Optional.of(value);
                }
            }
            current = current.getCause();
        }

        return Optional.empty();
    }
}
