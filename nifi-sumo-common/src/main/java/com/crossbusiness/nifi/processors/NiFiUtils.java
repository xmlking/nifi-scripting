package com.crossbusiness.nifi.processors;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
//import org.apache.nifi.processor.io.OutputStreamCallback;
//import org.apache.nifi.stream.io.BufferedOutputStream;
import org.apache.nifi.stream.io.StreamUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public  class NiFiUtils {

    public static String flowFileToString(FlowFile flowFile, ProcessSession session) {
        final String[] flowString = new String[1];
        session.read(flowFile, in ->  flowString[0] = IOUtils.toString(in, StandardCharsets.UTF_8));
        return flowString[0];
    }

    public static FlowFile stringToFlowFile(String flowString, ProcessSession session) {
        return stringToFlowFile(flowString, session, null);
    }

    public static FlowFile  exceptionToFlowFile(final Throwable throwable, ProcessSession session) {
        return exceptionToFlowFile(throwable, session, null);
    }

    public static FlowFile  exceptionToFlowFile(final Throwable throwable, ProcessSession session, FlowFile flowFile) {
        return stringToFlowFile(getStackTrace(throwable),  session,  flowFile);
    }

    public static FlowFile stringToFlowFile(String flowString, ProcessSession session, FlowFile flowFile) {
        FlowFile ff = (flowFile == null) ? session.create() : session.create(flowFile);
        return session.write(ff, out -> out.write(flowString.getBytes(StandardCharsets.UTF_8)));
    }


    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
