import groovy.json.JsonOutput

ldap.url =  "ldap://${flowFile.getAttribute('host')}:${flowFile.getAttribute('port')}"
def config = ldap.read('cn=config')
newFlowFile = util.stringToFlowFile(JsonOutput.toJson(config) , session, flowFile);
session.remove(flowFile);
flowFile = newFlowFile
