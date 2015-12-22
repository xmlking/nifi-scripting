log.info("sumo")
log.info("args: "  + args.toString())
log.info("ldap : " + ldap.url)

//ldap.eachEntry (filter: '(objectClass=person)') { entry ->
//    println "${entry.cn} (${entry.dn})"
//}

flowFile = session.putAttribute(flowFile, "MY_ARG_0", args[0]);
