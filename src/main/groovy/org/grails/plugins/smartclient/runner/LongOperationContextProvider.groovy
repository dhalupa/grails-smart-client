package org.grails.plugins.smartclient.runner

/**
 * @author Denis Halupa
 */
interface LongOperationContextProvider {

    void updateContextMap(Map context)

    void storeThreadLocalData(Map context)

    void clearThreadLocalData()

    String getMessage(String action, Object... args)
}
