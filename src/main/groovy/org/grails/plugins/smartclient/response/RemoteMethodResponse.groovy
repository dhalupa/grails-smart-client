package org.grails.plugins.smartclient.response

/**
 * @author Denis Halupa
 */
class RemoteMethodResponse extends BaseResponse {
    boolean suspendCallback = false

    RemoteMethodResponse() {
        super()
    }


    RemoteMethodResponse(data) {
        super(data)
    }

    RemoteMethodResponse(data, dwr) {
        super(data, dwr)
    }

    RemoteMethodResponse(data, dwr, boolean suspendCallback) {
        super(data, dwr)
        this.suspendCallback = suspendCallback
    }
}
