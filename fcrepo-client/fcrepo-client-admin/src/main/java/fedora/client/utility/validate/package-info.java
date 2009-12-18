/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

/**
 * <p>
 * These classes are used to validate objects against their content models,
 * and to validate the content models for internal consistency.
 * </p>
 * <p>
 * This package contains the basic Validator framework, with core classes and interfaces.
 * </p>
 * <p>
 * <code>fedora.client.utility.validate.process</code> contains a client-side {@link ValidatorProcess}
 * that parses the command line arguments, queries Fedora for the requested objects, validates them.
 * It also has an implementation of {@link ValidatorResult} that uses Log4J to present and
 * control the output.
 * </p>
 * <p>
 * <code>fedora.client.utility.validate.remote</code> contains an implementation of {@link ObjectSource}
 * that can be used to access a remote instance of Fedora, with helper classes.
 * </p>
 * <p>
 * </p>
 */
package fedora.client.utility.validate;

