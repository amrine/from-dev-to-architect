package io.teampulse.common.reference;

/**
 * Immutable state reserved atomically for one generated reference.
 *
 * @param logicalEpochMillis accepted logical instant expressed in epoch
 *                           milliseconds
 * @param counter sequence value associated with the logical instant
 */
record GenerationState(long logicalEpochMillis, int counter) {}
