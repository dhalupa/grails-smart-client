/**
 * This type of datasource supports usage of exposed remote methods for operations
 */


isc.defineClass('RemoteMethodDataSource', 'RestDataSource').addProperties({
    dataURL: 'remoteMethodExecutor',
    dataFormat: 'json',
    jsonPrefix: window.scJsonPrefix,
    jsonSuffix: window.scJsonSufix,
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
            dsResponse.httpResponseText = '{status:0}'
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
    "jsonSuffix": window.scJsonSufix,
    transformResponse: function (dsResponse, dsRequest, data) {
        var dsResponse = this.Super("transformResponse", arguments);
        if (dsResponse.httpResponseCode == 401) {
            window.location.reload()
            dsResponse.httpResponseText = '{status:0}'
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
});