package org.grails.plugins.smartclient.runner

/**
 * This is an exception thrown when cancel operation is requested
 * @author Denis Halupa
 */
class ActionCanceledException extends RuntimeException {

    ActionCanceledException(String message){
        super(message)
    }
}
