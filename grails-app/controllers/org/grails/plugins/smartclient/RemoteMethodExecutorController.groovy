package org.grails.plugins.smartclient

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.springframework.web.servlet.support.RequestContextUtils

@Slf4j
class RemoteMethodExecutorController {
    def remoteMethodExecutor
    def smartClientConfigProvider

    def index() {
        def locale = RequestContextUtils.getLocale(request)
        def model
        def json = request.JSON
        log.debug('Received remote method request {}', json)
        if (request.JSON.transaction) {
            model = remoteMethodExecutor.executeTransaction(json.transaction, locale)
        } else {
            model = remoteMethodExecutor.execute(json, locale)
        }

        if (smartClientConfigProvider.converterConfig) {
            JSON.use(smartClientConfigProvider.converterConfig, {
                render(text: "${smartClientConfigProvider.jsonPrefix}${model as JSON}${smartClientConfigProvider.jsonSuffix}".toString(), contentType: 'application/json')
            })
        } else {
            render(text: "${smartClientConfigProvider.jsonPrefix}${model as JSON}${smartClientConfigProvider.jsonSuffix}".toString(), contentType: 'application/json')
        }
    }

    def init() {
        render(view: 'js', model: [jsonPrefix: smartClientConfigProvider.jsonPrefix, jsonSufix: smartClientConfigProvider.jsonSuffix], contentType: 'application/javascript')
    }


}
