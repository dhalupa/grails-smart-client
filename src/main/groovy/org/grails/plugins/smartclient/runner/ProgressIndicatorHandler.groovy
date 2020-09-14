package org.grails.plugins.smartclient.runner


import org.grails.plugins.smartclient.response.AsyncRemoteMethodResponse

/**
 * Service with responsibility to handle session data related to progress of long running action
 * @author Denis Halupa
 */
class ProgressIndicatorHandler {


    LongOperationsRunner longOperationsRunner

    private static final MSG = 'message'
    private static final TARGET = 'target'
    private static final PROGRESS = 'progress'
    private static final PERCENT = 'percent'
    private static final CANCELED = 'canceled'

    static ProgressIndicatorHandler INSTANCE

    /**
     * Mark the start of long running action
     * @param target
     * @param action
     */
    void startProgress(int target, String action, Object... args) {
        def store = getStore()
        String msg = action
        try {
            msg = longOperationsRunner.longOperationContextProvider.getMessage("progress.${action}", args)
        } catch (Exception e) {
        }
        store[TARGET] = target
        store[MSG] = msg
        store[PERCENT] = 0
        store[PROGRESS] = 0
        store[CANCELED] = false
    }

    void init() {
        INSTANCE = this
    }

    static ProgressIndicatorHandler getInstance() {
        return INSTANCE
    }
    /**
     * Mark the start of long running action
     * @param target
     * @param action
     */
    void startProgress(int target, Closure builder) {
        def store = getStore()
        startProgress(target, builder.call(store, longOperationsRunner.longOperationContextProvider))
    }

    /**
     * Mark that some progress is made in approaching the designated target
     * @param progress
     */
    void incrementProgress(Integer step = null) {
        def store = getStore()
        int target = store[TARGET] ?: 0
        if (target) {
            if (step == null) {
                store[PROGRESS]++
            } else {
                store[PROGRESS] = store[PROGRESS] + step

            }
            store[PERCENT] = Math.round(store[PROGRESS] / target * 100)
        }
    }
    /**
     * Returns the current status of long running transaction
     * @return
     */
    def getStatus() {
        AsyncRemoteMethodResponse store = getStore()
        store.data.putAll([message : store[MSG], percent: store[PERCENT] ?: 0,
                           target  : store[TARGET] ?: 0,
                           progress: store[PROGRESS] ?: 0])
        store


    }


    AsyncRemoteMethodResponse getStore() {
        def s = longOperationsRunner.status
        if (s[CANCELED]) {
            throw new ActionCanceledException('Action has been canceled')
        }
        if (s.timestamp) {
            def quietPeriod = System.currentTimeMillis() - s.timestamp
            if (quietPeriod > 60000) {
                throw new ActionCanceledException('Timeout has occured')
            }
        }
        s
    }

}
