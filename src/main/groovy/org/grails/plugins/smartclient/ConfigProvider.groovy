package org.grails.plugins.smartclient

import grails.core.GrailsApplication

/**
 * Created by dhalupa on 29.04.16..
 */
class ConfigProvider {
    GrailsApplication grailsApplication

    String getRemoteApiFileName() {
        grailsApplication.config.getProperty('grails.plugin.smartclient.remoteApiFileName', 'grails-app/assets/javascripts/RemoteApi.js')
    }

    String getSmartMvcFileName() {
        grailsApplication.config.getProperty('grails.plugin.smartclient.smartMvcFileName', 'grails-app/assets/javascripts/SmartMvc.js')
    }

    def getConverterConfig() {
        grailsApplication.config.getProperty('grails.plugin.smartclient.converterConfig', 'smart')
    }

    String getJsonPrefix() {
        return grailsApplication.config.getProperty('grails.plugin.smartclient.debug', Boolean) ? '' : "<SCRIPT>//'\"]]>>isc_JSONResponseStart>>"
    }

    String getJsonSuffix() {
        return grailsApplication.config.getProperty('grails.plugin.smartclient.debug', Boolean) ? '' : "//isc_JSONResponseEnd"
    }
}
