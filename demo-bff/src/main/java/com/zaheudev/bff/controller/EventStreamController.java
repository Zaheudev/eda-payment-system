package com.zaheudev.bff.controller;

import com.zaheudev.bff.model.EventEnvelope;
import com.zaheudev.bff.service.EventStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@CrossOrigin(origins = "*")
public class EventStreamController {

    private final EventStreamService eventStream;

    public EventStreamController(EventStreamService eventStream) {
        this.eventStream = eventStream;
    }

    @GetMapping(value = "/api/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<EventEnvelope>> stream() {
        Flux<ServerSentEvent<EventEnvelope>> events = eventStream.stream()
                .map(envelope -> ServerSentEvent.<EventEnvelope>builder()
                        .data(envelope)
                        .build());

        Flux<ServerSentEvent<EventEnvelope>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(tick -> ServerSentEvent.<EventEnvelope>builder()
                        .comment("keepalive")
                        .build());

        return Flux.merge(events, heartbeat);
    }
}
