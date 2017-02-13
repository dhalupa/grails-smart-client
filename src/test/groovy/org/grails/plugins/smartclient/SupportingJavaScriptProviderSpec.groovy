package org.grails.plugins.smartclient

import grails.core.GrailsApplication
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.grails.core.artefact.ServiceArtefactHandler
import spock.lang.Specification

/**
 * Created by dhalupa on 02.05.16..
 */
@TestMixin(GrailsUnitTestMixin)
class SupportingJavaScriptProviderSpec extends Specification {

    RemoteApiJavaScriptProvider provider
    GrailsApplication grailsApplication

    def setup() {
        grailsApplication = applicationContext.getBean(GrailsApplication)
        grailsApplication.registerArtefactHandler(new ServiceArtefactHandler())
        grailsApplication.addArtefact(ExposedInvoiceService)
        grailsApplication.addArtefact(InvoiceService)

        ConfigProvider configProvider = Mock() {
            getJsonPrefix() >> ''
            getJsonSuffix() >> ''
            getConverterConfig() >> null
        }
        provider = new RemoteApiJavaScriptProvider(configProvider: configProvider, grailsApplication: grailsApplication)
    }

    def 'remote API should be built from service classes meta'() {
        expect:
        provider.createRemoteAPI() == '''var rmt=new function(){var emptyFn = function () {};return {ExposedInvoiceService:{doSomething : function(param1, successCallback,failureCallback) {var successCb=successCallback||emptyFn;var failureCb=failureCallback||emptyFn;var arg = {__params: [param1]};isc.RemoteMethod.invoke('ExposedInvoiceService.doSomething', arg, successCb,failureCb);},doSomethingMultiParam : function(arg0,arg1, successCallback,failureCallback) {var successCb=successCallback||emptyFn;var failureCb=failureCallback||emptyFn;var arg = {__params: [arg0,arg1]};isc.RemoteMethod.invoke('ExposedInvoiceService.doSomethingMultiParam', arg, successCb,failureCb);}},InvoiceService:{doSomething : function(arg0, successCallback,failureCallback) {var successCb=successCallback||emptyFn;var failureCb=failureCallback||emptyFn;var arg = {__params: [arg0]};isc.RemoteMethod.invoke('InvoiceService.doSomething', arg, successCb,failureCb);},doSomethingWithoutParam : function(successCallback,failureCallback) {var successCb=successCallback||emptyFn;var failureCb=failureCallback||emptyFn;var arg = {__params: []};isc.RemoteMethod.invoke('InvoiceService.doSomethingWithoutParam', arg, successCb,failureCb);}}}}();'''
    }
}
