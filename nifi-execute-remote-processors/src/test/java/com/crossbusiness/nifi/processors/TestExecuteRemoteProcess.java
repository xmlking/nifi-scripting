package com.crossbusiness.nifi.processors;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.stream.io.ByteArrayInputStream;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertTrue;



public class TestExecuteRemoteProcess {

    private TestRunner testRunner;
    private InputStream content;

    @Before
    public void init() {
        // Generate a test runner to mock a processor in a flow
        testRunner = TestRunners.newTestRunner(ExecuteRemoteProcess.class);
    }

    @Test
    public void testThing() throws IOException {

        String configDsl = IOUtils.toString(
                this.getClass().getResourceAsStream("/configDsl.groovy"),
                "UTF-8"
        );
        String runDsl = IOUtils.toString(
                this.getClass().getResourceAsStream("/runDsl.groovy"),
                "UTF-8"
        );

        // Add properites
        testRunner.setProperty(ExecuteRemoteProcess.CONFIG_DSL, configDsl);
        testRunner.setProperty(ExecuteRemoteProcess.RUN_DSL, runDsl);
        testRunner.setProperty(ExecuteRemoteProcess.RUN_DSL_ARGS, "5;2");

        // Run the enqueued content, it also takes an int = number of contents queued
        testRunner.run(1);

        // If you need to read or do aditional tests on results you can access the content
        List<MockFlowFile> results = testRunner.getFlowFilesForRelationship(ExecuteRemoteProcess.REL_SUCCESS);

        // All results were processed with out failure
        testRunner.assertQueueEmpty();

        assertTrue("1 match", results.size() == 1);
        MockFlowFile result = results.get(0);

        // Test attributes and content
        result.assertContentEquals("Darwin");
    }
}