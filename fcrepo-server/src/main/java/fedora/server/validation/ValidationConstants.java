/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.validation;

/**
 * Constants for validating Fedora objects and their components. These constants
 * are also expressed in the schematron rules. They are only repeated here so
 * that per-field validation can occur outside the context of schematron
 * validation.
 * 
 * @author Chris Wilper
 */
public interface ValidationConstants {

    /** Characters a datastream ID can never have. */
    public static final char[] DATASTREAM_ID_BADCHARS = new char[] {'+', ':'};

    /** Maximum characters a datastream ID can have. */
    public static final int DATASTREAM_ID_MAXLEN = 64;

    /** Maximum characters a datastream label can have. */
    public static final int DATASTREAM_LABEL_MAXLEN = 255;

    /** Maximum characters a disseminator ID can have. */
    public static final int DISSEMINATOR_ID_MAXLEN = 64;

    /** Characters a disseminator ID can never have. */
    public static final char[] DISSEMINATOR_ID_BADCHARS = new char[] {'+', ':'};

    /** Maximum characters a disseminator label can have. */
    public static final int DISSEMINATOR_LABEL_MAXLEN = 255;

    /** Maximum characters an object label can have. */
    public static final int OBJECT_LABEL_MAXLEN = 255;

}
