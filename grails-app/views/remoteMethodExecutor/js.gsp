
/**
 * This type of datasource supports usage of exposed remote methods for operations
 */
isc.defineClass('RemoteMethodDataSource', 'RestDataSource').addProperties({
    dataURL: 'remoteMethodExecutor',
    dataFormat: 'json',
    jsonPrefix: '${raw(jsonPrefix)}',
    jsonSufix: '${raw(jsonSufix)}',
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
        var rm = this[dsRequest.operationType + 'Method'];
        if (rm) {
            dsRequest.data['__method'] = rm;
        } else {
            throw 'Remote method not configured for ' + dsRequest.operationType;
        }
        return this.Super("transformRequest", arguments);
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
    "jsonPrefix": '${raw(jsonPrefix)}',
    "jsonSufix": '${raw(jsonSufix)}'
});
/**
 * This class is used to invoke exposed remote method
 */
isc.defineClass('RemoteMethod').addClassProperties({
    invoke: function (method, data, callback) {
        var cb = callback || function () {};
        var data = data || {};
        data.__method = method;
        var ds = isc.DataSource.get('scRemoteMethodExecutionDS');
        ds.performCustomOperation('custom', data, function () {
            var data = arguments[1][0];
            cb.call(this, data);
        })

    }
});

