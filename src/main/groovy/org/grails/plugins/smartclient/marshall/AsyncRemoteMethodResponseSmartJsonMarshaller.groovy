package org.grails.plugins.smartclient.marshall


import grails.converters.JSON
import org.grails.plugins.smartclient.response.AsyncRemoteMethodResponse
import org.grails.web.converters.exceptions.ConverterException

/**
 * @author Denis Halupa
 */
//@CompileStatic
class AsyncRemoteMethodResponseSmartJsonMarshaller implements SmartJsonMarshaller {


    @Override
    boolean supports(Object object) {
        return object instanceof AsyncRemoteMethodResponse
    }

    @Override
    void marshalObject(Object object, JSON converter) throws ConverterException {
        AsyncRemoteMethodResponse response = object as AsyncRemoteMethodResponse
        converter.writer.object()
        converter.writer.key('response')
        converter.writer.object()
        converter.property('status', 0)
        converter.property('data', [result: [suspendCallback: response.suspendCallback, data: response.data]])
        converter.property('dwr', response.dwr)
        converter.writer.endObject()
        converter.writer.endObject()

    }
}
