package org.grails.plugins.smartclient

import grails.converters.JSON
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsServiceClass
import org.grails.plugins.smartclient.annotation.Progress
import org.grails.plugins.smartclient.annotation.Remote
import org.grails.plugins.smartclient.builder.FieldsDefinitionBuilder
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.springframework.context.NoSuchMessageException

import java.beans.Introspector

/**
 * Service responsible to build definition of datasources from registered datasource artefacts and exposed service methods
 * @author Denis Halupa
 */
class SmartClientDataSourceDefinitionService {

    static jsonPrefixString = "<SCRIPT>//'\"]]>>isc_JSONResponseStart>>"
    static jsonSuffixString = "//isc_JSONResponseEnd"


    private Map cachedDefinitions = [:]

    static transactional = false

    def grailsApplication
    def messageSource

    static def SYSTEM_PROPS = ['version', 'created']
    static def REMOTE_DEF = '''
isc.defineClass('RemoteMethod').addClassProperties({invoke: function (method, data, callback)
 {var parts = method.split('.');
 if (callback)
 {isc.DataSource.get(parts[0]).performCustomOperation(parts[1], data, function () {var data = arguments[1][0].retValue;
 for (var p in data) {if (data[p] === void 0) {delete data[p];}}callback.call(this, data);})
 } else {isc.DataSource.get(parts[0]).performCustomOperation(parts[1], data);}}
});'''

    static
    def TYPE_MAPPING = ['string': 'text', 'long': 'integer', 'boolean': 'boolean', 'integer': 'integer', 'date': 'date', 'float': 'float', 'double': 'float']

    def resetDefinitions() {
        cachedDefinitions = [:]
    }

    def getDefinitions(lang) {
        if (!cachedDefinitions[lang]) {
            def d = grailsApplication.dataSourceHandlerClasses.findAll { ds -> ds.name }.collect { dataSourceHandlerClass ->
                buildDataSourceDefinition(dataSourceHandlerClass, lang)
            }
            def remoteServices = grailsApplication.serviceClasses.findAll { GrailsServiceClass sc ->
                def clazz = sc.getClazz()
                def res = clazz.getAnnotation(Remote) != null || clazz.methods.find { it.getAnnotation(Remote) } != null
                res
            }
            d += remoteServices.collect { buildServiceDataSourceDefinition(it, lang) }
            StringBuilder b = new StringBuilder()
            d.each {
                String c = new JSON(it as Map).toString()
                b.append("isc.RestDataSource.create(${c});")
            }
            b.append(REMOTE_DEF.replace("\n", "").replace("\r", ""))
            cachedDefinitions[lang] = b.toString()
        }
        cachedDefinitions[lang]

    }

    def getJsonPrefix() {
        return grailsApplication.config.grails.plugin.smartclient.debug ? '' : jsonPrefixString
    }

    def getJsonSuffix() {
        return grailsApplication.config.grails.plugin.smartclient.debug ? '' : jsonSuffixString
    }

    private def buildDataSourceDefinition = { dsClass, lang ->
        def dsInfo = [dataURL: 'datasource', ID: dsClass.logicalPropertyName, dataFormat: 'json']
        def dsConfig = GCU.getStaticPropertyValue(dsClass.getClazz(), 'config')
        if (dsConfig) dsInfo << dsConfig
        dsInfo.fields = buildFieldsDefinition(dsClass)
        dsInfo.progressInfo = buildProgressInfo(dsClass.getClazz(), lang, dsInfo.ID)
        dsInfo.operationBindings = buildOperationsDefinition(dsClass, dsInfo)
        dsInfo.jsonPrefix = this.jsonPrefix
        dsInfo.jsonSufix = this.jsonSuffix
        dsInfo
    }

    private def buildServiceDataSourceDefinition = { serviceClass, lang ->
        def dsInfo = [dataURL: 'datasource', ID: "${serviceClass.logicalPropertyName}Service", dataFormat: 'json']
        dsInfo.progressInfo = buildProgressInfo(serviceClass.getClazz(), lang, dsInfo.ID)
        dsInfo.operationBindings = [buildOperationDefinition.call('custom', dsInfo)]
        dsInfo.jsonPrefix = this.jsonPrefix
        dsInfo.jsonSufix = this.jsonSuffix
        dsInfo
    }

