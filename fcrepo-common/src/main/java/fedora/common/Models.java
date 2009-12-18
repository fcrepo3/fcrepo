/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.HashSet;
import java.util.Set;

import fedora.common.rdf.RDFName;

/**
 * Identities of the system content models.
 *
 * @author Aaron Birkland
 * @author Chris Wilper
 */
public abstract class Models {

    /**
     * The content model for Service Definition objects as of Fedora 3.0;
     * <code>info:fedora/fedora-system:ServiceDefinition-3.0</code>
     */
    public static final RDFName SERVICE_DEFINITION_3_0 =
            getSystemURI("ServiceDefinition-3.0");

    /**
     * The content model for Service Deployment objects as of Fedora 3.0;
     * <code>info:fedora/fedora-system:ServiceDeployment-3.0</code>
     */
    public static final RDFName SERVICE_DEPLOYMENT_3_0 =
            getSystemURI("ServiceDeployment-3.0");

    /**
     * The content model for Content Model objects as of Fedora 3.0;
     * <code>info:fedora/fedora-system:ContentModel-3.0</code>
     */
    public static final RDFName CONTENT_MODEL_3_0 =
            getSystemURI("ContentModel-3.0");

    /**
     * The basic content model for Fedora objects as of Fedora 3.0;
     * <code>info:fedora/fedora-system:FedoraObject-3.0</code>
     */
    public static final RDFName FEDORA_OBJECT_3_0 =
            getSystemURI("FedoraObject-3.0");

    /**
     * The basic content model for Fedora objects as the current release;
     * <code>info:fedora/fedora-system:FedoraObject-3.0</code>
     */
    public static final RDFName FEDORA_OBJECT_CURRENT =
            FEDORA_OBJECT_3_0;

    private static Set<String> m_values = new HashSet<String>();

    /*
     * Get a set of all string URI values for all public static fields exposed
     * above
     */
    static {
        for (Field field : Models.class.getFields()) {
            if (field.getType().equals(RDFName.class)
                    && Modifier.isPublic(field.getModifiers())) {
                try {
                    m_values.add(((RDFName) field.get(null)).uri);
                } catch (IllegalAccessException e) {
                    /* Never happen */
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static RDFName getSystemURI(String identity) {
        return new RDFName(Constants.FEDORA, "fedora-system:" + identity);
    }

    /**
     * Determine if any of the models defined herein match the given model uri.
     *
     * @param model
     *        URI of a content model;
     * @return true if the uri of one of the models matches the given uri.
     */
    public static boolean contains(String model) {
        return m_values.contains(model);
    }

    /**
     * Determine if the given model URI is a basic content model (current
     * or otherwise).
     *
     * @param model
     *        URI of a content model;
     * @return true if the given model URI denotes a basic content model.
     */
    public static boolean isBasicModel(String model) {
        return FEDORA_OBJECT_3_0.uri.equals(model);
    }
}
