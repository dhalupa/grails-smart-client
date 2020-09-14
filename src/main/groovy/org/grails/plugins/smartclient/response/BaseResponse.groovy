package org.grails.plugins.smartclient.response

import org.springframework.util.Assert

/**
 * Base class for remote operation response
 * @author Denis Halupa
 */
abstract class BaseResponse {
    List dwr = []
    String scope
    def data = [:]

    BaseResponse() {}

    BaseResponse(data) {
        this.data = data
    }

    BaseResponse(data, dwr) {
        this.data = data
        this.dwr = dwr
    }

    BaseResponse withScope(String scope) {
        this.scope = scope
    }

    BaseResponse invokeFunction(String function, Object... arguments) {
        Assert.notNull(scope, 'Scope is not set!')
        dwr << [scope: scope, function: function, arguments: arguments]
        this
    }

    BaseResponse invokeFunction(String scope, String function, Object... arguments) {
        dwr << [scope: scope, function: function, arguments: arguments]
        this
    }

    def propertyMissing(String name) {
        data[name]
    }

    def propertyMissing(String name, value) {
        data[name] = value
    }

}
