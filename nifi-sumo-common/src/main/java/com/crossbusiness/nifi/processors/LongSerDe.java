package com.crossbusiness.nifi.processors;

import org.apache.nifi.distributed.cache.client.Deserializer;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.DeserializationException;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LongSerDe implements Serializer<Long>, Deserializer<Long> {

    @Override
    public Long deserialize(final byte[] input) throws DeserializationException, IOException {
        if (input == null || input.length == 0) {
            return null;
        }

        final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(input));
        return dis.readLong();
    }

    @Override
    public void serialize(final Long value, final OutputStream out) throws SerializationException, IOException {
        final DataOutputStream dos = new DataOutputStream(out);
        dos.writeLong(value);
    }
}
