package com.zaheudev.vaadin.util;

import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.Schema;

import java.nio.ByteBuffer;
import java.util.*;

public final class AvroToMap {

    private AvroToMap() {}

    public static Object convert(Object value) {
        if (value == null) return null;
        if (value instanceof GenericRecord gr) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Schema.Field field : gr.getSchema().getFields()) {
                map.put(field.name(), convert(gr.get(field.name())));
            }
            return map;
        }
        if (value instanceof Collection<?> c) {
            return c.stream().map(AvroToMap::convert).toList();
        }
        if (value instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), convert(v)));
            return out;
        }
        if (value instanceof GenericEnumSymbol<?> e) {
            return e.toString();
        }
        if (value instanceof CharSequence cs) {
            return cs.toString();
        }
        if (value instanceof ByteBuffer bb) {
            byte[] bytes = new byte[bb.remaining()];
            bb.duplicate().get(bytes);
            return Base64.getEncoder().encodeToString(bytes);
        }
        return value;
    }
}
