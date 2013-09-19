/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.types;

/**
 * @author Sandy Payette
 */
public class MethodParmDef {
    
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final String PASS_BY_REF = "URL_REF";

    public static final String PASS_BY_VALUE = "VALUE";

    public static final String DATASTREAM_INPUT = "fedora:datastreamInputType";

    public static final String USER_INPUT = "fedora:userInputType";

    public static final String DEFAULT_INPUT = "fedora:defaultInputType";

    public String parmName = null;

    public String parmType = null;

    public String parmDefaultValue = null;

    public String[] parmDomainValues = EMPTY_STRING_ARRAY;

    public boolean parmRequired = true;

    public String parmLabel = null;

    public String parmPassBy = null;

    public MethodParmDef() {

    }
}
