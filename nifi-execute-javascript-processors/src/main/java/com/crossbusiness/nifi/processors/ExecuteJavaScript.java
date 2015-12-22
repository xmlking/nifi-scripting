package com.crossbusiness.nifi.processors;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
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

@EventDriven
@InputRequirement(Requirement.INPUT_ALLOWED)
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
        FlowFile outgoing, incoming = null;
        if (context.hasIncomingConnection()) {
            incoming = session.get();

            // If we have no FlowFile, and all incoming connections are self-loops then we can continue on.
            // However, if we have no FlowFile and we have connections coming from other Processors, then
            // we know that we should run only if we have a FlowFile.
            if (incoming == null && context.hasNonLoopConnection()) {
                return;
            }
        }

        String[] args = context.getProperty(JAVASCRIPT_ARGS).evaluateAttributeExpressions().getValue().split(";");
        ProcessorLog log = getLogger();


        // Create new bindings
        Bindings binding = compiledJavaScript.getEngine().createBindings();
        binding.put("args", args);
        binding.put("session", session);
        binding.put("flowFile", incoming);
        binding.put("log", log);
        binding.put("SUCCESS", REL_SUCCESS);
        binding.put("FAILURE", REL_FAILURE);
        binding.put("util", new NiFiUtils());

        try {

            final StopWatch stopWatch = new StopWatch(true);

            compiledJavaScript.eval(binding);

            outgoing = (FlowFile) binding.get("flowFile");
            if(outgoing != null) {
                log.info("Successfully processed {} ({}) in {} millis", new Object[]{outgoing, outgoing.getSize(), stopWatch.getElapsed(TimeUnit.MILLISECONDS)});
                session.transfer(outgoing, REL_SUCCESS);
            }
        } catch (ProcessException | ScriptException pe) {
            if (incoming == null) {
                log.error("Unable to process due to {}. No incoming flow file to route to failure", new Object[] {pe.toString()}, pe);
            } else {
                log.error("Failed to process {} due to {}; routing to failure", new Object[] {incoming, pe.toString()}, pe);
                session.transfer(incoming, REL_FAILURE);
            }
        }
    }
}



