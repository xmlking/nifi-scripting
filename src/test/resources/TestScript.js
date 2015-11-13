
function convert(val) {
    var g = JSON.parse(val);
    var d = {
        widget: g.widget.window.title,
        imageURI: g.widget.image.src
    };
    return JSON.stringify(d);
}

print("Hello from inside scripting!");

var  fs = util.flowFileToString(flowFile, session);
log.error(fs);
var flowString = convert(fs);
log.error(flowString);

//flowFile = session.importFrom(buff, true, flowFile);
session.remove(flowFile);
flowFile = util.stringToFlowFile(flowString, session);


flowFile = session.putAttribute(flowFile, "JS", 2222 );





