package com.crossbusiness.nifi.processors;

import org.apache.nifi.distributed.cache.client.Deserializer;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.DeserializationException;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StringSerDe implements Serializer<String>, Deserializer<String> {

    @Override
    public String deserialize(final byte[] value) throws DeserializationException, IOException {
        if (value == null) {
            return null;
        }

        return new String(value, StandardCharsets.UTF_8);
    }

    @Override
    public void serialize(final String value, final OutputStream out) throws SerializationException, IOException {
        out.write(value.getBytes(StandardCharsets.UTF_8));
    }

}
