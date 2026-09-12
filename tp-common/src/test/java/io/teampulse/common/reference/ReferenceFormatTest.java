package io.teampulse.common.reference;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceFormatTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-0908-00000ZA7B900";

    @ParameterizedTest
    @CsvSource({
        "ORG, ORG-2026-0908-00000ZA7B900",
        "USR, USR-2026-0908-00000ZA7B900",
        "TEM, TEM-2026-0908-00000ZA7B900",
        "ABC, ABC-2026-0908-00000ZA7B900"
    })
    void acceptsSharedFormatForAnyValidExpectedPrefix(
        String expectedPrefix,
        String reference
    ) {
        assertTrue(ReferenceFormat.matches(reference, expectedPrefix));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "ORG-2026-0908-00000ZA7B90",
        "ORG-2026-0908-00000ZA7B9000",
        "org-2026-0908-00000ZA7B900",
        "ORG-26-0908-00000ZA7B900",
        "ORG-2026-908-00000ZA7B900",
        "ORG-2026-0908-00000Za7B900",
        "ORG/2026/0908/00000ZA7B900",
        " ORG-2026-0908-00000ZA7B900",
        "ORG-2026-0908-00000ZA7B900 "
    })
    void rejectsMalformedReference(String reference) {
        assertFalse(ReferenceFormat.matches(reference, "ORG"));
    }

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
    void rejectsInvalidExpectedPrefix(String expectedPrefix) {
        assertFalse(
            ReferenceFormat.matches(ORGANIZATION_REFERENCE, expectedPrefix)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"USR", "TEM", "ABC"})
    void rejectsReferenceFromAnotherPrefix(String expectedPrefix) {
        assertFalse(
            ReferenceFormat.matches(ORGANIZATION_REFERENCE, expectedPrefix)
        );
    }
}
