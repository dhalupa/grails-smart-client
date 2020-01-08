package org.grails.plugins.smartclient

/**
 * Created by dhalupa on 29.04.16..
 */
class ConfigProvider {
    def grailsApplication

    String getRemoteApiFileName() {
        grailsApplication.config.grails.plugin.smartclient.remoteApiFileName ?: 'grails-app/assets/javascripts/RemoteApi.js'
    }

    String getSmartMvcFileName() {
        grailsApplication.config.grails.plugin.smartclient.smartMvcFileName ?: 'grails-app/assets/javascripts/SmartMvc.js'
    }

    def getConverterConfig() {
        grailsApplication.config.grails.plugin.smartclient.converterConfig ?: 'smart'
    }

    String getJsonPrefix() {
        return grailsApplication.config.grails.plugin.smartclient.debug ? '' : "<SCRIPT>//'\"]]>>isc_JSONResponseStart>>"
    }

    String getJsonSuffix() {
        return grailsApplication.config.grails.plugin.smartclient.debug ? '' : "//isc_JSONResponseEnd"
    }
}
