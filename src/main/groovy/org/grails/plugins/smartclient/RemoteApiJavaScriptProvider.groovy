package org.grails.plugins.smartclient

import grails.core.GrailsServiceClass
import groovy.text.SimpleTemplateEngine
import org.apache.commons.lang.StringUtils
import org.grails.plugins.smartclient.annotation.Operation
import org.grails.plugins.smartclient.annotation.P
import org.grails.plugins.smartclient.annotation.Remote

import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter

/**
 * This is the component responsible to output initialization and remote API definition JavaScript
 * @Denis Halupa
 */
class RemoteApiJavaScriptProvider {
    def configProvider
    def grailsApplication


    String remoteApiTemplateText = 'var rmt=new function(){var emptyFn = function () {};return {${services}}}();'
    String serviceTemplateText = '${serviceName}:{${methods}}'
    String functionSignatureTemplateText = 'function (${params} , callback)'
    String noParamsFunctionSignatureTemplateText = 'function (callback)'
    String functionTemplateText = '''
$methodName : $functionSignature {
var cb=callback||emptyFn;
var arg = {__params: [${params}]};
isc.RemoteMethod.invoke('${serviceName}.${methodName}', arg, cb);
}
'''


    String createRemoteAPI() {
        def engine = new SimpleTemplateEngine()
        def remoteApiTemplate = engine.createTemplate(StringUtils.replaceChars(remoteApiTemplateText, '\n', ''))
        def serviceTemplate = engine.createTemplate(StringUtils.replaceChars(serviceTemplateText, '\n', ''))

        def functionTemplate = engine.createTemplate(StringUtils.replaceChars(functionTemplateText, '\n', ''))
        def bindingModel = [:]
        def serviceDefinitions = []
        grailsApplication.serviceClasses.each { GrailsServiceClass sc ->
            def clazz = sc.clazz
            boolean remoteService = clazz.getAnnotation(Remote.class)
            def methods = remoteService ? extractUserMethods(clazz) : clazz.methods.findAll {
                it.getAnnotation(Remote.class)
            }
            if (methods) {
                bindingModel.serviceName = clazz.simpleName
                def methodDefinitions = []
                methods.sort { it.name }.each { Method m ->
                    if (remoteService || m.getAnnotation(Remote).value() == Operation.CUSTOM) {
                        bindingModel.methodName = m.name
                        def paramsMeta = m.parameters.collect { Parameter parameter ->
                            def paramAnnotation = parameter.getAnnotation(P)
                            paramAnnotation ? paramAnnotation.value() : parameter.name
                        }
                        bindingModel.params = paramsMeta.join(',')
                        if (paramsMeta.empty) {
                            bindingModel.functionSignature = 'function(callback)'
                        } else {
                            bindingModel.functionSignature = "function(${bindingModel.params}, callback)".toString()
                        }
                        methodDefinitions << functionTemplate.make(bindingModel).toString()
                    }
                }
                bindingModel.methods = methodDefinitions.join(',')
                serviceDefinitions << serviceTemplate.make(bindingModel).toString()
            }
        }
        bindingModel.services = serviceDefinitions.join(',')
        remoteApiTemplate.make(bindingModel).toString()

    }


    private def extractUserMethods(Class serviceClass) {
        def methods = []
        // Find out what properties the service class contains, because
        // we want to leave them out of the interface definition.
        def info = Introspector.getBeanInfo(serviceClass)
        def propMethods = [] as Set
        info.propertyDescriptors.each { PropertyDescriptor desc ->
            propMethods << desc.readMethod
            propMethods << desc.writeMethod

            // Groovy adds a "get*()" method for booleans as well as
            // the usual "is*()", so we have to remove it too.
            if (desc.readMethod?.name?.startsWith("is")) {
                def name = "get${desc.readMethod.name[2..-1]}".toString()
                def getMethod = info.methodDescriptors.find { it.name == name }
                if (getMethod) {
                    propMethods << getMethod.method
                }
            }
        }

        // Iterate through the methods declared by the Grails service,
        // adding the appropriate ones to the interface definitions.
        serviceClass.declaredMethods.each { Method method ->
            // Skip non-public, static, Groovy, and property methods.
            if (method.synthetic ||
                    !Modifier.isPublic(method.modifiers) ||
                    Modifier.isStatic(method.modifiers) ||
                    propMethods.contains(method)) {
                return
            }
            methods << method
        }
        methods
    }

}
