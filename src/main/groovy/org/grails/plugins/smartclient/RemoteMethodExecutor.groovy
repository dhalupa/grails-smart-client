package org.grails.plugins.smartclient

import groovy.util.logging.Log4j
import org.apache.commons.lang.StringUtils
import org.grails.plugins.smartclient.annotation.Operation
import org.grails.plugins.smartclient.annotation.Remote
import org.springframework.aop.framework.AopProxyUtils

/**
 * Created by dhalupa on 29.04.16..
 */
@Log4j
class RemoteMethodExecutor implements SmartClientResponseRenderer {
    def grailsApplication

    /**
     * Executes batch of operations in a single transaction
     * @param transaction
     * @return
     */
    def executeTransaction(transaction) {
        def model = []
        transaction.operations.each { request ->
            def resp = execute(request.data)
            resp.response.queueStatus = 0
            model << resp
        }
        model
    }

    /**
     * Executes batch single operation
     * @param data is json request
     * @return
     */
    def execute(data) {
        String[] methodLocator = StringUtils.split(data.remove('__method'), '.')
        String serviceName = StringUtils.uncapitalize(methodLocator[0])
        String methodName = methodLocator[1]
        def retValue
        try {
            retValue = remoteMethodHandler.call(serviceName, methodName, data)
        } catch (Throwable t) {
            log.error(t.message, t)
            retValue = renderErrorResponse(t.message)
        }
        retValue
    }


    private remoteMethodHandler = { serviceName, methodName, data ->
        def service = grailsApplication.mainContext.getBean(serviceName)
        def methods = service.metaClass.methods.findAll { it.name == methodName }
        Class clazz = AopProxyUtils.ultimateTargetClass(service)
        if (methods.size() == 1) {
            def m = methods[0].cachedMethod
            if (m.getAnnotation(Remote) || clazz.getAnnotation(Remote)) {
                if (data['__params']) {
                    data = data['__params'].collect { it } as Object[]
                }
                def value = service.invokeMethod(methodName, data)
                def raw = m.getAnnotation(Remote)?.raw() ?: false
                if (!raw) {
                    def operation = m.getAnnotation(Remote)?.operation() ?: Operation.CUSTOM
                    switch (operation) {
                        case Operation.ADD:
                        case Operation.UPDATE:
                        case Operation.REMOVE:
                        case Operation.CUSTOM:
                            return renderDataResponse(value)
                        case Operation.FETCH:
                            return renderFetchResponse(value)

                    }
                } else {
                    return value
                }
            } else {
                throw new RuntimeException("Method ${clazz.getName()}#${methodName} is not exposed for remote invocation")
            }
        } else {
            if (methods.isEmpty()) {
                throw new RuntimeException("Method ${clazz.getName()}#${methodName} could not be found")
            } else {
                throw new RuntimeException("Method ${clazz.getName()}#${methodName} can not be overloaded. It is not supported")
            }
        }

    }
}
