package io.teampulse.common.reference;

/**
 * Generates stable functional references from a three-letter family prefix.
 */
@FunctionalInterface
public interface ReferenceFactory {

    /**
     * Generates the next reference for the supplied family prefix.
     *
     * @param prefix exactly three uppercase ASCII letters
     * @return the generated functional reference
     * @throws IllegalArgumentException if {@code prefix} does not satisfy the
     *                                  implementation contract
     */
    String generate(String prefix);
}
