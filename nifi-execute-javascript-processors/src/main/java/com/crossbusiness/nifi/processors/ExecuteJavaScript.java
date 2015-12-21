package com.crossbusiness.nifi.processors;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ProcessorLog;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.util.StopWatch;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ExecuteJavaScript extends AbstractProcessor {

    private Set<Relationship> relationships;
    private List<PropertyDescriptor> properties;

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("FlowFiles that were successfully processed")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("FlowFiles that were failed to process")
            .build();


    public static final PropertyDescriptor JAVA_SCRIPT = new PropertyDescriptor.Builder()
            .name("JavaScript")
            .required(true)
            .description("JavaScript to execute")
            .addValidator(Validator.VALID)
            .build();

    public static final PropertyDescriptor JAVASCRIPT_ARGS = new PropertyDescriptor.Builder()
            .name("Arguments")
            .required(false)
            .description("Arguments to pass to JavaScript")
            .addValidator(Validator.VALID)
            .expressionLanguageSupported(true)
            .defaultValue("")
            .build();

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);

        final List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
        properties.add(JAVA_SCRIPT);
        properties.add(JAVASCRIPT_ARGS);
        this.properties = Collections.unmodifiableList(properties);
    }


    private CompiledScript compiledJavaScript;


    @OnScheduled
    public void setup(final ProcessContext context) throws ScriptException {
        NashornScriptEngineFactory engineFactory = new NashornScriptEngineFactory();
        Compilable engine = (Compilable) engineFactory.getScriptEngine();
        compiledJavaScript = engine.compile(context.getProperty(JAVA_SCRIPT).getValue());
    }



    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        String[] args = context.getProperty(JAVASCRIPT_ARGS).evaluateAttributeExpressions().getValue().split(";");
        ProcessorLog log = getLogger();


        // Create new bindings
        Bindings binding = compiledJavaScript.getEngine().createBindings();
        binding.put("args", args);
        binding.put("session", session);
        binding.put("flowFile", flowFile);
        binding.put("log", log);
        binding.put("util", new NiFiUtils());

        try {

            final StopWatch stopWatch = new StopWatch(true);

            compiledJavaScript.eval(binding);

            flowFile = (FlowFile) binding.get("flowFile");

            log.info("Successfully processed flowFile {} ({}) in {} millis", new Object[]{flowFile, flowFile.getSize(), stopWatch.getElapsed(TimeUnit.MILLISECONDS)});
            session.transfer(flowFile, REL_SUCCESS);
        } catch (ProcessException | ScriptException pe) {
            log.error("Failed to process flowFile {} due to {}; routing to failure", new Object[] {flowFile, pe.toString()}, pe);
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}



