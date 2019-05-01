package org.grails.plugins.smartclient.marshall

import grails.converters.JSON
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.converters.marshaller.ObjectMarshaller

/**
 * @author Denis Halupa
 */
class RawJavascriptMarshaller implements ObjectMarshaller<JSON> {
    @Override
    boolean supports(Object object) {
        return object instanceof RawJavascriptMarshaller
    }

    @Override
    void marshalObject(Object object, JSON converter) throws ConverterException {

        converter.writer()

    }
}
