package com.zaheudev.vaadin.service;

import com.vaadin.flow.component.UI;
import com.zaheudev.vaadin.model.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
@Service
public class EventBroadcaster {

    private final Map<UI, Consumer<EventEnvelope>> listeners = new ConcurrentHashMap<>();
    private final List<EventEnvelope> buffer = new CopyOnWriteArrayList<>();
    private static final int BUFFER_SIZE = 256;

    public void publish(EventEnvelope envelope) {
        buffer.add(envelope);
        if (buffer.size() > BUFFER_SIZE) {
            buffer.remove(0);
        }

        for (var entry : Map.copyOf(listeners).entrySet()) {
            UI ui = entry.getKey();
            Consumer<EventEnvelope> callback = entry.getValue();
            try {
                ui.access(() -> callback.accept(envelope));
            } catch (Exception e) {
                log.debug("Failed to push event to UI: {}", e.getMessage());
            }
        }
    }

    public List<EventEnvelope> getRecentEvents() {
        return List.copyOf(buffer);
    }

    public void subscribe(UI ui, Consumer<EventEnvelope> callback) {
        listeners.put(ui, callback);
    }

    public void unsubscribe(UI ui) {
        listeners.remove(ui);
    }

    public Set<UI> getSubscribedUIs() {
        return Set.copyOf(listeners.keySet());
    }
}
