package org.grails.plugins.smartclient

import grails.converters.JSON
import grails.gorm.multitenancy.CurrentTenant
import grails.gorm.multitenancy.Tenants
import groovy.util.logging.Slf4j
import org.springframework.web.servlet.support.RequestContextUtils

@Slf4j
class RemoteMethodExecutorController {
    def remoteMethodExecutor
    def smartClientConfigProvider


    def index() {
        if (this.multiTenantMode) {
            Tenants.withCurrent {
                processIndexRequest()
            }
        } else {
            processIndexRequest()
        }
    }

    private def processIndexRequest() {
        def locale = RequestContextUtils.getLocale(request)
        def model
        def json = request.JSON
        log.debug('Received remote method request {}', json)
        if (request.JSON.transaction) {
            model = remoteMethodExecutor.executeTransaction(json.transaction, locale)
        } else {
            model = remoteMethodExecutor.execute(json, locale)
        }
        StringBuilder builder = new StringBuilder(smartClientConfigProvider.jsonPrefix)

        if (smartClientConfigProvider.converterConfig) {
            JSON.use(smartClientConfigProvider.converterConfig, {
                builder.append(new JSON(model).toString())
            })
        } else {
            builder.append(new JSON(model).toString())
            //  builder.append(smartClientConfigProvider.jsonSuffix)
        }
        builder.append(smartClientConfigProvider.jsonSuffix)
        render(text: builder.toString(), contentType: 'application/json')
    }

    def init() {
        render(view: 'js', model: [jsonPrefix: smartClientConfigProvider.jsonPrefix, jsonSufix: smartClientConfigProvider.jsonSuffix], contentType: 'application/javascript')
    }

    private boolean isMultiTenantMode() {
        return grailsApplication.config.getProperty('rf.multiTenancy.mode', Boolean, false)
    }


}
