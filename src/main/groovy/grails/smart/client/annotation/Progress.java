package grails.smart.client.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If applied to the DataSource method will indicate that SmartClient progress dialog should be shown for the annotated
 * method
 *
 * @author Denis Halupa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Progress {
    /**
     * A key for the progress message shown in the dialog
     *
     * @return
     */
    String value() default "org.grails.plugins.smartclient.progress";
}
