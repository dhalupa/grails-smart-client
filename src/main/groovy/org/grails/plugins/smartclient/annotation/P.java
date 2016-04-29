package org.grails.plugins.smartclient.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If applied to the parameter, will produce named parameter in remote API definition
 *
 * @author Denis Halupa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface P {
    /**
     * A parameter name
     *
     * @return
     */
    String value();
}
