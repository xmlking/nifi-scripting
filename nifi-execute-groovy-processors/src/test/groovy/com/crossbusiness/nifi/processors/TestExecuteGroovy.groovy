package com.crossbusiness.nifi.processors

import org.apache.nifi.util.MockFlowFile
import org.apache.nifi.util.TestRunner
import org.apache.nifi.util.TestRunners
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue

public class TestExecuteGroovy {

    private TestRunner testRunner;
    private String content;

    @Before
    public void init() {
        // Content to be mock a json file
        content = "{\"hello\":\"nifi rocks\"}"

        // Generate a test runner to mock a processor in a flow
        testRunner = TestRunners.newTestRunner(ExecuteGroovy.class);
    }

    @Test
    public void testThing() throws IOException {

        String groovyString = this.getClass().getResource( '/TestScript.groovy' ).text

        // Add properites
        testRunner.setProperty(ExecuteGroovy.GROOVY_SCRIPT, groovyString);
        testRunner.setProperty(ExecuteGroovy.GROOVY_ARGS, "5;2");

        // Add the content to the runner
        testRunner.enqueue(content, [sumo:'dem']);
//        testRunner.setIncomingConnection(false)

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