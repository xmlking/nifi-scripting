import com.crossbusiness.nifi.processors.ExecuteGroovyLdap
import groovy.json.JsonOutput
import org.apache.directory.groovyldap.Search
import org.apache.directory.groovyldap.SearchScope

log.info("ldap : " + ldap.url)

params  = new Search()
params.base = 'ou=sumo,ou=demo,dc=cc,dc=com'
params.scope = SearchScope.ONE
params.filter = '(objectclass=*)'

ldap.eachEntry (params) { entry ->
    println "${entry.cn}   (${entry.dn})"
    attbs['cn'] = entry.cn
    attbs['dn'] = entry.dn

    ff = util.stringToFlowFile(JsonOutput.toJson(attbs), session)

    //session.putAllAttributes(ff,attbs)
    session.transfer(ff, ExecuteGroovyLdap.REL_SUCCESS);
}

// if there is an incoming flowFile, then replace it with new outgoing flowFile,
// remove incoming flowFile and also set outgoing flowFile to null
// flowFile = session.remove(flowFile);
// if triggered by scheduler without incoming flowFile, no action is needed.



