package org.grails.plugins.smartclient

import groovy.transform.TupleConstructor

/**
 * @author Denis Halupa
 */
@TupleConstructor
class PagingFetchResponse {
    def model
    int startRow, endRow, totalRows
}
