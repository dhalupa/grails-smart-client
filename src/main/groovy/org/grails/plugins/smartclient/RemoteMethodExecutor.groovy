package org.grails.plugins.smartclient


import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
import org.grails.plugins.smartclient.annotation.Operation
import org.grails.plugins.smartclient.annotation.Remote
import org.springframework.aop.framework.AopProxyUtils

/**
 * Created by dhalupa on 29.04.16..
 */
@Slf4j
class RemoteMethodExecutor implements SmartClientResponseRenderer {
    def grailsApplication
    def messageSource

    /**
     * Executes batch of operations in a single transaction
     * @param transaction
     * @return
     */
    def executeTransaction(transaction, locale) {
        def model = []
        transaction.operations.each { request ->
            def resp = execute(request, locale)
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
    def execute(request, locale) {
        String conversationId = request.data.remove('__conversationId')
        ConversationScope.CURRENT_CONVERSATION.set(conversationId)
        String[] methodLocator = StringUtils.split(request.data.remove('__method'), '.')
        String serviceName = StringUtils.uncapitalize(methodLocator[0])
        String methodName = methodLocator[1]
        def retValue
        try {
            retValue = remoteMethodHandler.call(serviceName, methodName, request, locale)
        } catch (Throwable t) {
            log.error(t.message, t)
            retValue = renderErrorResponse(t.message)
        }
        retValue
    }


    private remoteMethodHandler = { serviceName, methodName, request, locale ->
        def data = request.data
        def service = grailsApplication.mainContext.getBean(serviceName)
        def methods = service.metaClass.methods.findAll { it.name == methodName }
        Class clazz = AopProxyUtils.ultimateTargetClass(service)
        if (methods.size() == 1) {
            def m = methods[0].cachedMethod
            def operation = m.getAnnotation(Remote)?.value() ?: Operation.CUSTOM
            if (m.getAnnotation(Remote) || clazz.getAnnotation(Remote)) {
                switch (operation) {
                    case Operation.CUSTOM:
                        data = data['__params'].collect { it } as Object[]
                        break
                    case Operation.FETCH:
                        ['startRow', 'endRow', 'textMatchStyle', 'sortBy'].each {
                            if (!request.isNull(it)) {
                                data."_${it}" = request[it]
                            }
                        }
                }
                def value = service.invokeMethod(methodName, data)
                def raw = m.getAnnotation(Remote)?.raw() ?: false
                if (!raw) {
                    switch (operation) {
                        case Operation.ADD:
                        case Operation.UPDATE:
                        case Operation.REMOVE:
                            return renderDataUpdateResponse(value, messageSource, locale)
                        case Operation.CUSTOM:
                            return renderDataResponse([result: value])
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
