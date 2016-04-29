package org.grails.plugins.smartclient

import grails.plugins.*

class GrailsSmartClientGrailsPlugin extends Plugin {


    def grailsVersion = "3.0.16 > *"
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]


    def title = "Grails Smart Client" // Headline display name of the plugin
    def author = "Denis Halupa"
    def authorEmail = "denis.halupa@gmail.com"
    def description = '''\
This plugin enables easy client server integration when SmartClient JS library is used
'''
    def profiles = ['web']
    def documentation = "http://grails.org/plugin/grails-smart-client"
    def license = "APACHE"
    def scm = [url: "https://github.com/dhalupa/grails-smart-client"]

    Closure doWithSpring() {
        { ->
            remoteMethodExecutor(RemoteMethodExecutor) { bean ->
                bean.autowire = 'byName'
            }
            supportingJavaScriptProvider(RemoteApiJavaScriptProvider) { bean ->
                bean.autowire = 'byName'
            }
            configProvider(ConfigProvider) { bean ->
                bean.autowire = 'byName'
            }
        }
    }


}
