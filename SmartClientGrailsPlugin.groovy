import org.codehaus.groovy.grails.commons.GrailsClass
import org.grails.plugins.smartclient.DataSourceHandlerArtefactHandler

class SmartClientGrailsPlugin {
    def packaging = "binary"
    def version = "0.2"
    def grailsVersion = "1.3.7 > *"
    def dependsOn = [:]
    def observe = ["services"]
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/domain/**"
    ]

    def title = "SmartClient Plugin" // Headline display name of the plugin
    def author = "Denis Halupa"
    def authorEmail = "denis.halupa@gmail.com"
    def description = '''\
This is the plugin which supports operation of smartclient datasources
'''

    def artefacts = [DataSourceHandlerArtefactHandler]
    // watch for any changes in these directories
    def watchedResources = [
            "file:./grails-app/dataSourceHandlers/**/*DataSourceHandler.groovy",
            "file:../../plugins/*/dataSourceHandlers/**/*DataSourceHandler.groovy"
    ]


    def onChange = { event ->
        application.mainContext.smartClientDataSourceDefinitionService.resetDefinitions()
        if (application.isArtefactOfType(DataSourceHandlerArtefactHandler.TYPE, event.source)) {
            def oldClass = application.getDataSourceHandlerClass(event.source.name)
            application.addArtefact(DataSourceHandlerArtefactHandler.TYPE, event.source)

            // Reload subclasses
            application.dataSourceHandlerClasses.each {
                if (it.clazz != event.source && oldClass.clazz.isAssignableFrom(it.clazz)) {
                    def newClass = application.classLoader.reloadClass(it.clazz.name)
                    application.addArtefact(DataSourceHandlerArtefactHandler.TYPE, newClass)
                }
            }
        }
        writeRemoteApi(application.mainContext)
    }

    def documentation = "http://grails.org/plugin/smartclient"

    def doWithSpring = {
        application.dataSourceHandlerClasses.each { GrailsClass dataSourceHandlerClass ->
            "smartClient${dataSourceHandlerClass.shortName}"(dataSourceHandlerClass.clazz) { bean ->
                bean.autowire = "byName"
            }
        }
    }

    def doWithDynamicMethods = { applicationContext ->
  //      writeRemoteApi(application)
    }


    private def writeRemoteApi(application) {
        String fileName = application.config.grails.plugin.smartclient.remoteApiFile ?: 'web-app/js/sc-remote-api.js'
        def remoteApi = new File(fileName)
        def w = remoteApi.newWriter()
        w << application.mainContext.smartClientDataSourceDefinitionService.remoteApi
        w.close()
    }


}
