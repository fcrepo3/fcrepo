/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation class for parameter names.
 * If and when Java 7 provides reflective access to parameter names,
 * use of this class should be revisited.
 *
 * @author Edwin Shin
 * @version $Id$
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PName {

    //TODO consider enums for the various API-* parameter names

    String value();
}
