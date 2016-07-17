NiFi-Scripting
--------------
The goal of this project is to enable processing NiFi *FlowFiles* using scripting languages.   
   
1. **ExecuteJavaScript**        Execute supplied javaScript with arguments configured. Use case: JSON -> Mapping -> JSON
2. **ExecuteGroovy**:           Execute supplied groovy script with arguments configured. 
3. **ExecuteGroovyLdap**:       Provide configured LDAP object to the script. Execute supplied groovy script with arguments configured. See [Groovy LDAP](https://directory.apache.org/api/groovy-api/2-groovy-ldap-user-guide.html)
4. **ExecuteRemoteProcess**:    Similar to NiFi built-in **ExecuteProcess** but run on remote host. See [Groovy SSH](https://github.com/int128/groovy-ssh)


> **ExecuteJavaScript** and **ExecuteGroovy** are depricated since NiFi from version 0.5.1 includes **ExecuteScript** and **InvokeScriptProcessor**

You can still take advantage of **nifi-sumo-common** lib in scripting processors, e.g., convert `FlowFile <--> String`


### Install NiFi
1. Manual: Download [Apache NiFi](https://nifi.apache.org/download.html) binaries and unpack to a folder. 
2. On Mac: brew install nifi

### Deploy NAR files.
```bash
# Assume you unpacked nifi-0.6.1-bin.zip to /Developer/Applications/nifi
gradle clean deploy -Pnifi_home=/Developer/Applications/nifi
```

```
# if you install NiFi via brew
gradle clean deploy -Pnifi_home=/usr/local/Cellar/nifi/0.6.1/libexec
```

### Start NiFi
```bash
cd /Developer/Applications/nifi
./bin/nifi.sh  start
./bin/nifi.sh  stop
```

```
# If you install NiFi via brew
# Working Directory: /usr/local/Cellar/nifi/0.6.1/libexec
nifi start|stop|run|restart|status|dump|install
nifi start 
nifi status  
nifi stop
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

#### Using 3rd party libs with Groovy Script via @Grab
> You need to copy Apache Ivy [JAR](http://ant.apache.org/ivy/download.cgi) to **NiFi/bin** for @Grab to work.

Add your twitter's *consumerKey, consumerSecret accessToken and secretToken* to ExecuteScript's dynamic properties 

```Groovy
@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7')
@Grab('oauth.signpost:signpost-core:1.2.1.2')
@Grab('oauth.signpost:signpost-commonshttp4:1.2.1.2')

import groovy.json.JsonOutput
import groovyx.net.http.RESTClient
import static groovyx.net.http.ContentType.*
import org.apache.http.params.HttpConnectionParams
import com.crossbusiness.nifi.processors.NiFiUtils as util

//Access dynamic property
consumerKey = consumerKey.value
consumerSecret = consumerSecret.value
accessToken = accessToken.value
secretToken = secretToken.value

def twitter = new RESTClient( 'https://api.twitter.com/1.1/statuses/' )
twitter.auth.oauth  consumerKey, consumerSecret, accessToken, secretToken
twitter.contentType = JSON
HttpConnectionParams.setSoTimeout twitter.client.params, 15000

def resp = twitter.get( path: 'home_timeline.json' )
assert resp.status == 200
assert resp.contentType == JSON.toString()
assert ( resp.data instanceof List )
assert resp.data.status.size() > 0

flowFile = util.stringToFlowFile(JsonOutput.toJson(resp.data), session);
session.transfer(flowFile, REL_SUCCESS)
```


### Using `nifi-sumo-common` lib in scripting processors

1. Copy `nifi-sumo-common-x.y.x-SNAPSHOT.jar` from [releases](https://github.com/xmlking/nifi-scripting/releases) to  *Module Directory* set in the `ExecuteScript` Processor's properties. 

#### How to use NiFiUtil?

2. Import `NiFiUtils` into `ExecuteScript`'s Script 
```groovy
import com.crossbusiness.nifi.processors.NiFiUtils as util
flowFile = util.stringToFlowFile("test 123", session);
flowString = util.flowFileToString(flowFile, session)
log.info "flowString: ${flowString}"
session.transfer(flowFile, REL_SUCCESS)
```

#### How to use distributed cache in scripting processors?

2. Import `StringSerDe` or `LongSerDe` etc., into `ExecuteScript`'s Script 

```groovy
import org.apache.nifi.controller.ControllerService
import com.crossbusiness.nifi.processors.StringSerDe

final StringSerDe stringSerDe = new StringSerDe();

def lookup = context.controllerServiceLookup
def cacheServiceName = DistributedMapCacheClientServiceName.value

log.error  "cacheServiceName: ${cacheServiceName}"

def cacheServiceId = lookup.getControllerServiceIdentifiers(ControllerService).find {
    cs -> lookup.getControllerServiceName(cs) == cacheServiceName
}

log.error  "cacheServiceId:  ${cacheServiceId}"

def cache = lookup.getControllerService(cacheServiceId)
log.error cache.get("aaa", stringSerDe, stringSerDe )
```


#### ExecuteRemoteProcess testing

**SSH Config DSL**
```groovy
remotes {
    web01 {
        role 'masterNode'
        host = '192.168.1.5'
        user = 'sumo'
        password = 'demo'
        knownHosts = allowAnyHosts
    }
    web02 {
        host = '192.168.1.5'
        user = 'sumo'
        knownHosts = allowAnyHosts
    }
}
```

**Run DSL**
```groovy
ssh.run {
    session(ssh.remotes.web01) {
          result = execute 'uname -a' 
    }
}
```

### Build 
```bash
gradle nar
```

#### If you are using MapR hadoop distribution

1. Follow steps [NiFi Hadoop Library for MapR](https://github.com/xmlking/mapr-nifi-hadoop-libraries-bundle)
2. Set auth login config in $NIFI_HOME/conf/bootstrap.conf

    `java.arg.15=-Djava.security.auth.login.config=/opt/mapr/conf/mapr.login.conf`


### TODO
1. Support adding popular javaScript libraries (lodash.js, moment.js etc.,) via processor configuration.
1. ExecuteRemoteProcess: add expression language support for RUN_DSL.
 

### Reference  
1. [Groovy Script](http://www.groovy-lang.org/integrating.html)
2. [java8-nashorn-tutorial](http://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/)
3. [Mapping Complex JSON Structures With JDK8 Nashorn](https://dzone.com/articles/mapping-complex-json-structures-with-jdk8-nashorn)
4. [Groovy SSH](https://github.com/int128/groovy-ssh)
5. See [document of Gradle SSH Plugin](https://gradle-ssh-plugin.github.io/docs/) for details of DSL.
6. Groovy Goodness: [Store Closures in Script Binding](http://mrhaki.blogspot.com/2010/08/groovy-goodness-store-closures-in.html)
7. Matt Burgess's Blog: [Fun with Apache NiFi](http://funnifi.blogspot.com/)