import groovy.json.JsonOutput

import javax.naming.Binding
import javax.naming.Context
import javax.naming.NamingEnumeration
import javax.naming.directory.DirContext
import javax.naming.ldap.InitialLdapContext

env = [(Context.INITIAL_CONTEXT_FACTORY) : "com.sun.jndi.ldap.LdapCtxFactory",
       (Context.PROVIDER_URL): "ldap://${flowFile.getAttribute('host')}:${flowFile.getAttribute('port')}" as String,
       (Context.SECURITY_AUTHENTICATION): "simple",
       (Context.SECURITY_PRINCIPAL): ldap.bindUser,
       (Context.SECURITY_CREDENTIALS): ldap.bindPassword] as Hashtable

ctx = new InitialLdapContext( env, null );
DirContext schema = ctx.getSchema("");
NamingEnumeration<Binding> attributeTypes = schema.listBindings("AttributeDefinition");

attributes = Collections.list(attributeTypes).stream()
        .map({ binding -> (DirContext)(binding).getObject() })
        .map({ dc -> (Collections.list(dc.getAttributes("").getAll()))})
        .map({ attbList -> attbList.collectEntries{[it.getID(), it.get()]}}) //List to Map
        .collect()

//println JsonOutput.toJson(attributes)
newFlowFile = util.stringToFlowFile(JsonOutput.toJson(attributes) , session, flowFile);
session.remove(flowFile);
flowFile = newFlowFile