package org.grails.plugins.smartclient.marshall


import grails.converters.JSON
import org.grails.plugins.smartclient.response.RemoteMethodResponse
import org.grails.web.converters.exceptions.ConverterException

/**
 * @author Denis Halupa
 */
//@CompileStatic
class RemoteMethodResponseSmartJsonMarshaller implements SmartJsonMarshaller {


    @Override
    boolean supports(Object object) {
        return object instanceof RemoteMethodResponse
    }

    @Override
    void marshalObject(Object object, JSON converter) throws ConverterException {
        RemoteMethodResponse response = object as RemoteMethodResponse
        converter.writer.object()
        converter.writer.key('response')
        converter.writer.object()
        converter.property('status', 0)
        converter.property('data', [suspendCallback: response.suspendCallback, result: response.data])
        converter.property('dwr', response.dwr)
        converter.writer.endObject()
        converter.writer.endObject()

    }
}
