/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

import java.util.Hashtable;
import java.util.Set;

import org.apache.log4j.Logger;

import fedora.server.errors.GeneralException;

/**
 * Class that instantiates information parsed from the beSecurity.xml file.
 * Methods are provided to set and get backend security properties by role id.
 * 
 * @author payette
 */
public class BackendSecuritySpec {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(BackendSecuritySpec.class.getName());

    /**
     * The Hashtable is as follows: roleKey = the role identifier for the
     * backend service, for example: - "default" (the overall default for
     * backend services) - "sdep:9" (the role key for a backend service) -
     * "sdep:9/getThumb" (the role key for a method within a backend service) -
     * "fedoraInternalCall-1" (the role key for fedora calling back to itself)
     * VALUE = a Hashtable of security properties whose keys are defined in
     * BackendSecurityDeserializer.java as: -
     * BackendSecurityDeserializer.CALL_BASIC_AUTH -
     * BackendSecurityDeserializer.CALL_SSL -
     * BackendSecurityDeserializer.CALL_USERNAME -
     * BackendSecurityDeserializer.CALL_PASSWORD -
     * BackendSecurityDeserializer.CALLBACK_BASIC_AUTH -
     * BackendSecurityDeserializer.CALLBACK_SSL -
     * BackendSecurityDeserializer.IPLIST
     */
    private final Hashtable<String, Hashtable<String, String>> rolePropertiesTable;

    public BackendSecuritySpec() {
        rolePropertiesTable =
                new Hashtable<String, Hashtable<String, String>>();

    }

    /**
     * Set the security properties at the backend service or for a method of
     * that backend service.
     * 
     * @param serviceRoleID -
     *        the role identifier for a service. Valid values for this parameter
     *        are: - a sDep PID for a backend service - "default" to indicate
     *        the default properties for any service - "fedoraInternalCall-1"
     *        for Fedora calling back to itself as a service
     * @param methodName -
     *        optional method name within the backend service. If specified
     *        security properties at the service method level will be recorded.
     *        If null, service properties at the service level will be recorded.
     * @param properties
     */
    public void setSecuritySpec(String serviceRoleID,
                                String methodName,
                                Hashtable<String, String> properties)
            throws GeneralException {

        LOG.debug(">>>>>> setSecuritySpec: " + " serviceRoleID="
                + serviceRoleID + " methodName=" + methodName
                + " property count=" + properties.size());
        if (serviceRoleID == null || serviceRoleID.equals("")) {
            throw new GeneralException("serviceRoleID is missing.");
        }
        // if methodRoleID is missing, then set properties at the service level.
        if (methodName == null || methodName.equals("")) {
            rolePropertiesTable.put(serviceRoleID, properties);

            // otherwise set properties at the method level, but only if
            // parent service-level properties already exist.
        } else {
            Hashtable<String, String> serviceProps = rolePropertiesTable.get(serviceRoleID);
            if (serviceProps == null) {
                throw new GeneralException("Cannot add method-level security properties"
                        + " if there are no properties defined for the backend service that the "
                        + " method is part of. ");
            }
            String roleKey = serviceRoleID + "/" + methodName;
            rolePropertiesTable.put(roleKey, properties);
        }
    }

    /**
     * Get the default backend security properties.
     */
    public Hashtable<String, String> getDefaultSecuritySpec() {
        return rolePropertiesTable.get("default");
    }

    /**
     * Get security properties for either the a backend service or a method
     * within that backend service.
     * 
     * @param serviceRoleID -
     *        role identifier for a backend service. Valid options: - "default"
     *        (the overall default for backend services) -
     *        "fedoraInternalCall-1" (the role key for fedora calling back to
     *        itself) - A sDep PID (e.g., "sDep:9") as the identifier for a
     *        backend service
     * @param methodName -
     *        a method name that is specified within a sDep service. If values
     *        is null, then this method will return the security properties
     *        defined for the backend service specified by the serviceRoleID
     *        parameter.
     * @return a Hashtable containing the backend security properties for the
     *         role or role/method. The security property names, defined in
     *         BackendSecurityDeserializer.java, are: -
     *         BackendSecurityDeserializer.CALL_BASIC_AUTH -
     *         BackendSecurityDeserializer.CALL_SSL -
     *         BackendSecurityDeserializer.CALL_USERNAME -
     *         BackendSecurityDeserializer.CALL_PASSWORD -
     *         BackendSecurityDeserializer.CALLBACK_BASIC_AUTH -
     *         BackendSecurityDeserializer.CALLBACK_SSL -
     *         BackendSecurityDeserializer.IPLIST
     */
    public Hashtable<String, String> getSecuritySpec(String serviceRoleID, String methodName) {
        if (serviceRoleID == null || serviceRoleID.equals("")) {
            return getDefaultSecuritySpec();
        } else if (methodName == null || methodName.equals("")) {
            return rolePropertiesTable.get(serviceRoleID);
        } else {
            String roleKey = serviceRoleID + "/" + methodName;
            // First see if there is already a role key at the method level
            Hashtable<String, String> properties = rolePropertiesTable.get(roleKey);

            // if we did not find security properties for the method level,
            // roll up to the parent service level and get properties.
            if (properties == null) {
                properties = rolePropertiesTable.get(serviceRoleID);
            }

            // if we did not find method or service-level properties,
            // roll up the the default level and get default properties.
            if (properties == null) {
                properties = getDefaultSecuritySpec();
            }
            return properties;
        }
    }

    /**
     * Get security properties for either the a backend service or a method
     * within that backend service.
     * 
     * @param roleKey =
     *        the role identifier for the backend service, for example: -
     *        "default" (the overall default for backend services) -
     *        "fedoraInternalCall-1" (the role key for fedora calling back to
     *        itself) - "sDep:9" (the role key for a backend service) -
     *        "sDe:9/getThumb" (the role key for a method within a backend
     *        service)
     * @return a Hashtable containing the backend security properties for the
     *         roleKey. The security property names, defined in
     *         BackendSecurityDeserializer.java, are: -
     *         BackendSecurityDeserializer.CALL_BASIC_AUTH -
     *         BackendSecurityDeserializer.CALL_SSL -
     *         BackendSecurityDeserializer.CALL_USERNAME -
     *         BackendSecurityDeserializer.CALL_PASSWORD -
     *         BackendSecurityDeserializer.CALLBACK_BASIC_AUTH -
     *         BackendSecurityDeserializer.CALLBACK_SSL -
     *         BackendSecurityDeserializer.IPLIST
     */
    public Hashtable<String, String> getSecuritySpec(String roleKey) {
        return rolePropertiesTable.get(roleKey);

    }

    public Set<String> listRoleKeys() {
        return rolePropertiesTable.keySet();
    }
}
