package com.campusgigs.pattern.observer;

/**
 * Observer side of the pattern: reacts to one kind of {@link AppEvent}.
 *
 * @param <E> the event type this listener handles
 */
@FunctionalInterface
public interface EventListener<E extends AppEvent> {

    /**
     * Called by the {@link EventBus} for every published event of type {@code E}.
     * Runs on the publisher's thread, so keep it short and never touch Swing
     * components directly from here.
     *
     * @param event the published event, never {@code null}
     */
    void onEvent(E event);
}
