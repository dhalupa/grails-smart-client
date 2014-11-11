package org.grails.plugins.smartclient.builder

import groovy.xml.MarkupBuilder

/**
 * @author Denis Halupa
 */
class FieldsDefinitionBuilder {
    def config = [:]

    MarkupBuilder m;

    def field(Map config, Closure children) {
        FieldInnerBuilder b = new FieldInnerBuilder(config)
        children.delegate = b
        children.resolveStrategy = Closure.DELEGATE_ONLY
        children()
        field(b.config)
    }

    def field(Map config) {
        def name = config.name
        if (name) {
            this.config[name] = config
        } else {
            throw new RuntimeException("name attribute has to be set for a field definition: $config")
        }
    }



    private static class FieldInnerBuilder {

        def config

        FieldInnerBuilder(Map config) {
            this.config = config
        }


        def valueMap(List model) {
            this.config.valueMap = model
        }

        def valueMap(Map model) {
            this.config.valueMap = model
        }

        def valueMap(Closure model) {
            this.config.valueMap = model()
        }


        def validator(Map config) {
            if (!this.config.validators) {
                this.config.validators = []
            }
            this.config.validators << config
        }
        def message(msg){
            'loc_'+msg
        }

    }


}
