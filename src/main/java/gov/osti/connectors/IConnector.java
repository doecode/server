/*
 */
package gov.osti.connectors;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author ensornl
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target (ElementType.TYPE)
@Inherited
public @interface IConnector {
    
}
