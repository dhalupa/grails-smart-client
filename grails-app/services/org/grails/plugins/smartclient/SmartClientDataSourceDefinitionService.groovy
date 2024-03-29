package org.grails.plugins.smartclient

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.core.GrailsServiceClass
import grails.util.GrailsClassUtils
import groovy.text.SimpleTemplateEngine
import org.grails.core.artefact.ServiceArtefactHandler
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Identity
import org.grails.io.support.ClassPathResource
import org.grails.plugins.smartclient.annotation.P
import org.grails.plugins.smartclient.annotation.Progress
import org.grails.plugins.smartclient.annotation.Remote
import org.grails.plugins.smartclient.builder.FieldsDefinitionBuilder
import org.grails.plugins.smartclient.marshall.RawJavascript
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException

import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.annotation.Annotation
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Service responsible to build definition of datasources and remote API from registered datasource artefacts
 * and exposed service methods
 * @author Denis Halupa
 */
class SmartClientDataSourceDefinitionService {

    static jsonPrefixString = "<SCRIPT>//'\"]]>>isc_JSONResponseStart>>"
    static jsonSuffixString = "//isc_JSONResponseEnd"


    private Map cachedDefinitions = [:]

    static transactional = false

    GrailsApplication grailsApplication
    MessageSource messageSource


    static def SYSTEM_PROPS = ['version', 'created']
    static def REMOTE_DEF = '''isc.defineClass('RemoteMethod').addClassProperties({
    invoke: function (method, data, callback) {
        var parts = method.split('.');
        var ds = isc.DataSource.get(parts[0]);
        if (ds) {
            if (callback) {
                ds.performCustomOperation(parts[1], data, function () {
                    var data = arguments[1][0].retValue;
                    for (var p in data) {
                        if (data[p] === void 0) { delete data[p];}
                    }
                    callback.call(this, data);
                })
            } else {
                ds.performCustomOperation(parts[1], data);
            }
            } else {
                alert('DataSource ' + parts[0] + ' can not be found!')}
            }
        });'''

    private static def remoteApiTemplateText = '''
var rmt=new function(){
    return {
        $services
    }
}();'''

    private static def serviceTemplateText = '''
        ${serviceName}: {
            ${functions}
        }'''

    private static def functionTemplateText = '''
            $functionName: function ($params , callback) {
                var params = {values: [$params],meta: [$paramsMeta]};
                isc.RemoteMethod.invoke('${dataSourceName}.${functionName}', params, function (data) {callback.call(this, data)})
            }
'''
    private static
    def errorFunctionTemplateText = '''
$functionName: function ($params , callback) { alert('${message}')}'''

    static
    def TYPE_MAPPING = ['string': 'text', 'long': 'integer', 'boolean': 'boolean', 'integer': 'integer', 'date': 'date', 'float': 'float', 'double': 'float']


    def resetDefinitions() {
        cachedDefinitions = [:]
    }

    def getDefinitions(lang) {
        if (!cachedDefinitions[lang]) {
            def d = grailsApplication.getArtefacts(DataSourceHandlerArtefactHandler.TYPE).findAll { ds -> ds.name }.collect { dataSourceHandlerClass ->
                def doNotGenerate = GrailsClassUtils.getStaticPropertyValue(dataSourceHandlerClass.getClazz(), 'doNotGenerateDataSource')
                if (!doNotGenerate) {
                    buildDataSourceDefinition(dataSourceHandlerClass, lang)
                }
            }.findAll { it }
            StringBuilder b = new StringBuilder()
            d.each {
                String c = new JSON(it as Map).toString()
                b.append("isc.RestDataSource.create(${c});")
            }
            ClassPathResource res = new ClassPathResource('js/DeprecatedRemoteMethodExecutor.js')
            b.append(res.inputStream.getText('UTF-8'))
            cachedDefinitions[lang] = b.toString()
        }
        cachedDefinitions[lang]

    }

    def getJsonPrefix() {
        return grailsApplication.config.getProperty('grails.plugin.smartclient.debug', Boolean) ? '' : jsonPrefixString
    }

    def getJsonSuffix() {
        return grailsApplication.config.getProperty('grails.plugin.smartclient.debug', Boolean) ? '' : jsonSuffixString
    }


    def getRemoteApi() {
        def engine = new SimpleTemplateEngine()
        def remoteApiTemplate = engine.createTemplate(remoteApiTemplateText)
        def serviceTemplate = engine.createTemplate(serviceTemplateText)
        def functionTemplate = engine.createTemplate(functionTemplateText)
        def errorFunctionTemplate = engine.createTemplate(errorFunctionTemplateText)
        def bindingModel = [:]
        def servicesDefinitions = []
        grailsApplication.getArtefacts(ServiceArtefactHandler.TYPE).each { GrailsServiceClass sc ->
            boolean remoteService = sc.clazz.getAnnotation(Remote.class)
            def methods
            if (remoteService) {
                methods = extractUserMethods(sc.clazz)
            } else {
                methods = sc.clazz.methods.findAll { it.getAnnotation(Remote.class) != null }
            }
            if (!methods.isEmpty()) {
                bindingModel.dataSourceName = "${sc.logicalPropertyName}Service"
                bindingModel.serviceName = "${sc.name}Service"
                def functionsDefinitions = []
                methods.each { Method m ->
                    bindingModel.functionName = m.name
                    int index = 1
                    def paramNames = m.parameterAnnotations.collect { Annotation[] anns ->
                        String paramName = anns.find { Annotation a -> a instanceof P }?.value()
                        paramName ?: "param${index++}"
                    }
                    bindingModel.params = paramNames.join(',')
                    bindingModel.paramsMeta = m.parameterTypes.collect { Class c ->
                        if (c.primitive) {
                            bindingModel.message = "Primitive parameters are not supported. Please correct ${bindingModel.serviceName}.${bindingModel.functionName}"
                        }
                        "'${c.name}'"
                    }.join(',')
                    if (bindingModel.message) {
                        functionsDefinitions << errorFunctionTemplate.make(bindingModel).toString()
                        bindingModel.remove('message')
                    } else {
                        functionsDefinitions << functionTemplate.make(bindingModel).toString()
                    }

                }
                bindingModel.functions = functionsDefinitions.join(',')
                def srv = serviceTemplate.make(bindingModel).toString()
                servicesDefinitions << srv
            }
        }
        bindingModel.services = servicesDefinitions.join(',')
        def builder = new StringBuilder(REMOTE_DEF)
        builder.append(remoteApiTemplate.make(bindingModel).toString())
        builder.toString()

    }

