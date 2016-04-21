package org.grails.plugins.smartclient

import grails.converters.JSON
import grails.smart.client.DataSourceHandlerArtefactHandler
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by dhalupa on 4/16/14.
 */

@TestFor(org.grails.plugins.smartclient.SmartClientDataSourceHandlerExecutionService)
@Mock([Invoice])
class SmartClientDataSourceHandlerExecutionServiceSpec extends Specification {

    def invoice


    def setup() {
        grailsApplication.registerArtefactHandler(new DataSourceHandlerArtefactHandler())
        grailsApplication.addArtefact(InvoiceDataSourceHandler)
        grailsApplication.addArtefact(ExposedInvoiceService)
        grailsApplication.addArtefact(InvoiceService)

        defineBeans {
            'smartClientInvoiceDataSourceHandler'(InvoiceDataSourceHandler)
            'invoiceService'(InvoiceService)
            'exposedInvoiceService'(ExposedInvoiceService)
        }
        invoice = new Invoice(name: 'Some name', admin: true, created: new Date()).save()
        JSON.withDefaultConfiguration {
            it.registerObjectMarshaller(new MapMarshaller())
            it.registerObjectMarshaller(new CollectionMarshaller())
        }


    }

    def "should be able to resolve datasource"() {
        when:
        def s = service.resolveDataSource('invoice')
        then:
        s != null

    }


    def "should be able to invoke fetch operation"() {
        given:
        def request = JSON.parse('{ "dataSource":"invoice", "operationType":"fetch", "startRow":0, "endRow":75, "textMatchStyle":"substring", "componentId":"isc_ListGrid_0", "data":{ "some":"criteria" }, "oldValues":null }')
        when:
        def ret = service.executeOperation(null, request)
        then:
        ret.response
        ret.response.status == 0
        ret.response.startRow == 0
        ret.response.endRow == 1
        ret.response.totalRows == 1
        ret.response.data.size() == 1
        ret.response.data[0] == invoice
    }

    @Unroll
    def "should be able to invoke #operation operation"() {
        given:
        def request = JSON.parse("""{ "dataSource":"invoice", "operationType":"${
            operation
        }", "data":{ "some":"data" } }""")
        when:
        def ret = service.executeOperation(null, request)
        then:
        ret == [response: [status: 0, data: [exec: operation]]]
        where:
        operation << ['add', 'remove', 'update']
    }

    def "should be able to invoke custom operation"() {
        given:
        def request = JSON.parse("""{ "dataSource":"invoice", "operationType":"custom",operationId:"myMethod", "data":{ "some":"data" } }""")
        when:
        def ret = service.executeOperation(null, request)
        then:
        ret == [response: [status: 0, data: [retValue: [exec: 'custom']]]]
    }

    def "should be able to invoke method from exposed class"() {
        given:
        def request = JSON.parse("""{ "dataSource":"invoiceService", "operationType":"custom",operationId:"doSomething", "data":{meta:['java.lang.String','java.lang.Integer','java.lang.Object'],values:["data",1,5] } }""")
        when:
        def ret = service.executeOperation(null, request)
        then:
        ret == [response: [status: 0, data: [retValue: [some: 'value']]]]
    }

    def "should be forbiden to invoke not exposed service method"() {
        given:
        def request = JSON.parse("""{ "dataSource":"invoiceService", "operationType":"custom",operationId:"notExposed", "data":{meta:['java.lang.Object'],values:["data"] } }""")
        when:
        def ret = service.executeOperation(null, request)
        then:
        ret == [response: [status: -1, data: 'Method org.grails.plugins.smartclient.InvoiceService#notExposed is not exposed for remote invocation']]
    }

    def "error response should be returned if method does not exists"() {
        given:
        def request = JSON.parse("""{ "dataSource":"invoiceService", "operationType":"custom",operationId:"foo", "data":{meta:['java.lang.Object'],values:["data"] } }""")
        when:
        def ret = service.executeOperation(null, request)
        then:
        ret == [response: [status: -1, data: 'Method org.grails.plugins.smartclient.InvoiceService#foo could not be found']]
    }


}
