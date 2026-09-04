package io.teampulse.common.reference;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;

/**
 * Thread-safe factory generating monotonic functional references within a
 * single factory instance.
 *
 * <p>References follow the fixed format
 * {@code PREFIX-YEAR-DDMM-MMMMMMNNNNCC}. A compare-and-set loop combines a
 * logical UTC timestamp with a per-millisecond counter. A four-character nonce
 * remains stable for the lifetime of the factory and reduces collision risk
 * after a restart.</p>
 *
 * <p>This implementation guarantees uniqueness only among references emitted
 * by the same factory instance. Uniqueness across multiple application
 * instances is not deterministically guaranteed.</p>
 */
// TODO: Introduce a stable node identity or shared coordination before
// running multiple application instances.
public final class MonotonicReferenceFactory implements ReferenceFactory {

    private static final int RADIX = 36;
    private static final int TIME_WIDTH = 6;
    private static final int NONCE_WIDTH = 4;
    private static final int COUNTER_WIDTH = 2;

    private static final int NONCE_BOUND = 1_679_616; // 36^4
    private static final int MAX_COUNTER = 1_295;     // 36^2 - 1
    private static final int INITIAL_COUNTER = 0;

    private static final int REFERENCE_LENGTH = 26;

    private static final Pattern PREFIX_PATTERN = Pattern.compile("[A-Z]{3}");
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("uuuu", Locale.ROOT);
    private static final DateTimeFormatter DAY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("ddMM", Locale.ROOT);

    private final Clock clock;
    private final String bootNonce;

    private final AtomicReference<GenerationState> state = new AtomicReference<>();

    /**
     * Creates a reference factory backed by the supplied clock and by a
     * cryptographically strong nonce generated once for this factory instance.
     *
     * @param clock clock used to observe the current instant
     * @throws NullPointerException if {@code clock} is {@code null}
     */
    public MonotonicReferenceFactory(Clock clock) {
        this(clock, secureNonceSource());
    }

