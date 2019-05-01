isc.defineClass('RemoteMethod').addClassProperties({
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
});