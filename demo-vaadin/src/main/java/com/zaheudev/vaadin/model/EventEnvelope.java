package com.zaheudev.vaadin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope {
    private String topic;
    private String key;
    private long timestamp;
    private Object payload;
    private String paymentId;
    private int partition;
    private long offset;
}
