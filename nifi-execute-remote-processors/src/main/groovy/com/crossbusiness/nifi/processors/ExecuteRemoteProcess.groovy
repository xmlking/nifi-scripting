package com.crossbusiness.nifi.processors

import com.jcraft.jsch.JSchException
import groovy.transform.CompileStatic
import org.apache.nifi.annotation.behavior.InputRequirement
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement
import org.apache.nifi.annotation.behavior.TriggerSerially
import org.apache.nifi.annotation.documentation.CapabilityDescription
import org.apache.nifi.annotation.documentation.Tags
import org.apache.nifi.annotation.lifecycle.OnScheduled
import org.apache.nifi.annotation.lifecycle.OnUnscheduled
import org.apache.nifi.components.PropertyDescriptor
import org.apache.nifi.components.Validator
import org.apache.nifi.flowfile.FlowFile
import org.apache.nifi.logging.ProcessorLog
import org.apache.nifi.processor.*
import org.apache.nifi.processor.exception.ProcessException
import org.apache.nifi.processor.util.StandardValidators
import org.apache.nifi.util.StopWatch
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.BadExitStatusException

import java.util.concurrent.TimeUnit

@CompileStatic
@TriggerSerially
@InputRequirement(Requirement.INPUT_FORBIDDEN)
@Tags(["command", "process", "source", "external", "remote", "invoke", "script"])
@CapabilityDescription('''Runs an operating system command specified by the user on remote host and writes the output of that command to a FlowFile.
                          If the command is expectedto be long-running, the Processor can output the partial data on a specified interval.
                          When this option is used, the output is expected to be in textual format,
                          as it typically does not make sense to split binary data on arbitrary time-based intervals.''')
public class ExecuteRemoteProcess extends AbstractProcessor {

    private Set<Relationship> relationships;
    private List<PropertyDescriptor> properties;

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("All created FlowFiles are routed to this relationship")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("FlowFiles that were failed to process")
            .build();


    public static final PropertyDescriptor CONFIG_DSL = new PropertyDescriptor.Builder()
            .name("SSH Config DSL")
            .required(true)
            .description("Remote host SSL configuration")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor RUN_DSL = new PropertyDescriptor.Builder()
            .name("Run DSL")
            .required(true)
            .description("Run DSL to execute on remote hosts")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor RUN_DSL_ARGS = new PropertyDescriptor.Builder()
            .name("Run DSL arguments")
            .required(false)
            .description("Arguments passed to Run DSL")
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
        properties.add(CONFIG_DSL);
        properties.add(RUN_DSL);
        properties.add(RUN_DSL_ARGS);
        this.properties = Collections.unmodifiableList(properties);
    }

    private Service ssh = Ssh.newService();
    private Script configDsl;
    private Script runDsl;

    @OnScheduled
    public void setup(final ProcessContext context) throws JSchException {
        GroovyShell groovyShell = new GroovyShell();
        configDsl = groovyShell.parse(context.getProperty(CONFIG_DSL).getValue());
        SshBinding binding = new SshBinding();
        binding.setBuilder(ssh);
        configDsl.setBinding(binding);
        configDsl.run();
        runDsl = groovyShell.parse(context.getProperty(RUN_DSL).getValue());
    }

    @OnUnscheduled
    public void shutdown() {
        //TODO : should we close SSH?
    }


    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {

        String[] args = context.getProperty(RUN_DSL_ARGS).evaluateAttributeExpressions().getValue().split(";");
        ProcessorLog log = getLogger();

        // Create new bindings
        Binding binding = new Binding();
        String result = null;
        binding.setVariable("result", result);
        binding.setVariable("ssh", ssh);
        binding.setVariable("args", args);
        binding.setVariable("log", log);

        try {
            final StopWatch stopWatch = new StopWatch(true);
            runDsl.setBinding(binding);
            runDsl.run();
            result = (String) binding.getProperty("result");

            if(result != null) {
                FlowFile flowFile = new NiFiUtils().stringToFlowFile(result, session);
                log.info("Successfully executed remote command. ${flowFile} (${flowFile.getSize()}) in ${stopWatch.getElapsed(TimeUnit.MILLISECONDS)} millis");
                session.transfer(flowFile, REL_SUCCESS);
            } else {
                log.warn("no result???")
            }
        } catch (ProcessException | BadExitStatusException pe) {
            log.error("Failed to execute remote command due to ${pe.toString()}, routing to failure", pe);
            FlowFile flowFile = new NiFiUtils().exceptionToFlowFile(pe, session);
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}
