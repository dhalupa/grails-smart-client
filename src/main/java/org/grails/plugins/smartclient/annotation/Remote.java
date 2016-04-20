package org.grails.plugins.smartclient.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If applied to the Service class will expose methods for remote invocation
 * method. It can also be applied to particular methods
 *
 * @author Denis Halupa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Remote {
}
