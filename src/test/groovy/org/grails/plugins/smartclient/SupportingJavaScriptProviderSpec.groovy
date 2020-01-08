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
        provider = new RemoteApiJavaScriptProvider(smartClientConfigProvider: configProvider, grailsApplication: grailsApplication)
    }

    def 'remote API should be built from service classes meta'() {
        expect:
        provider.createRemoteAPI() == expected
    }

    def expected='''/**
 * This type of datasource supports usage of exposed remote methods for operations
 */


isc.defineClass('RemoteMethodDataSource', 'RestDataSource').addProperties({
    dataURL: 'remoteMethodExecutor',
    dataFormat: 'json',
    jsonPrefix: window.scJsonPrefix,
    jsonSuffix: window.scJsonSuffix,
    fetchMethod: null,
    updateMethod: null,
    addMethod: null,
    removeMethod: null,
    operationBindings: [
        {operationType: 'fetch', dataProtocol: "postMessage"},
        {operationType: "update", dataProtocol: "postMessage"},
        {operationType: "add", dataProtocol: "postMessage"},
        {operationType: "remove", dataProtocol: "postMessage"}
    ],
    transformRequest: function (dsRequest) {
        var headers = dsRequest.httpHeaders || {};
        headers.nopage = 'true';
        if (window._scCsrfToken) headers['Csrf-Token'] = window._scCsrfToken;
        dsRequest.httpHeaders = headers;
        var rm = this[dsRequest.operationType + 'Method'];
        if (rm) {
            dsRequest.data['__method'] = rm;
            dsRequest.data['__conversationId'] = window._scConversationId;
        } else {
            throw 'Remote method not configured for ' + dsRequest.operationType;
        }
        return this.Super("transformRequest", arguments);
    },
    transformResponse: function (dsResponse, dsRequest, data) {
        var dsResponse = this.Super("transformResponse", arguments);
        if (dsResponse.httpResponseCode == 401) {
            window.location.reload()
            dsResponse.httpResponseText = '{status:0}\'
            return dsResponse
        }
        if (data.response.dwr) {
            _.each(data.response.dwr, function (it) {
                var scope = window[it.scope]
                if (scope) {
                    var f = scope[it.function]
                    if (_.isFunction(f)) {
                        f.apply(scope, it.arguments)
                    } else {
                        throw 'Function for reverse ajax invocation can not be found: ' + it.scope + '.' + it.function;
                    }

                } else {
                    throw 'Scope for reverse ajax invocation can not be found: ' + it.scope;

                }
            });
        }
        return dsResponse;
    }

});
/**
 * Datasource used for remote method invocation
 */
isc.RestDataSource.create({
    "dataURL": "remoteMethodExecutor",
    "ID": "scRemoteMethodExecutionDS",
    "dataFormat": "json",
    "fields": [],
    "progressInfo": {},
    "operationBindings": [{"operationType": "custom", "dataProtocol": "postMessage"}],
    "jsonPrefix": window.scJsonPrefix,
    "jsonSuffix": window.scJsonSuffix,
    transformResponse: function (dsResponse, dsRequest, data) {
        var dsResponse = this.Super("transformResponse", arguments);
        if (dsResponse.httpResponseCode == 401) {
            window.location.reload()
            dsResponse.httpResponseText = '{status:0}\'
            return dsResponse
        }
        if (data.response && data.response.dwr) {
            _.each(data.response.dwr, function (it) {
                var scope = window[it.scope]
                if (scope) {
                    var f = scope[it.function]
                    if (_.isFunction(f)) {
                        f.apply(scope, it.arguments)
                    } else {
                        throw 'Function for reverse ajax invocation can not be found: ' + it.scope + '.' + it.function;
                    }

                } else {
                    throw 'Scope for reverse ajax invocation can not be found: ' + it.scope;

                }
            });
        }
        return dsResponse;
    },
    transformRequest: function (dsRequest) {
        var headers = dsRequest.httpHeaders || {};
        headers.nopage = 'true';
        if (window._scCsrfToken) headers['Csrf-Token'] = window._scCsrfToken;
        dsRequest.httpHeaders = headers;
        return this.Super("transformRequest", arguments);
    },
});
/**
 * This class is used to invoke exposed remote method
 */
isc.defineClass('RemoteMethodExecutor').addClassProperties({
    invoke: function (method, data, callback, requestProperties) {
        var cb = callback || function () {
        };
        var data = data || {};
        data.__method = method;
        data.__conversationId = window._scConversationId;
        var ds = isc.DataSource.get('scRemoteMethodExecutionDS');
        ds.performCustomOperation('custom', data, function () {
            var data = arguments[1][0].result;
            var suspendCallback = arguments[1][0].suspendCallback
            if (!suspendCallback) {
                cb.call(this, data);
            }
        }, requestProperties)

    }
});var rmt=new function(){var emptyFn = function () {};return {ExposedInvoiceService:{doSomething : function(param1, successCallback,failureCallback) {var successCb=successCallback||emptyFn;var failureCb=failureCallback||emptyFn;var arg = {__params: [param1]};isc.RemoteMethodExecutor.invoke('ExposedInvoiceService.doSomething', arg, successCb,failureCb);},doSomethingMultiParam : function(arg0,arg1, successCallback,failureCallback) {var successCb=successCallback||emptyFn;var failureCb=failureCallback||emptyFn;var arg = {__params: [arg0,arg1]};isc.RemoteMethodExecutor.invoke('ExposedInvoiceService.doSomethingMultiParam', arg, successCb,failureCb);}},InvoiceService:{doSomething : function(arg0, successCallback,failureCallback) {var successCb=successCallback||emptyFn;var failureCb=failureCallback||emptyFn;var arg = {__params: [arg0]};isc.RemoteMethodExecutor.invoke('InvoiceService.doSomething', arg, successCb,failureCb);},doSomethingWithoutParam : function(successCallback,failureCallback) {var successCb=successCallback||emptyFn;var failureCb=failureCallback||emptyFn;var arg = {__params: []};isc.RemoteMethodExecutor.invoke('InvoiceService.doSomethingWithoutParam', arg, successCb,failureCb);}}}}();'''
}

