package com.crossbusiness.nifi.processors;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.stream.io.BufferedOutputStream;
import org.apache.nifi.stream.io.StreamUtils;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class NiFiUtils {
    public String flowFileToString(FlowFile flowFile, ProcessSession session) {
        final byte[] buffer = new byte[(int) flowFile.getSize()];
        session.read(flowFile, in -> StreamUtils.fillBuffer(in, buffer, false));
        return new String(buffer, StandardCharsets.UTF_8);
    }

    public FlowFile stringToFlowFile(String flowString, ProcessSession session) {
        return stringToFlowFile(flowString, session, null);
    }

    public FlowFile stringToFlowFile(String flowString, ProcessSession session, FlowFile flowFile) {
        FlowFile ff = (flowFile == null) ? session.create() : session.create(flowFile);
        return session.write(ff, out -> {
            try (OutputStream outputStream = new BufferedOutputStream(out)) {
                outputStream.write(flowString.getBytes(StandardCharsets.UTF_8));
            }
        });
    }
}
