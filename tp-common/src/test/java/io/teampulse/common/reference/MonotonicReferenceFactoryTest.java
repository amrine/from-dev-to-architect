package io.teampulse.common.reference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MonotonicReferenceFactoryTest {

    private static final Instant BASE_INSTANT =
        Instant.parse("2026-08-09T00:00:00.035Z");

    private static final int TEST_NONCE =
        Integer.parseInt("A7B9", 36);

    private static final Pattern REFERENCE_PATTERN = Pattern.compile(
        "[A-Z]{3}-[0-9]{4}-[0-9]{4}-[0-9A-Z]{12}"
    );

    @Nested
    class ConstructionTests {

        @Test
        void rejectsNullClock() {
            assertThrows(NullPointerException.class, () -> new MonotonicReferenceFactory(null));
        }

        @Test
        void rejectsNullNonceSource() {
            assertThrows(NullPointerException.class, () -> new MonotonicReferenceFactory(fixedClock(), null));
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 1_679_616})
        void rejectsNonceOutsideSupportedRange(int nonce) {
            assertThrows(IllegalArgumentException.class, () -> new MonotonicReferenceFactory(fixedClock(), () -> nonce));
        }

        @Test
        void readsNonceSourceExactlyOnce() {
            AtomicInteger calls = new AtomicInteger();
            MonotonicReferenceFactory factory =
                new MonotonicReferenceFactory(
                    fixedClock(),
                    () -> {
                        calls.incrementAndGet();
                        return TEST_NONCE;
                    }
                );

            assertEquals(1, calls.get());

            factory.generate("ORG");
            factory.generate("USR");
            factory.generate("TEM");

            assertEquals(1, calls.get());
        }

        @ParameterizedTest
        @CsvSource({
            "0, 0000",
            "1679615, ZZZZ"
        })
        void acceptsNonceBoundaryValues(int nonce, String expectedEncoding) {
            MonotonicReferenceFactory factory =
                new MonotonicReferenceFactory(fixedClock(), () -> nonce);

            assertEquals(expectedEncoding, nonceOf(factory.generate("ORG")));
        }

        @Test
        void publicConstructorGeneratesWellFormedReference() {
            MonotonicReferenceFactory factory =
                new MonotonicReferenceFactory(fixedClock());

            String reference = factory.generate("ORG");

            assertEquals(26, reference.length());
            assertTrue(REFERENCE_PATTERN.matcher(reference).matches());
        }
    }

    @Nested
    class PrefixValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            "OR",
            "ORGA",
            "org",
            "O1G",
            " ORG",
            "ORG ",
            "ÉRG"
        })
        void rejectsInvalidPrefix(String prefix) {
            MonotonicReferenceFactory factory = factory(fixedClock());

            assertThrows(IllegalArgumentException.class, () -> factory.generate(prefix));
        }

        @Test
        void invalidPrefixDoesNotReadClockOrConsumeState() {
            MutableClock clock = new MutableClock(BASE_INSTANT);
            MonotonicReferenceFactory factory = factory(clock);

            assertThrows(IllegalArgumentException.class, () -> factory.generate("org"));

            assertEquals(0, clock.millisReadCount());

            String firstValidReference = factory.generate("ORG");

            assertEquals(1, clock.millisReadCount());
            assertEquals("00", counterOf(firstValidReference));
        }
    }

    @Nested
    class FormattingTests {

        @Test
        void generatesExpectedReferenceFormat() {
            MonotonicReferenceFactory factory = factory(fixedClock());

            String reference = factory.generate("ORG");

            assertEquals("ORG-2026-0908-00000ZA7B900", reference);
            assertEquals(26, reference.length());
            assertTrue(REFERENCE_PATTERN.matcher(reference).matches());
        }

        @Test
        void alwaysDerivesDateAndTimeFromUtc() {
            Clock clockWithDifferentZone = Clock.fixed(
                Instant.parse("2026-08-09T23:00:00Z"),
                ZoneId.of("Pacific/Kiritimati")
            );
            MonotonicReferenceFactory factory = factory(clockWithDifferentZone);

            String reference = factory.generate("ORG");

            assertTrue(reference.startsWith("ORG-2026-0908-"));
        }

        @Test
        void keepsBootNonceStableForFactoryLifetime() {
            MonotonicReferenceFactory factory = factory(fixedClock());

            for (int index = 0; index < 100; index++) {
                assertEquals("A7B9", nonceOf(factory.generate("ORG")));
            }
        }

        @Test
        void factoriesWithDifferentNoncesGenerateDifferentReferences() {
            MonotonicReferenceFactory firstFactory = new MonotonicReferenceFactory(fixedClock(), () -> TEST_NONCE);
            MonotonicReferenceFactory secondFactory =
                new MonotonicReferenceFactory(fixedClock(), () -> Integer.parseInt("B8C0", 36));

            String firstReference = firstFactory.generate("ORG");
            String secondReference = secondFactory.generate("ORG");

            assertNotEquals(firstReference, secondReference);
            assertEquals("A7B9", nonceOf(firstReference));
            assertEquals("B8C0", nonceOf(secondReference));
        }
    }

    @Nested
    class LogicalStateTests {

        @Test
        void startsCounterAtZero() {
            MonotonicReferenceFactory factory = factory(fixedClock());

            assertEquals("00", counterOf(factory.generate("ORG")));
        }

        @Test
        void incrementsCounterWithinSameMillisecond() {
            MonotonicReferenceFactory factory = factory(fixedClock());

            assertEquals("00", counterOf(factory.generate("ORG")));
            assertEquals("01", counterOf(factory.generate("ORG")));
            assertEquals("02", counterOf(factory.generate("ORG")));
        }

        @Test
        void sharesLogicalStateAcrossPrefixes() {
            MonotonicReferenceFactory factory = factory(fixedClock());

            assertEquals("00", counterOf(factory.generate("ORG")));
            assertEquals("01", counterOf(factory.generate("USR")));
            assertEquals("02", counterOf(factory.generate("TEM")));
        }

        @Test
        void usesZzForTheOneThousandTwoHundredNinetySixthReference() {
            MonotonicReferenceFactory factory = factory(fixedClock());
            String reference = null;

            for (int index = 0; index < 1_296; index++) {
                reference = factory.generate("ORG");
            }

            assertEquals("ZZ", counterOf(reference));
        }

        @Test
        void advancesLogicalTimeAfterCounterOverflow() {
            MonotonicReferenceFactory factory = factory(fixedClock());

            for (int index = 0; index < 1_296; index++) {
                factory.generate("ORG");
            }

            String reference = factory.generate("ORG");

            assertEquals("000010", timeOfDayOf(reference));
            assertEquals("00", counterOf(reference));
        }

        @Test
        void resetsCounterWhenClockMovesAheadOfLogicalTime() {
            MutableClock clock = new MutableClock(BASE_INSTANT);
            MonotonicReferenceFactory factory = factory(clock);

            factory.generate("ORG");
            assertEquals("01", counterOf(factory.generate("ORG")));

            clock.setInstant(BASE_INSTANT.plusMillis(1));
            String reference = factory.generate("ORG");

            assertEquals("000010", timeOfDayOf(reference));
            assertEquals("00", counterOf(reference));
        }

        @Test
        void doesNotResetCounterWhenClockOnlyCatchesUpWithLogicalTime() {
            MutableClock clock = new MutableClock(BASE_INSTANT);
            MonotonicReferenceFactory factory = factory(clock);

            for (int index = 0; index < 1_297; index++) {
                factory.generate("ORG");
            }

            clock.setInstant(BASE_INSTANT.plusMillis(1));
            String reference = factory.generate("ORG");

            assertEquals("000010", timeOfDayOf(reference));
            assertEquals("01", counterOf(reference));
        }

        @Test
        void keepsLogicalTimeWhenClockMovesBackward() {
            MutableClock clock = new MutableClock(BASE_INSTANT);
            MonotonicReferenceFactory factory = factory(clock);

            String firstReference = factory.generate("ORG");
            clock.setInstant(BASE_INSTANT.minusMillis(1_000));
            String secondReference = factory.generate("ORG");

            assertEquals(
                firstReference.substring(0, 24),
                secondReference.substring(0, 24)
            );
            assertEquals("01", counterOf(secondReference));
        }

        @Test
        void keepsSegmentsConsistentAcrossDayAndYearChange() {
            MutableClock clock = new MutableClock(
                Instant.parse("2026-12-31T23:59:59.999Z")
            );
            MonotonicReferenceFactory factory = factory(clock);

            String lastReferenceOfYear = factory.generate("ORG");

            clock.setInstant(Instant.parse("2027-01-01T00:00:00Z"));
            String firstReferenceOfNextYear = factory.generate("ORG");

            assertTrue(lastReferenceOfYear.startsWith("ORG-2026-3112-"));
            assertEquals(
                "ORG-2027-0101-000000A7B900",
                firstReferenceOfNextYear
            );
        }

        @Test
        void carriesLogicalOverflowAcrossDayAndYearChange() {
            Clock clock = Clock.fixed(
                Instant.parse("2026-12-31T23:59:59.999Z"),
                ZoneOffset.UTC
            );
            MonotonicReferenceFactory factory = factory(clock);

            for (int index = 0; index < 1_296; index++) {
                factory.generate("ORG");
            }

            String firstReferenceOfNextYear = factory.generate("ORG");

            assertEquals(
                "ORG-2027-0101-000000A7B900",
                firstReferenceOfNextYear
            );
        }
    }

    @Nested
    class ConcurrencyTests {

        @Test
        void generatesUniqueReferencesConcurrently() throws Exception {
            int threadCount = 12;
            int referencesPerThread = 300;
            int expectedReferenceCount =
                threadCount * referencesPerThread;

            MonotonicReferenceFactory factory = factory(fixedClock());
            Set<String> references = ConcurrentHashMap.newKeySet();
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);
            List<Future<Void>> futures = new ArrayList<>();

            try {
                for (int thread = 0; thread < threadCount; thread++) {
                    futures.add(executor.submit(() -> {
                        start.await();

                        for (int index = 0;
                             index < referencesPerThread;
                             index++) {
                            String reference = factory.generate("ORG");

                            if (!references.add(reference)) {
                                fail("Duplicate reference: " + reference);
                            }
                        }

                        return null;
                    }));
                }

                start.countDown();

                for (Future<Void> future : futures) {
                    future.get(10, TimeUnit.SECONDS);
                }
            } finally {
                executor.shutdownNow();
            }

            assertEquals(expectedReferenceCount, references.size());
        }
    }

    private static MonotonicReferenceFactory factory(Clock clock) {
        return new MonotonicReferenceFactory(clock, () -> TEST_NONCE);
    }

    private static Clock fixedClock() {
        return Clock.fixed(BASE_INSTANT, ZoneOffset.UTC);
    }

    private static String timeOfDayOf(String reference) {
        return reference.substring(14, 20);
    }

    private static String nonceOf(String reference) {
        return reference.substring(20, 24);
    }

    private static String counterOf(String reference) {
        return reference.substring(24, 26);
    }

    private static final class MutableClock extends Clock {

        private final AtomicLong epochMillis;
        private final AtomicInteger millisReadCount;
        private final ZoneId zone;

        private MutableClock(Instant initialInstant) {
            this(
                new AtomicLong(initialInstant.toEpochMilli()),
                new AtomicInteger(),
                ZoneOffset.UTC
            );
        }

        private MutableClock(
            AtomicLong epochMillis,
            AtomicInteger millisReadCount,
            ZoneId zone
        ) {
            this.epochMillis = epochMillis;
            this.millisReadCount = millisReadCount;
            this.zone = zone;
        }

        private void setInstant(Instant instant) {
            epochMillis.set(instant.toEpochMilli());
        }

        private int millisReadCount() {
            return millisReadCount.get();
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            if (zone.equals(newZone)) {
                return this;
            }

            return new MutableClock(
                epochMillis,
                millisReadCount,
                newZone
            );
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(epochMillis.get());
        }

        @Override
        public long millis() {
            millisReadCount.incrementAndGet();
            return epochMillis.get();
        }
    }
}
