package org.grails.plugins.smartclient

import grails.converters.JSON
import org.springframework.web.servlet.support.RequestContextUtils

class RemoteMethodExecutorController {
    def remoteMethodExecutor
    def configProvider

    def index() {
        def locale = RequestContextUtils.getLocale(request)
        def model
        if (request.JSON.transaction) {
            model = remoteMethodExecutor.executeTransaction(request.JSON.transaction, locale)
        } else {
            model = remoteMethodExecutor.execute(request.JSON.data, locale)
        }

        if (configProvider.converterConfig) {
            JSON.use(configProvider.converterConfig, {
                render(text: "${configProvider.jsonPrefix}${model as JSON}${configProvider.jsonSuffix}", contentType: 'application/json')
            })
        } else {
            render(text: "${configProvider.jsonPrefix}${model as JSON}${configProvider.jsonSuffix}", contentType: 'application/json')
        }
    }

    def init() {
        render(view: 'js', model: [jsonPrefix: configProvider.jsonPrefix, jsonSufix: configProvider.jsonSuffix], contentType: 'application/javascript')
    }


}
