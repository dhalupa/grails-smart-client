package org.grails.plugins.smartclient

class UrlMappings {

    static mappings = {
        "/datasource/definitions"(controller:"smartClientDatasource",action:"definitions")

        "/datasource/$dsID"(controller:"smartClientDatasource",action:"serve")
        "/datasource"(controller:"smartClientDatasource",action:"serve")


    }
}
