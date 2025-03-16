package org.grails.plugins.smartclient

import java.lang.reflect.Method

/**
 * Will execute before remote method is executed
 */
interface RemoteMethodInterceptor {

    def before(Method remoteMethod)

}