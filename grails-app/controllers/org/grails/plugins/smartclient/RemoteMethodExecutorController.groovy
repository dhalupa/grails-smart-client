package org.grails.plugins.smartclient

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.springframework.web.servlet.support.RequestContextUtils

@Slf4j
class RemoteMethodExecutorController {
    def remoteMethodExecutor
    def configProvider

    def index() {

        def locale = RequestContextUtils.getLocale(request)

        def model
        def json = request.JSON
        log.debug('Received remote method request {}', json)
        String conversationId = json.data.remove('__conversationId')
        ConversationScope.CURRENT_CONVERSATION.set(conversationId)
        if (request.JSON.transaction) {
            model = remoteMethodExecutor.executeTransaction(json.transaction, locale)
        } else {
            model = remoteMethodExecutor.execute(json, locale)
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
