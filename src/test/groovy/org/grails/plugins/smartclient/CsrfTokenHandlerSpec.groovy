package org.grails.plugins.smartclient

import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

/**
 *
 * @author Ivan Milojevic
 */
class CsrfTokenHandlerSpec extends Specification {

    @Shared CsrfTokenHandler tokenHandler
    @Shared String token

    def setupSpec() {
        tokenHandler = new CsrfTokenHandler()
        token = tokenHandler.getToken()
    }

    def "verification passes if request method is GET"() {
        given:
        def request = Mock(HttpServletRequest)
        request.method >> 'GET'

        def response = new MockHttpServletResponse()

        when:
        def result = tokenHandler.verify(request, response)

        then:
        result
    }

    def "verification passes if request method is POST and Csrf-Token header is the same as session's csrf token"() {
        given:
        def request = Mock(HttpServletRequest)
        request.method >> 'POST'
        request.getHeader('Csrf-Token') >> token

        def response = new MockHttpServletResponse()

        when:
        def result = tokenHandler.verify(request, response)

        then:
        result
    }

    def "verification failed if method is POST and Csrf-Token header is empty"() {
        given:
        def request = Mock(HttpServletRequest)
        request.method >> 'POST'
        request.getHeader('Csrf-Token') >> ""

        def response = new MockHttpServletResponse()

        when:
        def result = tokenHandler.verify(request, response)

        then:
        !result
        response.getStatus() == 401
    }

    def "verification failed if method is POST and Csrf-Token header is different then session's csrf token"() {
        given:
        def request = Mock(HttpServletRequest)
        request.method >> 'POST'
        request.getHeader('Csrf-Token') >> "wrong token"

        def response = new MockHttpServletResponse()

        when:
        def result = tokenHandler.verify(request, response)

        then:
        !result
        response.getStatus() == 401
    }

}
