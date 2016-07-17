package com.crossbusiness.nifi.processors;

import org.apache.nifi.distributed.cache.client.Deserializer;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.DeserializationException;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;
import org.apache.nifi.stream.io.ByteArrayInputStream;
import org.apache.nifi.stream.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class ObjectSerDe implements Serializer<Object>, Deserializer<Object> {

    @Override
    public Object deserialize(byte[] input) throws DeserializationException, IOException {
        if (input == null || input.length == 0) {
            return null;
        }

        try (final ByteArrayInputStream in = new ByteArrayInputStream(input);
             final ObjectInputStream objIn = new ObjectInputStream(in)) {
            return objIn.readObject();
        } catch (ClassNotFoundException e) {
            throw new DeserializationException("Could not deserialize object due to ClassNotFoundException", e);
        }
    }

    @Override
    public void serialize(Object value, OutputStream output) throws SerializationException, IOException {
        try (final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
             final ObjectOutputStream objOut = new ObjectOutputStream(bOut)) {
            objOut.writeObject(value);
            output.write(bOut.toByteArray());
        }
    }

}