package com.crossbusiness.nifi.processors

import org.apache.nifi.util.MockFlowFile
import org.apache.nifi.util.TestRunner
import org.apache.nifi.util.TestRunners
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue

public class TestExecuteGroovyLdap {

    private TestRunner testRunner;
    private String content;

    @Before
    public void init() {
        // Content to be mock a json file
        content = "{\"hello\":\"nifi rocks\"}"

        // Generate a test runner to mock a processor in a flow
        testRunner = TestRunners.newTestRunner(ExecuteGroovyLdap.class);
    }

    @Test
    public void testThing() throws IOException {

        String groovyString = this.getClass().getResource( '/TestScript.groovy' ).text
//        String groovyString = this.getClass().getResource( '/TestLdapConfigFlow.groovy' ).text
//        String groovyString = this.getClass().getResource( '/TestLdapSchemaFlow.groovy' ).text

        // Add properites
        testRunner.setProperty(ExecuteGroovyLdap.LDAP_URI, "ldap://zanzibar:10389");
        testRunner.setProperty(ExecuteGroovyLdap.LDAP_USERNAME, "uid=admin,ou=system");
        testRunner.setProperty(ExecuteGroovyLdap.LDAP_PASSWORD, "******");
        testRunner.setProperty(ExecuteGroovyLdap.GROOVY_SCRIPT, groovyString);
        testRunner.setProperty(ExecuteGroovyLdap.GROOVY_ARGS, "5;2");

        // Add the content to the runner
        testRunner.enqueue(content, [host:'localhost',port:'389']);
//        testRunner.setIncomingConnection(false)

        // Run the enqueued content, it also takes an int = number of contents queued
        testRunner.run(1);

        // If you need to read or do aditional tests on results you can access the content
        List<MockFlowFile> results = testRunner.getFlowFilesForRelationship(ExecuteGroovyLdap.REL_SUCCESS);

        // All results were processed with out failure
        testRunner.assertQueueEmpty();

        println results.size()
        assertTrue("1 match", results.size() == 1);
        MockFlowFile result = results.get(0);

        // Test attributes and content
        result.assertAttributeEquals("MY_ARG_0",  "5");
        result.assertContentEquals("{\"hello\":\"nifi rocks\"}");
    }

}