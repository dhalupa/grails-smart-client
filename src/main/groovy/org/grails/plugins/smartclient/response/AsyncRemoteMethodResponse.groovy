package org.grails.plugins.smartclient.response

/**
 * @author Denis Halupa
 */
class AsyncRemoteMethodResponse extends RemoteMethodResponse {

    AsyncRemoteMethodResponse() {
        super()
    }


    AsyncRemoteMethodResponse(data) {
        super(data)
    }

    AsyncRemoteMethodResponse(data, dwr) {
        super(data, dwr)
    }

    AsyncRemoteMethodResponse(data, dwr, boolean suspendCallback) {
        super(data, dwr, suspendCallback)
    }

    /**
     * A copy of the data, made while no other thread can write to it. The operation writes to the
     * data in its own thread, while a request reads it to report the progress. LongOperationsRunner
     * keeps the data in a synchronized map, and a copy must hold the lock of that map: iterating it
     * without the lock can throw ConcurrentModificationException.
     */
    Map snapshot() {
        Map map = data as Map
        if (map == null) {
            return [:]
        }
        synchronized (map) {
            return new LinkedHashMap(map)
        }
    }
}
