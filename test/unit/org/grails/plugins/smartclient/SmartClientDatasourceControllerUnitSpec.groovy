package org.grails.plugins.smartclient

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.plugins.smartclient.annotation.NamedParam
import org.grails.plugins.smartclient.annotation.Progress
import org.grails.plugins.smartclient.annotation.Remote
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(SmartClientDatasourceController)
@Mock([Invoice])
class SmartClientDatasourceControllerUnitSpec extends Specification {
    def invoice, item1, item2

    def setup() {

        grailsApplication.registerArtefactHandler(new DataSourceHandlerArtefactHandler())
        grailsApplication.addArtefact(InvoiceDataSourceHandler)

        controller.smartClientDataSourceHandlerExecutionService = Mock(SmartClientDataSourceHandlerExecutionService)
        controller.smartClientDataSourceDefinitionService = Mock(SmartClientDataSourceDefinitionService) {
            getJsonPrefix() >> ''
            getJsonSuffix() >> ''
        }

    }

    def "with non transactional request, single operation should be executed"() {
        given:
        request.json = '{ "dataSource":"invoice", "operationType":"fetch", "startRow":0, "endRow":75, "textMatchStyle":"substring", "componentId":"isc_ListGrid_0", "data":{ }, "oldValues":null }'
        when:
        controller.serve()
        then:
        1 * controller.smartClientDataSourceHandlerExecutionService.executeOperation(null, _) >> [some: 'response']
        response.contentAsString == """{"some":"response"}"""
    }

    def "with transactional request, a transaction should be executed"() {
        given:
        request.json = """{ transaction: { transactionNum: 3, operations: [{ "dataSource":"test", "operationType":"fetch", "startRow":0, "endRow":75, "textMatchStyle":"substring", "componentId":"isc_ListGrid_1", "data":{ }, "oldValues":null }, { "dataSource":"test1", "operationType":"fetch", "data":null, "oldValues":null }]}}"""
        when:
        controller.serve()
        then:
        1 * controller.smartClientDataSourceHandlerExecutionService.executeTransaction(_) >> [some: 'response']
        response.contentAsString == """{"some":"response"}"""
    }
}

@Entity
class Invoice {

    String name
    boolean admin
    Date created

}


class InvoiceDataSourceHandler {
    static domainClass = Invoice

    static fields = {
        field(name: 'name', width: 300, title: 'Invoice') {
            validator(type: 'floatRange', min: 0, errorMessage: message('Please enter a valid (positive) cost'))

        }
    }
    static operations = ['fetch', 'remove', 'update']

    def fetch(input) {
        return Invoice.list()
    }

    def add(input) {
        [exec: 'add']
    }

    def remove(input) {
        [exec: 'remove']
    }

    @Progress
    def update(input) {
        [exec: 'update']
    }


    @Progress('some.key')
    def custom(data) {

    }

    def myMethod(data) {
        [exec: 'custom']
    }


}

@Remote
class ExposedInvoiceService {

    def doSomething() {
        return [some: 'value1']
    }
}

class InvoiceService {
    @Remote
    def doSomething(@NamedParam('client') String client, @NamedParam('ledger') Integer ledger, param) {
        return [some: 'value']
    }


    def notExposed(criteria) {
        return [some: 'value2']
    }

}