    /**
     * Creates a reference factory with controllable time and nonce sources.
     * This constructor has package visibility so tests can produce deterministic
     * references without exposing nonce selection as part of the public API.
     * The nonce source is evaluated exactly once during construction.
     *
     * @param clock clock used to observe the current instant
     * @param nonceSource source of a value in the inclusive range
     *                    {@code 0} to {@code 36^4 - 1}
     * @throws NullPointerException if {@code clock} or {@code nonceSource} is
     *                              {@code null}
     * @throws IllegalArgumentException if the supplied nonce does not fit in
     *                                  four base-36 characters
     */
    MonotonicReferenceFactory(Clock clock, IntSupplier nonceSource) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        IntSupplier checkedNonceSource = Objects.requireNonNull(
            nonceSource,
            "nonceSource must not be null"
        );
        int nonceValue = checkedNonceSource.getAsInt();
        if (nonceValue < 0 || nonceValue >= NONCE_BOUND) {
            throw new IllegalArgumentException(
                "nonce must be between 0 and " + (NONCE_BOUND - 1)
            );
        }
        this.bootNonce = encodeBase36(nonceValue, NONCE_WIDTH);
    }

    /**
     * Creates a secure nonce source dedicated to one factory construction.
     *
     * @return a source producing values that fit in four base-36 characters
     */
    private static IntSupplier secureNonceSource() {
        SecureRandom secureRandom = new SecureRandom();
        return () -> secureRandom.nextInt(NONCE_BOUND);
    }

    /**
     * Generates the next reference for the given family prefix.
     *
     * <p>The returned value follows the format
     * {@code PREFIX-YEAR-DDMM-MMMMMMNNNNCC}, where the final twelve characters
     * contain the logical milliseconds of the UTC day, the factory nonce and
     * the per-millisecond counter. Concurrent calls on this factory instance do
     * not return duplicate references.</p>
     *
     * @param prefix exactly three uppercase ASCII letters identifying the
     *               reference family
     * @return the next 26-character reference
     * @throws IllegalArgumentException if {@code prefix} does not match
     *                                  {@code [A-Z]{3}}
     */
    @Override
    public String generate(String prefix) {
        validatePrefix(prefix);
        long observedMillis = clock.millis();
        GenerationState next = reserveNextState(observedMillis);
        return formatReference(prefix, next);
    }

    /**
     * Verifies the public prefix contract without normalizing the input.
     *
     * @param prefix prefix to validate
     * @throws IllegalArgumentException if the prefix is {@code null} or does
     *                                  not contain exactly three uppercase
     *                                  ASCII letters
     */
    private static void validatePrefix(String prefix) {
        if (prefix == null || !PREFIX_PATTERN.matcher(prefix).matches()) {
            throw new IllegalArgumentException(
                "prefix must contain exactly three uppercase ASCII letters"
            );
        }
    }

    /**
     * Atomically reserves the next logical timestamp and counter pair.
     *
     * <p>The observed time replaces the logical time only when it moves
     * forward. Equal or backward clock readings retain the last logical time
     * and increment its counter. Once the counter reaches {@code ZZ}, the
     * logical time advances by one millisecond and the counter restarts at
     * {@code 00}. A compare-and-set loop coordinates concurrent callers without
     * using an explicit lock.</p>
     *
     * @param observedMillis epoch milliseconds read once from the injected
     *                       clock for the current generation request
     * @return the state exclusively reserved for the generated reference
     */
    private GenerationState reserveNextState(long observedMillis) {
        while (true) {
            GenerationState current = state.get();
            GenerationState next = nextState(current, observedMillis);

            if (state.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    /**
     * Computes the state following the last successfully reserved state.
     *
     * @param current last reserved state, or {@code null} before the first
     *                generation
     * @param observedMillis epoch milliseconds observed for the current request
     * @return the candidate state for the compare-and-set operation
     */
    private static GenerationState nextState(GenerationState current, long observedMillis) {
        if (current == null || observedMillis > current.logicalEpochMillis()) {
            return new GenerationState(observedMillis, INITIAL_COUNTER);
        }

        if (current.counter() < MAX_COUNTER) {
            return new GenerationState(current.logicalEpochMillis(), current.counter() + 1);
        }

        return new GenerationState(Math.incrementExact(current.logicalEpochMillis()), INITIAL_COUNTER);
    }

    /**
     * Encodes a non-negative value in uppercase base 36 and pads it on the left
     * with zeroes to the requested fixed width.
     *
     * @param value non-negative value to encode
     * @param width required number of output characters
     * @return the fixed-width uppercase base-36 representation
     * @throws IllegalArgumentException if {@code value} is negative, if
     *                                  {@code width} is not positive or if the
     *                                  encoded value exceeds the requested width
     */
    private static String encodeBase36(long value, int width) {
        if (value < 0) {
            throw new IllegalArgumentException("value must not be negative");
        }

        if (width <= 0) {
            throw new IllegalArgumentException("width must be greater than zero");
        }

        String encoded = Long.toString(value, RADIX).toUpperCase(Locale.ROOT);

        if (encoded.length() > width) {
            throw new IllegalArgumentException(
                "value does not fit in " + width + " base-36 characters"
            );
        }

        return "0".repeat(width - encoded.length()) + encoded;
    }

    /**
     * Formats a reserved generation state as the public reference value.
     * Every date and time segment is derived from the same accepted logical
     * instant in UTC, keeping the reference coherent across clock rollbacks and
     * day or year boundaries.
     *
     * @param prefix validated three-letter reference prefix
     * @param reservedState state previously reserved for this reference
     * @return the assembled 26-character reference
     * @throws IllegalStateException if the assembled reference does not have
     *                               the expected fixed length
     */
    private String formatReference(String prefix, GenerationState reservedState) {
        ZonedDateTime utcDateTime = Instant
            .ofEpochMilli(reservedState.logicalEpochMillis())
            .atZone(UTC);

        String year = YEAR_FORMATTER.format(utcDateTime);
        String dayMonth = DAY_MONTH_FORMATTER.format(utcDateTime);
        long millisOfDay = utcDateTime.toLocalTime().toNanoOfDay() / 1_000_000L;
        String encodedTime = encodeBase36(millisOfDay, TIME_WIDTH);
        String encodedCounter = encodeBase36(
            reservedState.counter(),
            COUNTER_WIDTH
        );

        String reference =
            prefix + "-" + year + "-" + dayMonth + "-" + encodedTime + bootNonce + encodedCounter;

        if (reference.length() != REFERENCE_LENGTH) {
            throw new IllegalStateException(
                "generated reference must contain exactly "
                    + REFERENCE_LENGTH
                    + " characters"
            );
        }

        return reference;
    }

}
