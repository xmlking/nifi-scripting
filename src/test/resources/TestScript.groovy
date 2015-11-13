import org.apache.nifi.processor.io.InputStreamCallback



log.error("sumo")
log.error(args.toString())

log.error(session.toString())

flowFile = session.putAttribute(flowFile, "GROOVY", 111 as String);
session.read(flowFile, new InputStreamCallback() {
    @Override
    void process(InputStream inputStream) throws IOException {
        final stream = new DataInputStream(inputStream)
        int num = 1;
        for (String line: stream.readLines()) {
            log.info(line)
        }
    }
})
