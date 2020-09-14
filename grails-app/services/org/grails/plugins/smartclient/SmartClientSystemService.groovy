package org.grails.plugins.smartclient

import grails.core.GrailsApplication
import org.grails.plugins.smartclient.annotation.Remote
import org.grails.plugins.smartclient.response.AsyncRemoteMethodResponse

/**
 * @author Denis Halupa
 */
class SmartClientSystemService {
    GrailsApplication grailsApplication

    @Remote
    ping() {
        grailsApplication.mainContext.conversationScope.touch()
        [success: true]
    }

    @Remote(raw = true)
    getProgressInfo(criteria) {
        AsyncRemoteMethodResponse response = longOperationsRunner.getStatus(criteria.uuid as String) ?: [:]
        response.timestamp = System.currentTimeMillis()
        response.canceled = criteria.canceled ?: false
        response = new AsyncRemoteMethodResponse(response.data, response.dwr, response.suspendCallback)

        if (response.canceled) {
            response.message = localizedMessageSource.getMessage("progress.canceling")
        } else {
            response.message = response.message ? localizedMessageSource.getMessage("${response.message}") : ''
        }
        if (response.finished) {
            response.finalizationClosure?.call()
            response.data.remove('finalizationClosure')
            longOperationsRunner.removeStatus(criteria.uuid as String)
        }
        return response
    }
}
