package org.grails.plugins.smartclient

/**
 * This component is able to render different types of reponses required for SmartClient RestDataSource
 * @author Denis Halupa
 */
trait SmartClientResponseRenderer {

    /**
     * Used when operation type is 'fetch'
     * @param model
     * @return
     */
    def renderFetchResponse(model) {
        ['response':
                 [
                         status   : 0,
                         startRow : 0,
                         endRow   : model.size(),
                         totalRows: model.size(),
                         data     : model
                 ]
        ]
    }

    /**
     * Response used for all other operation types except 'fetch'
     * @param value
     * @return
     */
    def renderDataResponse(value) {
        return ['response': [status: 0, data: value]]
    }
    /**
     * Response rendered in case some kind of error occured
     * @param value
     * @return
     */
    def renderErrorResponse(value) {
        ['response': [status: -1, data: value]]
    }
}
