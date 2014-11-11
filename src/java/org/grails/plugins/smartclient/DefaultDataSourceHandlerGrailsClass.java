package org.grails.plugins.smartclient;

import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass;

public class DefaultDataSourceHandlerGrailsClass extends AbstractInjectableGrailsClass implements DataSourceHandlerGrailsClass {

	public DefaultDataSourceHandlerGrailsClass(Class<?> clazz) {
		super(clazz, DataSourceHandlerArtefactHandler.TYPE);
	}
}
