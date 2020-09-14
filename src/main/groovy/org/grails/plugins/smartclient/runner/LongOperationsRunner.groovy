package org.grails.plugins.smartclient.runner


import grails.core.GrailsApplication
import grails.gorm.transactions.GrailsTransactionTemplate
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.Dataflow
import org.grails.plugins.smartclient.response.AsyncRemoteMethodResponse
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionDefinition

/**
 * Component which is responsible to execute long running operations in the background
 * @author Denis Halupa
 */
@Slf4j
class LongOperationsRunner {
    GrailsApplication grailsApplication
    LongOperationContextProvider longOperationContextProvider
    PlatformTransactionManager transactionManager
    boolean async = true
    static LongOperationsRunner INSTANCE


    ThreadLocal<String> uuidHolder = new ThreadLocal<String>()

    Map statusMap = [:]

    /**
     * Executes a long running operation
     * @param config map which will be added to ThreadLocal status map
     * @param operation long running operation itself
     * @return uuid as identification of background job
     */
    def execute(Map config, Closure operation) {
        String uuid = UUID.randomUUID().toString()
        longOperationContextProvider.updateContextMap(config)
        statusMap[uuid] = new AsyncRemoteMethodResponse(config)
        if (async) {
            Dataflow.task {
                longOperationContextProvider.storeThreadLocalData(config)
                asyncWorker.call(uuid, operation)
                longOperationContextProvider.clearThreadLocalData()
            }
        } else {
            syncWorker.call(uuid, operation)
        }
        uuid
    }

    def init() {
        INSTANCE = this
    }

    static LongOperationsRunner getInstance() {
        INSTANCE
    }


    AsyncRemoteMethodResponse getStatus() {
        String uuid = uuidHolder.get()
        def s = statusMap[uuid]
        return s ?: new AsyncRemoteMethodResponse()
    }

    def getStatus(String uuid) {
        statusMap[uuid]
    }

    def removeStatus(String uuid) {
        statusMap.remove(uuid)
    }

    def removeStatus() {
        String uuid = uuidHolder.get()
        statusMap.remove(uuid)
    }

    private def syncWorker = { String uuid, Closure operation ->
        uuidHolder.set(uuid)
        withTransaction { TransactionStatus status ->
            operation.call(status)
        }

    }

    private def asyncWorker = { String uuid, Closure operation ->
        uuidHolder.set(uuid)
        withNewTransaction { TransactionStatus status ->
            try {
                operation.call(status)
            } catch (ActionCanceledException ace) {
                status.setRollbackOnly()
            } catch (Throwable e) {
                statusMap[uuid].error = e.message
                statusMap[uuid].finished = true
                log.error(e.message, e)
                status.setRollbackOnly()
            }
            statusMap[uuid].finished = true
        }
    }

    private void withTransaction(Closure callable) {
        new GrailsTransactionTemplate(transactionManager, new DefaultTransactionDefinition()).execute(callable)
    }

    private void withNewTransaction(Closure callable) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition()
        definition.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        new GrailsTransactionTemplate(transactionManager, definition).execute(callable)

    }


}
