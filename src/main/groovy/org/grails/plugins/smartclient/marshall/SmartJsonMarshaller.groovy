package org.grails.plugins.smartclient.marshall

import grails.converters.JSON
import org.grails.web.converters.marshaller.ObjectMarshaller

/**
 * Should be implemented by all marshallers which has to be registered in smart configuration
 * @author Denis Halupa
 */
interface SmartJsonMarshaller extends ObjectMarshaller<JSON> {

}