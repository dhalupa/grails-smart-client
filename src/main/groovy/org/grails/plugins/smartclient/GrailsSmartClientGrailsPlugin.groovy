package org.grails.plugins.smartclient

import grails.converters.JSON
import grails.core.ArtefactHandler
import grails.core.GrailsClass
import grails.plugins.Plugin
import org.apache.commons.lang3.StringUtils
import org.grails.plugins.smartclient.marshall.*
import org.springframework.beans.factory.config.CustomScopeConfigurer

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

    List<ArtefactHandler> artefacts = [DataSourceHandlerArtefactHandler]

    def watchedResources = "file:./grails-app/dataSourceHandlers/**/*DataSourceHandler.groovy"


    Closure doWithSpring() {

        { ->
            csrfTokenHandlerBean(CsrfTokenHandler) { bean ->
                bean.scope = 'session'
            }
            csrfTokenHandler(org.springframework.aop.scope.ScopedProxyFactoryBean) {
                targetBeanName = 'csrfTokenHandlerBean'
                proxyTargetClass = true
            }

            remoteMethodExecutor(RemoteMethodExecutor) { bean ->
                bean.autowire = 'byName'
            }
            supportingJavaScriptProvider(RemoteApiJavaScriptProvider) { bean ->
                bean.autowire = 'byName'
            }
            smartClientConfigProvider(ConfigProvider) { bean ->
                bean.autowire = 'byName'
            }
            conversationScope(ConversationScope) {

            }

            conversationScopeConfigurer(CustomScopeConfigurer) {
                scopes = [conversation: ref("conversationScope")]
            }

            def marshallers = [AsyncRemoteMethodResponseSmartJsonMarshaller, FetchResponseSmartJsonMarshaller, RawJavascriptMarshaller,
                               RemoteMethodResponseSmartJsonMarshaller, UpdateResponseSmartJsonMarshaller]
            marshallers.each { clazz ->
                "${StringUtils.uncapitalize(clazz.simpleName)}"(clazz) { bean ->
                    bean.autowire = "byName"
                }
            }

            grailsApplication.getArtefacts('DataSourceHandler').each { GrailsClass dataSourceHandlerClass ->
                "smartClient${dataSourceHandlerClass.shortName}"(dataSourceHandlerClass.clazz) { bean ->
                    bean.autowire = "byName"
                }
            }
        }
    }

    @Override
    void doWithApplicationContext() {

        JSON.use('smart') {
            applicationContext.getBeanNamesForType(SmartJsonMarshaller).each {
                JSON.registerObjectMarshaller(applicationContext.getBean(it))
            }
        }


    }
}
