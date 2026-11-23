package io.teampulse.common.context;

/** Resolves the actor context associated with the current execution. */
@FunctionalInterface
public interface ActorContextProvider {

    /** @return the current actor context, never {@code null} */
    ActorContext current();
}
