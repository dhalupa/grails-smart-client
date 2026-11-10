package org.grails.plugins.smartclient.runner

import org.grails.plugins.smartclient.response.AsyncRemoteMethodResponse
import spock.lang.Issue
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The status of a long operation is written by the operation in its own thread and read by the
 * requests that poll it. These tests check that the status can be shared between threads.
 */
@Issue('RF-2676')
class LongOperationsRunnerSpec extends Specification {

    LongOperationContextProvider contextProvider = Mock()
    /** The provider is a mock, so the operation itself never runs. */
    LongOperationsRunner runner = new LongOperationsRunner(longOperationContextProvider: contextProvider)

    def "the status map of all operations is concurrent"() {
        expect:
        runner.statusMap instanceof ConcurrentHashMap
    }

    def "the status of an operation is a synchronized copy of its config"() {
        given:
        Map config = [type: 'import']

        when:
        String uuid = runner.execute(config) { }
        def data = runner.getStatus(uuid).data

        then:
        data.getClass().name == 'java.util.Collections$SynchronizedMap'
        data.type == 'import'

        when: 'the operation writes to its status'
        runner.getStatus(uuid).progress = 5

        then: 'the config of the caller does not change'
        !config.containsKey('progress')
    }

    def "the status can be read outside an operation, where there is no uuid"() {
        expect:
        runner.getStatus() instanceof AsyncRemoteMethodResponse
        runner.getStatus(null) == null
        runner.removeStatus() == null
        runner.removeStatus(null) == null
    }

    def "a snapshot is a copy of the data"() {
        given:
        def status = new AsyncRemoteMethodResponse(Collections.synchronizedMap([percent: 10]))

        when:
        Map snapshot = status.snapshot()
        status.percent = 20

        then:
        snapshot == [percent: 10]
        status.percent == 20
    }

    def "a snapshot of a status without data is empty"() {
        expect:
        new AsyncRemoteMethodResponse(null).snapshot() == [:]
    }

    def "a snapshot taken while the operation writes never meets a ConcurrentModificationException"() {
        given:
        def status = new AsyncRemoteMethodResponse(Collections.synchronizedMap(new LinkedHashMap()))
        def done = new CountDownLatch(1)
        def writer = Thread.start {
            for (int i = 0; i < 200_000; i++) {
                status."key${i % 500}" = i
                if (i % 500 == 499) {
                    status.data.clear()
                }
            }
            done.countDown()
        }
        List<Throwable> errors = []

        when:
        while (done.count > 0) {
            try {
                status.snapshot()
            } catch (Throwable t) {
                errors << t
                break
            }
        }
        done.await(1, TimeUnit.MINUTES)
        writer.join()

        then:
        errors.empty
    }
}
