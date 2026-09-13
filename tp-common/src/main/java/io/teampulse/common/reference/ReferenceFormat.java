package io.teampulse.common.reference;

import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates the shared syntactic format of TeamPulse functional references.
 *
 * <p>Business modules remain responsible for choosing the expected prefix and
 * for translating an invalid reference into their own error contract.</p>
 */
@UtilityClass
public final class ReferenceFormat {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("[A-Z]{3}");

    private static final Pattern REFERENCE_PATTERN = Pattern.compile(
        "([A-Z]{3})-[0-9]{4}-[0-9]{4}-[0-9A-Z]{12}"
    );

    /**
     * Checks whether a reference has the shared format and expected prefix.
     *
     * <p>This is a syntactic check only. It does not interpret the date
     * segments or attach business meaning to a prefix.</p>
     *
     * @param reference reference to check
     * @param expectedPrefix prefix selected by the consuming business module
     * @return {@code true} when both the format and prefix match
     */
    public static boolean matches(String reference, String expectedPrefix) {
        if (reference == null || isNotValidPrefix(expectedPrefix)) {
            return false;
        }

        Matcher matcher = REFERENCE_PATTERN.matcher(reference);
        return matcher.matches() && expectedPrefix.equals(matcher.group(1));
    }

    /**
     * Checks whether a prefix is valid.
     *
     * @param prefix prefix to check
     * @return {@code true} when the prefix is not valid
     */
    public static boolean isNotValidPrefix(String prefix) {
        return prefix == null || !PREFIX_PATTERN.matcher(prefix).matches();
    }
}
