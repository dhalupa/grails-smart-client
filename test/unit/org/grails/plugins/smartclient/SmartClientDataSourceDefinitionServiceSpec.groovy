package org.grails.plugins.smartclient

import grails.converters.JSON
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.web.converters.marshaller.json.CollectionMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.json.MapMarshaller
import org.springframework.context.MessageSource
import spock.lang.Specification


@TestFor(SmartClientDataSourceDefinitionService)
@Mock([Invoice])
class SmartClientDataSourceDefinitionServiceSpec extends Specification {

    def setup() {

        grailsApplication.registerArtefactHandler(new DataSourceHandlerArtefactHandler())
        grailsApplication.addArtefact(InvoiceDataSourceHandler)
        grailsApplication.addArtefact(InvoiceService)

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> { "${it[0]}_${it[2]}" }
        }
        defineBeans {
            'smartClientInvoiceDataSourceHandler'(InvoiceDataSourceHandler)
        }
        JSON.withDefaultConfiguration {
            it.registerObjectMarshaller(new MapMarshaller())
            it.registerObjectMarshaller(new CollectionMarshaller())
        }

    }

    def cleanup() {
    }


    void "test operations building"() {
        given:
        def d = grailsApplication.dataSourceHandlerClasses[0]
        def ds = [ID: 'invoice', progressInfo: service.buildProgressInfo(d.getClazz(), 'en', 'invoice')]
        when:
        def s = service.buildOperationsDefinition(d, ds)
        then:
        s == [[operationType: 'fetch', dataProtocol: 'postMessage'], [operationType: 'remove', dataProtocol: 'postMessage'], [operationType: 'update', dataProtocol: 'postMessage', requestProperties: [prompt: 'org.grails.plugins.smartclient.progress_en']]]

    }

    void "test progress info building"() {
        given:
        def d = grailsApplication.dataSourceHandlerClasses[0]
        when:
        def s = service.buildProgressInfo(d.getClazz(), 'en', 'invoice')
        then:
        s == ['invoice.custom': 'some.key_en', 'invoice.update': 'org.grails.plugins.smartclient.progress_en']

    }

    void "javascript which creates datasource should be created"() {
        given:
        def d = grailsApplication.dataSourceHandlerClasses[0]
        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> { args -> args[0] }
        }
        when:
        def s = service.getDefinitions('en')
        then:
        println s
        s == '''isc.RestDataSource.create({"dataURL":"datasource","ID":"invoice","dataFormat":"json","fields":[{"name":"admin","title":"smart.field.invoice.admin.title","type":"boolean"},{"name":"id","hidden":true,"primaryKey":true,"type":"integer"},{"name":"name","title":"Invoice","type":"text","width":300,"validators":[{"type":"floatRange","min":0,"errorMessage":"loc_Please enter a valid (positive) cost"}]}],"progressInfo":{"invoice.custom":"some.key"},"operationBindings":[{"operationType":"fetch","dataProtocol":"postMessage"},{"operationType":"remove","dataProtocol":"postMessage"},{"operationType":"update","dataProtocol":"postMessage","requestProperties":{"prompt":"org.grails.plugins.smartclient.progress"}}],"jsonPrefix":"<SCRIPT>//'\\"]]>>isc_JSONResponseStart>>","jsonSufix":"//isc_JSONResponseEnd"});isc.RestDataSource.create({"dataURL":"datasource","ID":"invoiceService","dataFormat":"json","progressInfo":{},"operationBindings":[{"operationType":"custom","dataProtocol":"postMessage"}],"jsonPrefix":"<SCRIPT>//'\\"]]>>isc_JSONResponseStart>>","jsonSufix":"//isc_JSONResponseEnd"});isc.defineClass('RemoteMethod').addClassProperties({invoke: function (method, data, callback) {var parts = method.split('.'); if (callback) {isc.DataSource.get(parts[0]).performCustomOperation(parts[1], data, function () {var data = arguments[1][0].retValue; for (var p in data) {if (data[p] === void 0) {delete data[p];}}callback.call(this, data);}) } else {isc.DataSource.get(parts[0]).performCustomOperation(parts[1], data);}}});'''

    }

    def "test remote api building"() {
        when:
        def s = service.getRemoteApi()
        then:
        println s

    }
}
