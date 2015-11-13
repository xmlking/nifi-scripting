package com.crossbusiness.nifi.processors;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestExecuteGroovy {
    @Test
    public void testThing() throws Exception {
        int numOfFlowFiles = 100;

        //File f = new File("test.out");
        final TestRunner controller = TestRunners.newTestRunner(ExecuteGroovy.class);
        controller.setThreadCount(10);
        controller.setProperty(ExecuteGroovy.GROOVY_SCRIPT, "/path/to/script/file/TestScript.groovy");
        controller.setProperty(ExecuteGroovy.GROOVY_ARGS, "5");

        Map<String, String> attributes = new HashMap<>(1);
        for (int i = 0; i < numOfFlowFiles; i++) {
            attributes.put("FF", Integer.toString(i));
            controller.enqueue("SOME DATA".getBytes(), attributes);
        }
        controller.run(numOfFlowFiles);
        controller.assertValid();
        controller.assertAllFlowFilesTransferred("success", numOfFlowFiles);

        for (MockFlowFile flowFile : controller.getFlowFilesForRelationship("success")) {
            assertEquals(flowFile.getAttribute("FF"), flowFile.getAttribute("GROOVY"));
        }
    }
}