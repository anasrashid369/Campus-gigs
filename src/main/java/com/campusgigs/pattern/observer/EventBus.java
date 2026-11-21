package com.campusgigs.pattern.observer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Subject side of the Observer pattern: keeps the listeners for each event type
 * and delivers every published event to the listeners of that exact type.
 * <p>
 * Services use the shared instance, {@code EventBus.getInstance().publish(...)},
 * and call it only after their database transaction has committed. Tests create
 * their own bus with the public constructor so they do not share listeners.
 * <p>
 * Safe to use from several threads, because services run inside SwingWorkers.
 * A listener that throws is logged and skipped: it never stops the other
 * listeners and never reaches the publisher.
 */
public class EventBus {

    private static final Logger LOG = Logger.getLogger(EventBus.class.getName());
    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<? extends AppEvent>, CopyOnWriteArrayList<EventListener<? extends AppEvent>>> listeners =
            new ConcurrentHashMap<>();

    /** Creates a separate bus. The application uses {@link #getInstance()}; tests use this. */
    public EventBus() {
    }

    /** The shared bus used by the running application. */
    public static EventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a listener for one event type. Subscribing the same listener
     * object twice to the same type has no extra effect.
     *
     * @param type     the event record class, for example {@code ApplicationSubmitted.class}
     * @param listener called for every published event of that type
     */
    public <E extends AppEvent> void subscribe(Class<E> type, EventListener<E> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");
        listeners.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).addIfAbsent(listener);
    }

    /**
     * Delivers the event to every listener subscribed to its type.
     * Does nothing when the type has no listeners.
     *
     * @param event the event to deliver, must not be {@code null}
     */
    public void publish(AppEvent event) {
        Objects.requireNonNull(event, "event");
        List<EventListener<? extends AppEvent>> forType = listeners.get(event.getClass());
        if (forType == null) {
            return;
        }
        for (EventListener<? extends AppEvent> listener : forType) {
            deliver(listener, event);
        }
    }

    @SuppressWarnings("unchecked")
    private static void deliver(EventListener<? extends AppEvent> listener, AppEvent event) {
        try {
            ((EventListener<AppEvent>) listener).onEvent(event);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Event listener failed while handling " + event, e);
        }
    }
}
