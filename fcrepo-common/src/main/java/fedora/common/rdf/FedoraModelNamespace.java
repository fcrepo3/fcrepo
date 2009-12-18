/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

import fedora.common.Models;

/**
 * The Fedora Model RDF namespace.
 *
 * <pre>
 * Namespace URI    : info:fedora/fedora-system:def/model#
 * Preferred Prefix : fedora-model
 * </pre>
 *
 * @author Chris Wilper
 * @version $Id$
 */
public class FedoraModelNamespace
        extends RDFNamespace {

    private static final long serialVersionUID = 2L;

    // Properties

    public final RDFName ALT_IDS;

    /**
     * Deprecated as of Fedora 3.0. Replaced by HAS_CONTENT_MODEL.
     */
    @Deprecated
    public final RDFName CONTENT_MODEL;

    public final RDFName CONTROL_GROUP;

    public final RDFName CREATED_DATE;

    public final RDFName DEFINES_METHOD;

    public final RDFName DIGEST;

    public final RDFName DIGEST_TYPE;

    /**
     * Deprecated as of Fedora 3.0. No replacement. This information is no
     * longer recorded.
     */
    @Deprecated
    public final RDFName DEPENDS_ON;

    public final RDFName EXT_PROPERTY;

    public final RDFName FORMAT_URI;

    /**
     * Deprecated as of Fedora 3.0. Replaced by HAS_BDEF.
     */
    @Deprecated
    public final RDFName IMPLEMENTS_BDEF;

    public final RDFName LABEL;

    public final RDFName LENGTH;

    public final RDFName OWNER;

    public final RDFName STATE;

    public final RDFName DOWNLOAD_FILENAME;

    /**
     * Deprecated as of Fedora 3.0. No direct replacement. Objects now point to
     * content models via HAS_CMODEL. Service Deployments used by an object are
     * those that point to the content model of the object via IS_CONTRACTOR_OF.
     */
    @Deprecated
    public final RDFName USES_BMECH;

    public final RDFName VERSIONABLE;

    // Values
    public final RDFName ACTIVE;

    public final RDFName DELETED;

    public final RDFName INACTIVE;

    // CMA RDF Relationships
    public final RDFName HAS_SERVICE;

    public final RDFName IS_CONTRACTOR_OF;

    public final RDFName IS_DEPLOYMENT_OF;

    public final RDFName HAS_MODEL;

    // Pre 3.0 object types

    /**
     * Behavior Definition Object, in pre-3.0 terminology.
     * <p>
     * In 3.0, an objects "typeness" is determined by its content model. What
     * used to be known as BDef objects in Fedora 2.x are analogous to objects
     * in the {@link Models#SERVICE_DEFINITION_3_0} model in Fedora 3.0.
     * </p>
     *
     * @deprecated
     */
    @Deprecated
    public final RDFName BDEF_OBJECT;

    /**
     * Behavior Mechanism Object, in pre-3.0 terminology.
     * <p>
     * In 3.0, an objects "typeness" is determined by its content model. What
     * used to be known as BMech objects in Fedora 2.x are analogous to objects
     * in the {@link Models#SERVICE_DEPLOYMENT_3_0} model in Fedora 3.0.
     * </p>
     *
     * @deprecated
     */
    @Deprecated
    public final RDFName BMECH_OBJECT;

    /**
     * Data Object, in pre-3.0 terminology.
     * <p>
     * In 3.0, an objects "typeness" is determined by its content model. What
     * used to be known as data objects in Fedora 2.x are analogous to objects
     * in the {@link Models#FEDORA_OBJECT_3_0} model in Fedora 3.0.
     * </p>
     *
     * @deprecated
     */
    @Deprecated
    public final RDFName DATA_OBJECT;


    public FedoraModelNamespace() {

        uri = "info:fedora/fedora-system:def/model#";
        prefix = "fedora-model";

        // Properties
        ALT_IDS = new RDFName(this, "altIds");
        CONTENT_MODEL = new RDFName(this, "contentModel");
        CONTROL_GROUP = new RDFName(this, "controlGroup");
        CREATED_DATE = new RDFName(this, "createdDate");
        DEFINES_METHOD = new RDFName(this, "definesMethod");
        DEPENDS_ON = new RDFName(this, "dependsOn");
        DIGEST = new RDFName(this, "digest");
        DIGEST_TYPE = new RDFName(this, "digestType");
        EXT_PROPERTY = new RDFName(this, "extProperty");
        FORMAT_URI = new RDFName(this, "formatURI");
        IMPLEMENTS_BDEF = new RDFName(this, "implementsBDef");
        LABEL = new RDFName(this, "label");
        LENGTH = new RDFName(this, "length");

        OWNER = new RDFName(this, "ownerId");
        STATE = new RDFName(this, "state");
        VERSIONABLE = new RDFName(this, "versionable");
        DOWNLOAD_FILENAME = new RDFName(this, "downloadFilename");

        // Values
        ACTIVE = new RDFName(this, "Active");
        DELETED = new RDFName(this, "Deleted");
        INACTIVE = new RDFName(this, "Inactive");

        // CMA RDF Relationships
        HAS_SERVICE = new RDFName(this, "hasService");
        IS_DEPLOYMENT_OF = new RDFName(this, "isDeploymentOf");
        IS_CONTRACTOR_OF = new RDFName(this, "isContractorOf");
        HAS_MODEL = new RDFName(this, "hasModel");

        // Types
        BDEF_OBJECT = new RDFName(this, "FedoraBDefObject");

        BMECH_OBJECT = new RDFName(this, "FedoraBMechObject");
        DATA_OBJECT = new RDFName(this, "FedoraObject");

        USES_BMECH = new RDFName(this, "usesBMech");
    }
}
