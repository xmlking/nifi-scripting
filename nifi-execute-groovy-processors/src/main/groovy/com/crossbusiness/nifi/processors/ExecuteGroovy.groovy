package com.crossbusiness.nifi.processors

import groovy.transform.CompileStatic
import org.apache.nifi.annotation.behavior.EventDriven
import org.apache.nifi.annotation.behavior.InputRequirement
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement
import org.apache.nifi.annotation.documentation.CapabilityDescription
import org.apache.nifi.annotation.documentation.Tags
import org.apache.nifi.annotation.lifecycle.OnScheduled
import org.apache.nifi.components.PropertyDescriptor
import org.apache.nifi.components.Validator
import org.apache.nifi.flowfile.FlowFile
import org.apache.nifi.logging.ProcessorLog
import org.apache.nifi.processor.*
import org.apache.nifi.processor.exception.ProcessException
import org.apache.nifi.util.StopWatch

import java.util.concurrent.TimeUnit

@CompileStatic
@EventDriven
@InputRequirement(Requirement.INPUT_ALLOWED)
@Tags(["command", "process", "source", "invoke", "groovy", "script"])
@CapabilityDescription("Runs Groovy script. User supplied script can assign any data to flowFile that can be passed to next processor.")
public class ExecuteGroovy extends AbstractProcessor {

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


    public static final PropertyDescriptor GROOVY_SCRIPT = new PropertyDescriptor.Builder()
            .name("Groovy Script")
            .required(true)
            .description("Groovy script to execute")
            .addValidator(Validator.VALID)
            .build();

    public static final PropertyDescriptor GROOVY_ARGS = new PropertyDescriptor.Builder()
            .name("Arguments")
            .required(false)
            .description("Arguments to pass to Groovy")
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
        properties.add(GROOVY_SCRIPT);
        properties.add(GROOVY_ARGS);
        this.properties = Collections.unmodifiableList(properties);
    }

    private Script groovyScript;

    @OnScheduled
    public void setup(final ProcessContext context) {
        GroovyShell groovyShell = new GroovyShell();
        groovyScript = groovyShell.parse(context.getProperty(GROOVY_SCRIPT).getValue());
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

        String[] args = context.getProperty(GROOVY_ARGS).evaluateAttributeExpressions().getValue().split(";");
        ProcessorLog log = getLogger();

        // Create new bindings
        Binding binding = new Binding();
        binding.setVariable("args", args);
        binding.setVariable("session", session);
        binding.setVariable("flowFile", incoming);
        binding.setVariable("log", log);

        try {
            final StopWatch stopWatch = new StopWatch(true);
            groovyScript.setBinding(binding);
            groovyScript.run();

            outgoing = (FlowFile) binding.getProperty("flowFile");
            if(outgoing != null) {
                log.info("Successfully processed ${outgoing} (${outgoing.getSize()}) in ${stopWatch.getElapsed(TimeUnit.MILLISECONDS)} millis");
                session.transfer(outgoing, REL_SUCCESS);
            }
        } catch (ProcessException pe) {
            if (incoming == null) {
                log.error("Unable to process due to ${pe.toString()}. No incoming flow file to route to failure", pe);
            } else {
                log.error("Unable to process ${incoming} due to ${pe.toString()}, routing to failure", pe);
                session.transfer(incoming, REL_FAILURE);
            }
        }
    }
}
