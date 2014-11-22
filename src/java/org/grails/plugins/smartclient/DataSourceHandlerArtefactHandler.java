package org.grails.plugins.smartclient;

import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;

/**
 * @author Denis Halupa
 */
public class DataSourceHandlerArtefactHandler extends ArtefactHandlerAdapter {


    public static final String TYPE = "DataSourceHandler";

    public DataSourceHandlerArtefactHandler() {
        super(TYPE, DataSourceHandlerGrailsClass.class, DefaultDataSourceHandlerGrailsClass.class, TYPE);
    }

}
