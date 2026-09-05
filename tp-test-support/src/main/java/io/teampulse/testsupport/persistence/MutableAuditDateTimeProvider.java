package io.teampulse.testsupport.persistence;

import org.jspecify.annotations.NonNull;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.Optional;

public final class MutableAuditDateTimeProvider implements DateTimeProvider {

    private Instant currentInstant = Instant.EPOCH;

    public void setCurrentInstant(Instant currentInstant) {
        this.currentInstant = Objects.requireNonNull(currentInstant);
    }

    @Override
    public @NonNull Optional<TemporalAccessor> getNow() {
        return Optional.of(currentInstant);
    }
}
