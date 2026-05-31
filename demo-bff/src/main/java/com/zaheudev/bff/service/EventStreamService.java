package com.zaheudev.bff.service;

import com.zaheudev.bff.model.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
public class EventStreamService {

    private final Sinks.Many<EventEnvelope> sink = Sinks.many().replay().limit(256);

    public void publish(EventEnvelope envelope) {
        Sinks.EmitResult result = sink.tryEmitNext(envelope);
        if (result.isFailure() && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            log.warn("Failed to emit event to stream: {}", result);
        }
    }

    public Flux<EventEnvelope> stream() {
        return sink.asFlux();
    }
}
