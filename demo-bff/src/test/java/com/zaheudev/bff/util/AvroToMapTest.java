package com.zaheudev.bff.util;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AvroToMapTest {

    @Test
    void convertShouldReturnNullForNull() {
        assertThat(AvroToMap.convert(null)).isNull();
    }

    @Test
    void convertShouldHandleGenericRecord() {
        Schema schema = Schema.createRecord("Test", null, null, false);
        schema.setFields(List.of(
                new Schema.Field("name", Schema.create(Schema.Type.STRING), null, "test"),
                new Schema.Field("value", Schema.create(Schema.Type.INT), null, 42)
        ));
        GenericRecord record = new GenericData.Record(schema);
        record.put("name", "hello");
        record.put("value", 123);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) AvroToMap.convert(record);

        assertThat(result).containsEntry("name", "hello");
        assertThat(result).containsEntry("value", 123);
    }

    @Test
    void convertShouldHandleCollection() {
        List<Integer> input = List.of(1, 2, 3);
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) AvroToMap.convert(input);
        assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    void convertShouldHandleMap() {
        Map<String, String> input = Map.of("key", "value");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) AvroToMap.convert(input);
        assertThat(result).containsEntry("key", "value");
    }

    @Test
    void convertShouldHandleCharSequence() {
        assertThat(AvroToMap.convert(new StringBuilder("test")).toString()).isEqualTo("test");
    }

    @Test
    void convertShouldHandleByteBuffer() {
        byte[] bytes = "hello".getBytes();
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        String expected = Base64.getEncoder().encodeToString(bytes);
        assertThat(AvroToMap.convert(bb)).isEqualTo(expected);
    }

    @Test
    void convertShouldReturnPassThroughForUnknownType() {
        assertThat(AvroToMap.convert(42)).isEqualTo(42);
        assertThat(AvroToMap.convert(Boolean.TRUE)).isEqualTo(Boolean.TRUE);
    }
}
