package com.crossbusiness.nifi.processors

import groovy.transform.CompileStatic

@CompileStatic
class SshBinding extends Binding{
    def builder

    Object getVariable(String name) {
        return { Object... args -> builder.invokeMethod(name,args) }
    }
}