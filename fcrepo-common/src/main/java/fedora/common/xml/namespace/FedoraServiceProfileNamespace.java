/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * The Fedora Service Profile XML namespace.
 * 
 * <pre>
 * Namespace URI    : http://fedora.comm.nsdlib.org/service/profile
 * Preferred Prefix : fsvp
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FedoraServiceProfileNamespace
        extends XMLNamespace {

    //---
    // Elements
    //---

    /** The <code>MIMEType</code> element. */
    public final QName MIME_TYPE;

    /** The <code>serviceDescription</code> element. */
    public final QName SERVICE_DESCRIPTION;

    /** The <code>serviceImplementation</code> element. */
    public final QName SERVICE_IMPLEMENTATION;

    /** The <code>serviceImplDependencies</code> element. */
    public final QName SERVICE_IMPL_DEPENDENCIES;

    /** The <code>serviceInputFormats</code> element. */
    public final QName SERVICE_INPUT_FORMATS;

    /** The <code>serviceLiveTestURL</code> element. */
    public final QName SERVICE_LIVE_TEST_URL;

    /** The <code>serviceMessagingProtocol</code> element. */
    public final QName SERVICE_MESSAGING_PROTOCOL;

    /** The <code>serviceProfile</code> element. */
    public final QName SERVICE_PROFILE;

    /** The <code>serviceOutputFormats</code> element. */
    public final QName SERVICE_OUTPUT_FORMATS;

    /** The <code>software</code> element. */
    public final QName SOFTWARE;

    //---
    // Attributes
    //---

    /** The <code>bDefPID</code> attribute. */
    public final QName BDEF_PID;

    /** The <code>name</code> attribute. */
    public final QName NAME;

    /** The <code>license</code> attribute. */
    public final QName LICENSE;

    /** The <code>opensource</code> attribute. */
    public final QName OPEN_SOURCE;

    /** The <code>type</code> attribute. */
    public final QName TYPE;

    /** The <code>version</code> attribute. */
    public final QName VERSION;

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FedoraServiceProfileNamespace ONLY_INSTANCE =
            new FedoraServiceProfileNamespace();

    /**
     * Constructs the instance.
     */
    private FedoraServiceProfileNamespace() {
        super("http://fedora.comm.nsdlib.org/service/profile", "fsvp");

        // elements
        MIME_TYPE = new QName(this, "MIMEType");
        SERVICE_DESCRIPTION = new QName(this, "serviceDescription");
        SERVICE_IMPLEMENTATION = new QName(this, "serviceImplementation");
        SERVICE_IMPL_DEPENDENCIES = new QName(this, "serviceImplDependencies");
        SERVICE_INPUT_FORMATS = new QName(this, "serviceInputFormats");
        SERVICE_LIVE_TEST_URL = new QName(this, "serviceLiveTestURL");
        SERVICE_MESSAGING_PROTOCOL =
                new QName(this, "serviceMessagingProtocol");
        SERVICE_PROFILE = new QName(this, "serviceProfile");
        SERVICE_OUTPUT_FORMATS = new QName(this, "serviceOutputFormats");
        SOFTWARE = new QName(this, "software");

        // attributes
        BDEF_PID = new QName(this, "bDefPID");
        NAME = new QName(this, "name");
        LICENSE = new QName(this, "license");
        OPEN_SOURCE = new QName(this, "opensource");
        TYPE = new QName(this, "type");
        VERSION = new QName(this, "version");
    }

    /**
     * Gets the only instance of this class.
     * 
     * @return the instance.
     */
    public static FedoraServiceProfileNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
