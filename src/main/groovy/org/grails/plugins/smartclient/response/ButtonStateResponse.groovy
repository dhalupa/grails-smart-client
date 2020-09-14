package org.grails.plugins.smartclient.response

class ButtonStateResponse extends BaseResponse {
    ButtonStateResponse(buttonStates) {
        this.dwr =[scope: 'statementEditor', function: 'setButtonStates', arguments: [buttonStates]]
    }
}