    private def buildDataSourceDefinition = { dsClass, lang ->
        def dsInfo = [dataURL: 'datasource', ID: dsClass.logicalPropertyName, dataFormat: 'json']
        def dsConfig = GrailsClassUtils.getStaticPropertyValue(dsClass.getClazz(), 'config')
        if (dsConfig) dsInfo << dsConfig
        dsInfo.fields = buildFieldsDefinition(dsClass)
        dsInfo.progressInfo = buildProgressInfo(dsClass.getClazz(), lang, dsInfo.ID)
        dsInfo.operationBindings = buildOperationsDefinition(dsClass, dsInfo)
        //    dsInfo.transformRequest = buildTransformRequest()
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
        def operations = GrailsClassUtils.getStaticPropertyValue(dataSourceClass.getClazz(), 'operations')
        operations.collect {
            buildOperationDefinition.call(it, dsInfo)
        }
    }

    private def buildTransformRequest() {
        new RawJavascript('''function (dsRequest) {
        dsRequest.httpHeaders = dsRequest.httpHeaders || {};
        if(window._scCsrfToken) dsRequest.httpHeaders['Csrf-Token']=window._scCsrfToken;
        return this.Super("transformRequest", arguments);
    }''')
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
        List included = GrailsClassUtils.getStaticPropertyValue(clazz, 'included') ?: []
        List excluded = GrailsClassUtils.getStaticPropertyValue(clazz, 'excluded') ?: []
        PersistentEntity entity = grailsApplication.mappingContext.getPersistentEntity(clazz.name)

        if (entity) {
            if (included) {
                included.each { i ->
                    PersistentProperty p = entity.getPropertyByName(i)
                    if (p) {
                        buildFieldDefinition(p, fd)
                    } else {
                        throw new RuntimeException("Included property '${i}' can not be found")
                    }
                }
            } else {
                entity.persistentProperties.each { PersistentProperty p ->
                    if (!excluded.contains(p.name)) {
                        buildFieldDefinition(p, fd)
                    }
                }

            }
        }
        applyFieldsDefinition(dsClass, fd)
        fd.collect { k, v -> v }
    }

    private def buildFieldDefinition = { PersistentProperty prop, Map fieldsDefinition ->
        if (!prop instanceof Association && prop.type != Object.class) {
            if (!SYSTEM_PROPS.contains(prop.name)) {
                Map defn = [name: prop.name]
                if (prop instanceof Identity) {
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

    private def resolveTitle = { PersistentProperty prop ->
        try {
            return messageSource.getMessage("smart.field.${prop.mapping.classMapping.entity.name}.${prop.name}.title".toString(), [] as Object[], null)
        } catch (NoSuchMessageException e) {
            return null
        }
    }

    private def resolveDomainClass = { dsClass ->
        Class clazz = dsClass.getClazz()

        String domainClassName = GrailsClassUtils.getStaticPropertyValue(clazz, 'domainClass')?.simpleName ?: dsClass.logicalPropertyName
        PersistentEntity entity = grailsApplication.mappingContext.getPersistentEntity(domainClassName)
//        GrailsDomainClass domainClass = grailsApplication.getArtefactByLogicalPropertyName(DomainClassArtefactHandler.TYPE, Introspector.decapitalize(domainClassName));
        if (entity) {
            return entity
        } else {
            return null
        }

    }

    private def applyFieldsDefinition = { dsClass, Map fieldsDefinition ->
        def meta = GrailsClassUtils.getStaticPropertyValue(dsClass.clazz, 'fields')
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

    private def resolveType = { PersistentProperty prop ->
        def type = TYPE_MAPPING[prop.name]
        if (type) {
            return type
        } else {
            return 'any'
        }

    }

    //copied from gwt plugin
    private def extractUserMethods(Class serviceClass) {
        def methods = []
        // Find out what properties the service class contains, because
        // we want to leave them out of the interface definition.
        def info = Introspector.getBeanInfo(serviceClass)
        def propMethods = [] as Set
        info.propertyDescriptors.each { PropertyDescriptor desc ->
            propMethods << desc.readMethod
            propMethods << desc.writeMethod

            // Groovy adds a "get*()" method for booleans as well as
            // the usual "is*()", so we have to remove it too.
            if (desc.readMethod?.name?.startsWith("is")) {
                def name = "get${desc.readMethod.name[2..-1]}".toString()
                def getMethod = info.methodDescriptors.find { it.name == name }
                if (getMethod) {
                    propMethods << getMethod.method
                }
            }
        }

        // Iterate through the methods declared by the Grails service,
        // adding the appropriate ones to the interface definitions.
        serviceClass.declaredMethods.each { Method method ->
            // Skip non-public, static, Groovy, and property methods.
            if (method.synthetic ||
                    !Modifier.isPublic(method.modifiers) ||
                    Modifier.isStatic(method.modifiers) ||
                    propMethods.contains(method)) {
                return
            }
            methods << method
        }
        methods
    }


}
