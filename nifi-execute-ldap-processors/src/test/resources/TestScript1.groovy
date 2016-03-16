import groovy.json.JsonOutput

log.info("ldap : " + ldap.url)

params  = [:]
params.base = 'ou=sumo,ou=demo,dc=cc,dc=com'
params.scope = ldap.ONE
params.filter = '(objectclass=*)'

attbs = [:]

ldap.eachEntry (params) { entry ->
    println "${entry.cn}   (${entry.dn})"
    attbs['cn'] = entry.cn
    attbs['dn'] = entry.dn

    ff = util.stringToFlowFile(JsonOutput.toJson(attbs), session)
    ff = session.putAllAttributes(ff,attbs)
    session.transfer(ff, SUCCESS);
}

// if there is an incoming flowFile, then replace it with new outgoing flowFile,
// if you don't need incoming flowFile, remove incoming flowFile and also set outgoing flowFile to null
// null tells parent processor, no transfer is required for the flowFile.
// flowFile = session.remove(flowFile);
// if processor is triggered by timer without incoming flowFile, no action is needed.



