package org.grails.plugins.smartclient

import grails.converters.JSON
import grails.test.mixin.TestFor
import org.grails.plugins.smartclient.annotation.Operation
import org.grails.plugins.smartclient.annotation.P
import org.grails.plugins.smartclient.annotation.Remote
import spock.lang.IgnoreRest
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(RemoteMethodExecutorController)
class RemoteMethodExecutorControllerSpec extends Specification {

    def setup() {
        defineBeans {
            'exposedInvoiceService'(ExposedInvoiceService)
            'invoiceService'(InvoiceService)
        }
        ConfigProvider configProvider = Mock() {
            getJsonPrefix() >> ''
            getJsonSuffix() >> ''
            getConverterConfig() >> null
        }
        controller.configProvider = configProvider
        controller.remoteMethodExecutor = new RemoteMethodExecutor(grailsApplication: grailsApplication)
    }


    def "it should be possible to invoke exposed remote method"() {
        given:
        request.json = '{ "data":{__method:"InvoiceService.doSomething",value:1 }, "oldValues":null }'
        when:
        controller.index()
        def resp = JSON.parse(response.contentAsString)
        then:
        resp.response.status == 0
        resp.response.data.some == 2
    }

    def "if service method not exist error response should be returned"() {
        given:
        request.json = '{ "data":{__method:"InvoiceServiceN.doSomething",value:1 }, "oldValues":null }'
        when:
        controller.index()
        def resp = JSON.parse(response.contentAsString)
        then:
        resp.response.status == -1
        resp.response.data == 'No bean named \'invoiceServiceN\' is defined'
    }

    def "if service method does not exist error response should be returned"() {
        given:
        request.json = '{ "data":{__method:"InvoiceService.doSomethingN",value:1 }, "oldValues":null }'
        when:
        controller.index()
        def resp = JSON.parse(response.contentAsString)
        then:
        resp.response.status == -1
        resp.response.data == 'Method org.grails.plugins.smartclient.InvoiceService#doSomethingN could not be found'
    }

    def "if service method is not exposed error response should be returned"() {
        given:
        request.json = '{ "data":{__method:"InvoiceService.notExposed",value:1 }, "oldValues":null }'
        when:
        controller.index()
        def resp = JSON.parse(response.contentAsString)
        then:
        println response.contentAsString
        resp.response.status == -1
        resp.response.data == 'Method org.grails.plugins.smartclient.InvoiceService#notExposed is not exposed for remote invocation'
    }

    def "it should be possible to invoke method on the exposed class"() {
        given:
        request.json = '{ "data":{__method:"ExposedInvoiceService.doSomething",value:1 }, "oldValues":null }'
        when:
        controller.index()
        def resp = JSON.parse(response.contentAsString)
        then:
        println response.contentAsString
        resp.response.status == 0
        resp.response.data.some1 == 2
    }

    def "with transactional request, multiple exposed methods should invoked"() {
        given:
        request.json = """{ transaction: { transactionNum: 2, operations: [{ "data":{__method:"ExposedInvoiceService.doSomething",value:2 } }, {  "data":{__method:"InvoiceService.doSomething",value:3 } }]}}"""
        when:
        controller.index()
        def resp = JSON.parse(response.contentAsString)
        then:
        println response.contentAsString
        resp.size() == 2

        resp[0].response.data.some1 == 3
        resp[1].response.data.some == 4
    }


    def "it should be possible to invoke exposed method with multiple parameters"() {
        given:
        request.json = '{ "data":{__method:"ExposedInvoiceService.doSomethingMultiParam",__params:[1,2] }, "oldValues":null }'
        when:
        controller.index()
        def resp = JSON.parse(response.contentAsString)
        then:
        println response.contentAsString

        resp.response.data.res == 3

    }


    def "fetch operation should be supported"() {
        given:
        request.json = '{ "data":{__method:"InvoiceService.fetchSomething" } }'
        when:
        controller.index()
        def resp = JSON.parse(response.contentAsString)
        then:

        resp.response.startRow == 0
        resp.response.endRow == 2
        resp.response.totalRows == 2
        resp.response.data.size() == 2


    }

}

@Remote
class ExposedInvoiceService {

    def doSomething(@P('param1') data) {
        return [some1: ++data.value]
    }

    def doSomethingMultiParam(p1, p2) {
        return [res: p1 + p2]
    }
}

class InvoiceService {
    @Remote
    def doSomething(data) {
        return [some: ++data.value]
    }

    @Remote(operation = Operation.FETCH)
    def fetchSomething(data) {
        return [[some: 1], [some: 1]]
    }


    def notExposed(data) {
        return [some: 'value2']
    }

}