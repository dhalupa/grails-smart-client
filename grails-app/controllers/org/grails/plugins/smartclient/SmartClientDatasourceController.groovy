package org.grails.plugins.smartclient

import grails.converters.JSON

/**
 * Controller responsible for json communication with SmartClient datasource instance
 *
 * @author Denis Halupa
 */
class SmartClientDatasourceController {


    def smartClientDataSourceDefinitionService
    def smartClientDataSourceHandlerExecutionService

    /**
     * Entry point for all SmartClient datasource operations
     * @return
     */
    def serve() {
        def model
        if (request.JSON.transaction) {
            model = smartClientDataSourceHandlerExecutionService.executeTransaction(request.JSON.transaction)
        } else {
            model = smartClientDataSourceHandlerExecutionService.executeOperation(params.dsID, request.JSON)
        }
        def converterConfig = grailsApplication.config.grails.plugin.smartclient.converterConfig
        def jsonPrefix = smartClientDataSourceDefinitionService.jsonPrefix
        def jsonSufix = smartClientDataSourceDefinitionService.jsonSuffix
        if (converterConfig) {
            JSON.use(converterConfig, {
                render(text: "${jsonPrefix}${model as JSON}${jsonSufix}", contentType: 'application/json')
            })
        } else {
            render(text: "${jsonPrefix}${model as JSON}${jsonSufix}", contentType: 'application/json')

        }

    }

    /**
     * Outputs JavaScript required to construct SmartClient datasources
     * @return
     */
    def definitions() {
        render(text: smartClientDataSourceDefinitionService.getDefinitions(params.lang), contentType: 'application/javascript')
    }


}
