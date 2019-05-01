package org.grails.plugins.smartclient

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Denis Halupa
 */
class CsrfTokenHandler {
    private String _token

    String getToken() {
        if (this._token == null) {
            this._token = UUID.randomUUID().toString()
        }
        this._token
    }

    def verify(HttpServletRequest request, HttpServletResponse response) {
        if (request.method == 'GET') {
            return true
        }

        def csrf = request.getHeader('Csrf-Token')
        if (csrf == null || csrf.isEmpty()) {
            response.sendError HttpServletResponse.SC_UNAUTHORIZED
            return false
        } else {
            def sessionCsrf = getToken()
            if (sessionCsrf != csrf) {
                response.sendError HttpServletResponse.SC_UNAUTHORIZED
                return false
            }
        }
        return true
    }


}
