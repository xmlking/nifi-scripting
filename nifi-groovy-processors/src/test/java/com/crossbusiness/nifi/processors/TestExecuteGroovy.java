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



public class TestExecuteGroovy {

    private TestRunner testRunner;
    private InputStream content;

    @Before
    public void init() {
        // Content to be mock a json file
        content = new ByteArrayInputStream("{\"hello\":\"nifi rocks\"}".getBytes());

        // Generate a test runner to mock a processor in a flow
        testRunner = TestRunners.newTestRunner(ExecuteGroovy.class);
    }

    @Test
    public void testThing() throws IOException {

        String groovyString = IOUtils.toString(
                this.getClass().getResourceAsStream("/TestScript.groovy"),
                "UTF-8"
        );

        // Add properites
        testRunner.setProperty(ExecuteGroovy.GROOVY_SCRIPT, groovyString);
        testRunner.setProperty(ExecuteGroovy.GROOVY_ARGS, "5;2");

        // Add the content to the runner
        testRunner.enqueue(content);

        // Run the enqueued content, it also takes an int = number of contents queued
        testRunner.run(1);

        // If you need to read or do aditional tests on results you can access the content
        List<MockFlowFile> results = testRunner.getFlowFilesForRelationship(ExecuteGroovy.REL_SUCCESS);

        // All results were processed with out failure
        testRunner.assertQueueEmpty();

        assertTrue("1 match", results.size() == 1);
        MockFlowFile result = results.get(0);

        // Test attributes and content
        result.assertAttributeEquals("MY_ARG_0",  "5");
        result.assertContentEquals("{\"hello\":\"nifi rocks\"}");
    }
}