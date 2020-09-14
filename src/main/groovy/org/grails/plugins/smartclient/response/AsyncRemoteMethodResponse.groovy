package org.grails.plugins.smartclient.response

/**
 * @author Denis Halupa
 */
class AsyncRemoteMethodResponse extends RemoteMethodResponse {

    AsyncRemoteMethodResponse() {
        super()
    }


    AsyncRemoteMethodResponse(data) {
        super(data)
    }

    AsyncRemoteMethodResponse(data, dwr) {
        super(data, dwr)
    }

    AsyncRemoteMethodResponse(data, dwr, boolean suspendCallback) {
        super(data, dwr, suspendCallback)
    }
}