    private def buildProgressInfo = { Class clazz, String lang, String dsID ->
        Locale locale = new Locale(lang)
        def args = new Object[0]
        clazz.declaredMethods.findAll { it.getAnnotation(Progress.class) } collectEntries {
            Progress ann = it.getAnnotation(Progress.class)
            String key = "${dsID}.${it.name}"
            [(key): messageSource.getMessage(ann.value(), args, locale)]
        }
    }

    private def buildOperationsDefinition = { dataSourceClass, dsInfo ->
        def operations = GCU.getStaticPropertyValue(dataSourceClass.getClazz(), 'operations')
        operations.collect {
            buildOperationDefinition.call(it, dsInfo)
        }
    }

    private def buildOperationDefinition = { operation, dsInfo ->
        def op = [operationType: operation, dataProtocol: "postMessage"]
        String key = "${dsInfo.ID}.${operation}"
        if (dsInfo.progressInfo[key]) {
            op.requestProperties = [prompt: dsInfo.progressInfo.remove(key)]
        }
        op
    }


    private def buildFieldsDefinition = { dsClass ->
        def fd = [:]
        Class clazz = dsClass.getClazz()
        def included = GCU.getStaticPropertyValue(clazz, 'included') ?: []
        def excluded = GCU.getStaticPropertyValue(clazz, 'excluded') ?: []
        GrailsDomainClass domainClass = resolveDomainClass(dsClass)
        if (domainClass) {
            if (included) {
                included.each { i ->
                    GrailsDomainClassProperty p = domainClass.getPropertyByName(i)
                    if (p) {
                        buildFieldDefinition(p, fd)
                    } else {
                        throw new RuntimeException("Included property '${i}' can not be found")
                    }
                }
            } else {
                domainClass.properties.each { GrailsDomainClassProperty p ->
                    if (!excluded.contains(p.name)) {
                        buildFieldDefinition(p, fd)
                    }
                }

            }
        }
        applyFieldsDefinition(dsClass, fd)
        fd.collect { k, v -> v }
    }

    private def buildFieldDefinition = { GrailsDomainClassProperty prop, Map fieldsDefinition ->
        if (prop.persistent && !prop.association && prop.typePropertyName != 'object') {
            if (!SYSTEM_PROPS.contains(prop.name)) {
                def defn = [name: prop.name]
                if (prop.identity) {
                    defn << [hidden: true, primaryKey: true]
                } else {
                    def title = resolveTitle(prop)
                    if (title) {
                        defn.title = title
                    }
                }
                defn.type = resolveType(prop)
                if (defn.type == 'any') {
                    defn.hidden = true
                    //it seems that fields of type 'any' have to be hidden, otherwise addData does not work
                }
                fieldsDefinition[prop.name] = defn
            }
        }
    }

    private def resolveTitle = { prop ->
        try {
            return messageSource.getMessage("smart.field.${prop.domainClass.logicalPropertyName}.${prop.name}.title".toString(), [] as Object[], null)
        } catch (NoSuchMessageException e) {
            return null
        }
    }

    private def resolveDomainClass = { dsClass ->
        Class clazz = dsClass.getClazz()
        String domainClassName = GCU.getStaticPropertyValue(clazz, 'domainClass')?.simpleName ?: dsClass.logicalPropertyName
        GrailsDomainClass domainClass = grailsApplication.getArtefactByLogicalPropertyName(DomainClassArtefactHandler.TYPE, Introspector.decapitalize(domainClassName));
        if (domainClass) {
            return domainClass
        } else {
            return null
        }

    }

    private def applyFieldsDefinition = { dsClass, Map fieldsDefinition ->
        def meta = GCU.getStaticPropertyValue(dsClass.clazz, 'fields')
        if (meta) {
            FieldsDefinitionBuilder builder = new FieldsDefinitionBuilder()
            meta.delegate = builder
            meta.resolveStrategy = Closure.DELEGATE_FIRST
            meta()
            builder.config.each { key, value ->
                if (fieldsDefinition[key]) {
                    fieldsDefinition[key] << value
                } else {
                    fieldsDefinition[key] = value
                }
            }
        }
    }

    private def resolveType = { GrailsDomainClassProperty prop ->
        def type = TYPE_MAPPING[prop.typePropertyName]
        if (type) {
            return type
        } else {
            return 'any'
        }

    }


}
