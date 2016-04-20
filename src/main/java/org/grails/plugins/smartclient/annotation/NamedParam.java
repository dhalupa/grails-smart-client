package org.grails.plugins.smartclient.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameter annotation defining a name which will be used for JavaScript parameter
 *
 * @author Denis Halupa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface NamedParam {
    /**
     * Name of parameter
     *
     * @return
     */
    String value();
}
