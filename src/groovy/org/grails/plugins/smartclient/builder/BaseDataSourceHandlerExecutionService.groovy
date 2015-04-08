package org.grails.plugins.smartclient.builder

import org.grails.plugins.smartclient.annotation.Remote
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Method

/**
 * Created by dhalupa on 4/8/15.
 */
abstract class BaseDataSourceHandlerExecutionService {
    def grailsApplication
    def messageSource
    /**
     * Executes batch of operations in a single transaction
     * @param transaction
     * @return
     */
    def executeTransaction(transaction) {
        def model = []
        transaction.operations.each { request ->
            def resp = executeOperation(null, request)
            resp.response.queueStatus = 0
            model << resp
        }
        model
    }
    /**
     * Executes batch single operation
     * @param transaction
     * @return
     */
    def executeOperation(dsID, request) {
        def retValue
        def dsName = dsID != null ? dsID : request.dataSource
        if (dsName.endsWith('Service')) {
            def service = resolveService(dsName)
            try {
                retValue = remoteMethodHandler.call(service, request)
            } catch (Throwable t) {
                log.error(t.message, t)
                retValue = ['response': [status: -1, data: t.message]]
            }
        } else {
            def dataSource = resolveDataSource(dsName)
            def handler = request.operationType == 'custom' ? customOperationHandler :
                    request.operationType == 'fetch' ? fetchOperationHandler : genericHandler
            try {
                retValue = handler.call(dataSource, request)
            } catch (Throwable t) {
                log.error(t.message, t)
                retValue = ['response': [status: -1, data: t.message]]
            }
        }
        retValue
    }


    protected def resolveDataSource(String name) {
        grailsApplication.mainContext.getBean("smartClient${name.capitalize()}DataSourceHandler")
    }

    protected def resolveService(String name) {
        grailsApplication.mainContext.getBean(name)
    }


    protected fetchOperationHandler = { dataSource, request ->
        def model = dataSource.fetch(request.data)
        return ['response':
                [
                        status: 0,
                        startRow: 0,
                        endRow: model.size(),
                        totalRows: model.size(),
                        data: model
                ]
        ]
    }


    protected genericHandler = { dataSource, request ->
        def value = dataSource.invokeMethod(request.operationType, [request.data] as Object[])
        return renderDataUpdateResponse(value)
    }

    protected customOperationHandler = { dataSource, request ->
        def value = dataSource.invokeMethod(request.operationId, [request.data] as Object[])
        return renderDataUpdateResponse([retValue: value])
    }

    protected remoteMethodHandler = { service, request ->
        Class clazz = AopProxyUtils.ultimateTargetClass(service)
        def paramsMeta = request.data.meta.collect { Class.forName(it) } as Class[]
        Method m = ReflectionUtils.findMethod(clazz, request.operationId, paramsMeta)
        if (m) {
            if (m.getAnnotation(Remote) || clazz.getAnnotation(Remote)) {
                def paramsValue = request.data.values.collect { it } as Object[]
                def value = service.invokeMethod(request.operationId, paramsValue)
                return renderDataUpdateResponse([retValue: value])
            } else {
                throw new RuntimeException("Method ${clazz.getName()}#${request.operationId} is not exposed for remote invocation")
            }
        } else {
            throw new RuntimeException("Method ${clazz.getName()}#${request.operationId} could not be found")
        }

    }


    protected def renderDataUpdateResponse(value) {
        if (value.errors?.hasErrors()) {
            def d = ['response': [status: -4]]
            def errors = [:]
            value.errors.allErrors.each { err ->
                if (!errors[err.field]) {
                    errors[err.field] = [
                            [errorMessage: messageSource.getMessage(err, null)]
                    ]
                } else {
                    errors[err.field] << [errorMessage: messageSource.getMessage(err, null)]
                }
            }
            d.response.errors = errors
            return d
        } else {
            return renderDataResponse(value)
        }
    }


    protected def renderDataResponse(value) {
        return ['response': [status: 0, data: value]]
    }

}
