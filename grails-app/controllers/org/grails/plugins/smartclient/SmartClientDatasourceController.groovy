package org.grails.plugins.smartclient

import grails.converters.JSON
import grails.util.GrailsClassUtils


/**
 * Controller responsible for json communication with SmartClient datasource instance
 *
 * @author Denis Halupa
 */
class SmartClientDatasourceController {


    def smartClientDataSourceDefinitionService
    def smartClientDataSourceHandlerExecutionService
    def nonTransactionalSmartClientDataSourceHandlerExecutionService

    /**
     * Entry point for all SmartClient datasource operations
     * @return
     */
    def serve() {
        def model
        if (request.JSON.transaction) {
            model = smartClientDataSourceHandlerExecutionService.executeTransaction(request.JSON.transaction)
        } else {
            model = resolveService(params.dsID, request.JSON).executeOperation(params.dsID, request.JSON)
        }
        def converterConfig = grailsApplication.config.grails.plugin.smartclient.converterConfig
        def jsonPrefix = smartClientDataSourceDefinitionService.jsonPrefix
        def jsonSufix = smartClientDataSourceDefinitionService.jsonSuffix
        try {
            if (converterConfig) {
                JSON.use(converterConfig, {
                    render(text: "${jsonPrefix}${model as JSON}${jsonSufix}".toString(), contentType: 'application/json')
                })
            } else {
                render(text: "${jsonPrefix}${model as JSON}${jsonSufix}".toString(), contentType: 'application/json')

            }
        } finally {
            def callback = grailsApplication.config.grails.plugin.smartclient.cleanupCallback
            if (callback) {
                callback.call()
            }
        }

    }

    /**
     * Outputs JavaScript required to construct SmartClient datasources
     * @return
     */
    def definitions() {
        render(text: smartClientDataSourceDefinitionService.getDefinitions(params.lang), contentType: 'application/javascript')
    }


    private def resolveService(dsID, req) {
        def dsName = dsID != null ? dsID : req.dataSource
        if (dsName.endsWith('Service')) {
            return nonTransactionalSmartClientDataSourceHandlerExecutionService
        } else {
            def dsHandler = grailsApplication.mainContext.getBean("smartClient${dsName.capitalize()}DataSourceHandler")
            def transactional = GrailsClassUtils.getStaticFieldValue(dsHandler.getClass(), 'transactional')
            if (transactional != null && !transactional) {
                return nonTransactionalSmartClientDataSourceHandlerExecutionService
            } else {
                return smartClientDataSourceHandlerExecutionService
            }

        }
    }


}