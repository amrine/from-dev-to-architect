package io.teampulse.common.persistence;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstraintNameExtractorTest {

    @Test
    void extractsAValueFromTheFirstMatchingCause() {
        IllegalArgumentException matchingCause =
            new IllegalArgumentException("organization_reference");
        RuntimeException exception = new RuntimeException(
            "wrapper",
            new IllegalStateException("intermediate", matchingCause)
        );

        Optional<String> value = ConstraintNameExtractor.extract(
            exception,
            IllegalArgumentException.class,
            IllegalArgumentException::getMessage
        );

        assertEquals(Optional.of("organization_reference"), value);
    }

    @Test
    void continuesWhenAValueExtractorReturnsNull() {
        IllegalArgumentException firstCause =
            new IllegalArgumentException("first");
        IllegalArgumentException matchingCause =
            new IllegalArgumentException("second", firstCause);

        Optional<String> value = ConstraintNameExtractor.extract(
            matchingCause,
            IllegalArgumentException.class,
            exception -> exception == matchingCause
                ? null
                : exception.getMessage()
        );

        assertEquals(Optional.of("first"), value);
    }

    @Test
    void returnsEmptyWhenNoMatchingCauseExists() {
        Optional<String> value = ConstraintNameExtractor.extract(
            new IllegalStateException("failure"),
            IllegalArgumentException.class,
            IllegalArgumentException::getMessage
        );

        assertEquals(Optional.empty(), value);
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(
            NullPointerException.class,
            () -> ConstraintNameExtractor.extract(
                null,
                IllegalArgumentException.class,
                IllegalArgumentException::getMessage
            )
        );
    }
}
