package org.grails.plugins.smartclient.marshall


import grails.converters.JSON
import org.grails.plugins.smartclient.response.FetchResponse
import org.grails.web.converters.exceptions.ConverterException

/**
 * @author Denis Halupa
 */
//@CompileStatic
class FetchResponseSmartJsonMarshaller implements SmartJsonMarshaller {


    @Override
    boolean supports(Object object) {
        return object instanceof FetchResponse
    }

    @Override
    void marshalObject(Object object, JSON converter) throws ConverterException {
        FetchResponse response = object as FetchResponse
        converter.writer.object()
        converter.writer.key('response')
        converter.writer.object()
        converter.property('status', 0)
        converter.property('startRow', 0)
        int size = response.data.size()
        converter.property('endRow', size)
        converter.property('totalRows', size)
        converter.property('data', response.data)
        converter.property('dwr', response.dwr)
        converter.writer.endObject()
        converter.writer.endObject()

    }
}
