package org.grails.plugins.smartclient.response

/**
 * Response provided by update operation
 * @author Denis Halupa
 */
class UpdateResponse extends BaseResponse {
    Map<String, String> errors


    UpdateResponse() {
        super()
    }


    UpdateResponse(data) {
        super(data)
    }

    UpdateResponse(data, dwr) {
        super(data, dwr)
    }


}
