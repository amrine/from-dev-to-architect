package io.teampulse.common.context;

import java.util.Objects;

/**
 * Identifies the actor used for audit and orchestration without authenticating
 * or authorizing that actor.
 *
 * @param actorReference stable reference persisted in audit fields
 */
public record ActorContext(String actorReference) {

    public static final String SYSTEM_ACTOR_REFERENCE = "SYSTEM";

    public ActorContext {
        Objects.requireNonNull(actorReference, "actorReference must not be null");
        if (actorReference.isBlank()) {
            throw new IllegalArgumentException("actorReference must not be blank");
        }
    }

    /** Returns the technical system actor used before authenticated identity exists. */
    public static ActorContext system() {
        return new ActorContext(SYSTEM_ACTOR_REFERENCE);
    }
}
