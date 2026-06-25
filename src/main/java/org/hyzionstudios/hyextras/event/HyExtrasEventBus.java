package org.hyzionstudios.hyextras.event;

import org.hyzionstudios.hyextras.HyExtrasPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Lightweight synchronous typed event bus for HyExtras state-change notifications.
 *
 * <p>Listeners are invoked on the thread that posts the event (matching the existing synchronous
 * dispatcher model). Each listener is isolated with try/catch so a single failing listener cannot
 * break dispatch or the calling subsystem. Subscription/dispatch are lock-free via
 * {@link ConcurrentHashMap} + {@link CopyOnWriteArrayList}.
 */
public final class HyExtrasEventBus {

    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    /**
     * Registers a listener for events of the given type.
     *
     * @return a handle whose {@link AutoCloseable#close()} removes the listener
     */
    public <E> AutoCloseable subscribe(Class<E> type, Consumer<E> listener) {
        if (type == null || listener == null) {
            return () -> { };
        }
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> unsubscribe(type, listener);
    }

    public <E> void unsubscribe(Class<E> type, Consumer<E> listener) {
        CopyOnWriteArrayList<Consumer<?>> list = listeners.get(type);
        if (list != null) {
            list.remove(listener);
        }
    }

    /** Posts an event to all listeners registered for its exact runtime type. */
    @SuppressWarnings("unchecked")
    public <E> void post(E event) {
        if (event == null) {
            return;
        }
        CopyOnWriteArrayList<Consumer<?>> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Consumer<?> raw : list) {
            try {
                ((Consumer<E>) raw).accept(event);
            } catch (Exception e) {
                HyExtrasPlugin plugin = HyExtrasPlugin.get();
                if (plugin != null && plugin.getLogger() != null) {
                    plugin.getLogger().at(Level.WARNING).withCause(e)
                            .log("[hextras events] listener for "
                                    + event.getClass().getSimpleName() + " threw; continuing");
                }
            }
        }
    }

    public void clear() {
        listeners.clear();
    }
}
