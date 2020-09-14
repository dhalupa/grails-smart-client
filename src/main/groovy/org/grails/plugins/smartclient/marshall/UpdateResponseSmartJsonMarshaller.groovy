package org.grails.plugins.smartclient.marshall


import grails.converters.JSON
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.plugins.smartclient.response.UpdateResponse
import org.grails.plugins.smartclient.response.ValidatingBean
import org.grails.web.converters.exceptions.ConverterException

/**
 * @author Denis Halupa
 */
//@CompileStatic
class UpdateResponseSmartJsonMarshaller implements SmartJsonMarshaller {

    //  LocalizedMessageSource localizedMessageSource

    @Override
    boolean supports(Object object) {
        return object instanceof UpdateResponse
    }

    @Override
    void marshalObject(Object object, JSON converter) throws ConverterException {
        UpdateResponse response = object as UpdateResponse
        def data = response.data
        if (DomainClassArtefactHandler.isDomainClass(data.getClass()) || data instanceof ValidatingBean) {
            if (data.errors?.hasErrors()) {
                def d = ['response': [status: -4, dwr: response.dwr]]
                def errors = [:]
                data.errors.allErrors.each { err ->
                    if (!errors[err.field]) {
                        errors[err.field] = [
                                [errorMessage: err]
                        ]
                    } else {
                        errors[err.field] << [errorMessage: err]
                    }
                }
                d.response.errors = errors
                converter.convertAnother(d)
                return
            }
        } else if (response.errors) {
            def d = ['response': [status: -4, dwr: response.dwr]]
            def errors = [:]
            response.errors.each { k, v ->
                errors[k] = [errorMessage: v]
            }
            d.response.errors = errors
            converter.convertAnother(d)
            return
        }
        converter.writer.object()
        converter.writer.key('response')
        converter.writer.object()
        converter.property('status', 0)
        converter.property('data', data)
        converter.property('dwr', response.dwr)
        converter.writer.endObject()
        converter.writer.endObject()

    }


}
