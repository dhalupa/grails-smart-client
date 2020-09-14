package org.grails.plugins.smartclient.response

import org.grails.datastore.mapping.validation.ValidationErrors

/**
 * @author Denis Halupa
 */
class ValidatingBean {

    ValidationErrors validationErrors
    //just to make ValidationErrors happy
    String ruleName

    def getErrors() {
        validationErrors ?: rule.errors
    }

    void addError(String name, String code) {
        if (validationErrors == null) {
            validationErrors = new ValidationErrors(this)
        }
        validationErrors.putAt(name, code)
    }
}
