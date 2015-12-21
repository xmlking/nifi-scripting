import org.apache.nifi.processor.io.InputStreamCallback

log.info("sumo")
log.info("args: "  + args.toString())

flowFile = session.putAttribute(flowFile, "MY_ARG_0", args[0]);
