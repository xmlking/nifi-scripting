package com.crossbusiness.nifi.processors;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script
import groovy.transform.CompileStatic;
import org.apache.directory.groovyldap.LDAP
import org.apache.nifi.annotation.behavior.EventDriven
import org.apache.nifi.annotation.behavior.InputRequirement
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.TriggerSerially;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnUnscheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ProcessorLog;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;


import java.util.*;
import java.util.concurrent.TimeUnit;

@CompileStatic
@TriggerSerially
@EventDriven
@InputRequirement(Requirement.INPUT_ALLOWED)
@Tags(["command", "process", "source", "ldap", "invoke", "search", "groovy", "script"])
@CapabilityDescription('''Runs Groovy script with LDAP binding.
                          User supplied script can assign any data to flowFile that can be passed to next processor.''')
public class ExecuteGroovyLdap extends AbstractProcessor {
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

    public static final PropertyDescriptor LDAP_URI = new PropertyDescriptor.Builder()
            .name("LDAP URI")
            .required(true)
            .description("LDAP URI")
            .addValidator(Validator.VALID)
            .build();

    public static final PropertyDescriptor LDAP_USERNAME = new PropertyDescriptor.Builder()
            .name("LDAP Username")
            .required(true)
            .description("LDAP Username")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor LDAP_PASSWORD = new PropertyDescriptor.Builder()
            .name("LDAP Password")
            .required(true)
            .description("LDAP Password")
            .addValidator(Validator.VALID)
            .sensitive(true)
            .build();

    public static final PropertyDescriptor GROOVY_SCRIPT = new PropertyDescriptor.Builder()
            .name("Groovy LDAP Script")
            .required(true)
            .description("Groovy LDAP script to execute")
            .addValidator(Validator.VALID)
            .build();

    public static final PropertyDescriptor GROOVY_ARGS = new PropertyDescriptor.Builder()
            .name("Arguments")
            .required(false)
            .description("Arguments to pass to Groovy script")
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
        properties.add(LDAP_URI);
        properties.add(LDAP_USERNAME);
        properties.add(LDAP_PASSWORD);
        properties.add(GROOVY_SCRIPT);
        properties.add(GROOVY_ARGS);
        this.properties = Collections.unmodifiableList(properties);
    }

    private Script groovyScript;
    private LDAP ldap;

    @OnScheduled
    public void setup(final ProcessContext context) {
        GroovyShell groovyShell = new GroovyShell();
        ldap = LDAP.newInstance(context.getProperty(LDAP_URI).value, context.getProperty(LDAP_USERNAME).value, context.getProperty(LDAP_PASSWORD).value)
        groovyScript = groovyShell.parse(context.getProperty(GROOVY_SCRIPT).getValue());
    }


    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile incoming, outgoing = null;
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
        binding.setVariable("ldap", ldap);
        binding.setVariable("log", log);
        binding.setVariable("util", new NiFiUtils());

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
                logger.error("Unable to process due to ${pe.toString()}. No incoming flow file to route to failure", pe);
            }
            log.error("Unable to process ${incoming} due to ${pe.toString()}, routing to failure", pe);
            session.transfer(incoming, REL_FAILURE);
        }
    }
}
