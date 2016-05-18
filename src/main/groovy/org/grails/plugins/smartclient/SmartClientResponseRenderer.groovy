package org.grails.plugins.smartclient

import org.springframework.context.MessageSource

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
        return ['response': [status: 0, data: [result: value]]]
    }
    /**
     * Response rendered in case some kind of error occured
     * @param value
     * @return
     */
    def renderErrorResponse(value) {
        ['response': [status: -1, data: value]]
    }
    /**
     * Render response for domain object update. Will render validation message
     * @param value
     * @param messageSource
     * @param locale
     * @return
     */
    def renderDataUpdateResponse(value, MessageSource messageSource, Locale locale) {
        if (value.errors?.hasErrors()) {
            def d = ['response': [status: -4]]
            def errors = [:]
            value.errors.allErrors.each { err ->
                if (!errors[err.field]) {
                    errors[err.field] = [
                            [errorMessage: messageSource.getMessage(err, locale)]
                    ]
                } else {
                    errors[err.field] << [errorMessage: messageSource.getMessage(err, locale)]
                }
            }
            d.response.errors = errors
            return d
        } else {
            return renderDataResponse(value)
        }
    }
}
