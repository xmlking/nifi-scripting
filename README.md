NiFi-Scripting
--------------
NiFi Dynamic Script Executors

```python
Work-in-Progress
```
The goal of this project is to enable processing NiFi *FlowFiles* using scripting languages.   
   
1. **ExecuteJavaScript**  Usage: JSON -> Mapping -> JSON
2. **ExecuteGroovy**      execute supplied groovy script with arguments configured. 
 
### Install
1. Manual: Download [Apache NiFi](https://nifi.apache.org/download.html) binaries and unpack to a folder. 
2. On Mac: brew install nifi

### Deploy
```bash
# Assume you unpacked nifi-0.3.0-bin.zip to /Developer/Applications/nifi
./gradlew clean deploy -Pnifi_home=/Developer/Applications/nifi
```
On Mac 
```bash
gradle clean deploy -Pnifi_home=/usr/local/Cellar/nifi/0.3.0/libexec
```

### Run
```bash
cd /Developer/Applications/nifi
./bin/nifi.sh  start
./bin/nifi.sh  stop
```
On Mac 
```bash
# nifi start|stop|run|restart|status|dump|install
nifi start 
nifi status  
nifi stop 
# Working Directory: /usr/local/Cellar/nifi/0.3.0/libexec
```
### Testing 

Upload the [sample flow](./scripting-flow.xml) into NiFi and use [test data](./src/test/resources/test.json) and below javascript for testing:

```js
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
```

### TODO
1. Support adding popular javaScript libraries (lodash.js, moment.js etc.,) via processor configuration.
 

### Reference  
1. [Groovy Script](http://www.groovy-lang.org/integrating.html)
2. [java8-nashorn-tutorial](http://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/)
3. [Mapping Complex JSON Structures With JDK8 Nashorn](https://dzone.com/articles/mapping-complex-json-structures-with-jdk8-nashorn)