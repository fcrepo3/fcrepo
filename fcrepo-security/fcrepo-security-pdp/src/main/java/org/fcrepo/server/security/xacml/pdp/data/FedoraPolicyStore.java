/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.security.xacml.pdp.data;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.fcrepo.common.Constants;
import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.PID;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.access.Access;
import org.fcrepo.server.access.ObjectProfile;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.management.Management;
import org.fcrepo.server.security.PolicyParser;
import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.server.validation.ValidationUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * A PolicyStore for managing policies stored as Fedora digital objects.
 *
 * Mainly used by the initial load of the bootstrap policies and by the rebuilder.
 *
 * For searching for policies see PolicyIndex.java and its implementations.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class FedoraPolicyStore extends AbstractPolicyStore
implements PolicyStore {

    private static final Logger log =
        LoggerFactory.getLogger(FedoraPolicyStore.class.getName());

    private static final String XACML20_POLICY_NS =
        Constants.XACML2_POLICY_SCHEMA.OS.uri;

    public static final String FESL_POLICY_DATASTREAM = "FESLPOLICY";

    public static String FESL_BOOTSTRAP_POLICY_NAMESPACE = "fedora-policy";


    // escaping to use for ":" in policy names
    private static final String PID_SEPARATOR_ESCAPED = "%3A"; // = "__";


    private static final char[] hexChar = {
        '0' , '1' , '2' , '3' ,
        '4' , '5' , '6' , '7' ,
        '8' , '9' , 'A' , 'B' ,
        'C' , 'D' , 'E' , 'F'};

    // read from config file:
    private String pidNamespace = "";
    private String contentModel = "";
    private String datastreamControlGroup = "";
    private String collection = "";
    private String collectionRelationship = "";
    private boolean validateSchema = false;
    private Map<String,String> schemaLocations = null;

    private final PolicyUtils utils = new PolicyUtils();
    protected Server fedoraServer;

    protected Management apiMService;
    protected Access apiAService;


    public FedoraPolicyStore(Server server)
    throws PolicyStoreException {
        this.fedoraServer = server;
        this.apiMService = (Management)server.getBean("org.fcrepo.server.management.Management");
        this.apiAService = (Access)server.getBean("org.fcrepo.server.access.Access");
    }

    @Override
    public void init() throws PolicyStoreException, FileNotFoundException {
        if (log.isDebugEnabled()) {
            Runtime runtime = Runtime.getRuntime();
            log.debug("Total memory: " + runtime.totalMemory() / 1024);
            log.debug("Free memory: " + runtime.freeMemory() / 1024);
            log.debug("Max memory: " + runtime.maxMemory() / 1024);
        }
        super.init();
        // if no pid namespace was specified, use the default specified in fedora.fcfg
        if (pidNamespace.equals("")) {
            pidNamespace = fedoraServer.getModule("org.fcrepo.server.storage.DOManager").getParameter("pidNamespace");
        }

        // check control group was supplied
        if (datastreamControlGroup.equals("")) {
            throw new PolicyStoreException("No control group for policy datastreams was specified in FedoraPolicyStore configuration");
        }
        if (validateSchema) {
            String schemaLocation = schemaLocations.get(XACML20_POLICY_NS);
            if ( schemaLocation == null) {
                throw new PolicyStoreException("Configuration error - no policy schema specified");
            }
            try{
            String serverHome =
                    fedoraServer.getHomeDir().getCanonicalPath() + File.separator;

                String schemaPath =
                    ((schemaLocation)
                            .startsWith(File.separator) ? "" : serverHome)
                            + schemaLocation;
                FileInputStream in = new FileInputStream(schemaPath);
                PolicyParser policyParser = new PolicyParser(in);
                ValidationUtility.setFeslPolicyParser(policyParser);
            } catch (IOException ioe) {
                throw new PolicyStoreException(ioe.getMessage(),ioe);
            } catch (SAXException se) {
                throw new PolicyStoreException(se.getMessage(),se);
            }
        }
    }

    public void setPidNamespace( String pidNamespace ) {
        this.pidNamespace = pidNamespace;
    }
    public void setContentModel( String contentModel ) {
        this.contentModel = contentModel;
    }
    public void setDatastreamControlGroup(String datastreamControlGroup){
        this.datastreamControlGroup = datastreamControlGroup;
    }
    public void setCollection(String collection){
        this.collection = collection;
    }
    public void setCollectionRelationship(String collectionRelationship){
        this.collectionRelationship = collectionRelationship;
    }
    // schema config properties
    public void setSchemaValidation(boolean validate){
        this.validateSchema = validate;
        log.info("Initialising validation " + Boolean.toString(validate));
        ValidationUtility.setValidateFeslPolicy(validate);
    }

    /**
     * Map policy schema URIs to locations for the schema document
     * @param schemaLocation
     * @throws IOException
     * @throws SAXException
     */
    public void setSchemaLocations(Map<String,String> schemaLocation) throws IOException, SAXException{
        this.schemaLocations = schemaLocation;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.io.File)
     */
    @Override
    public String addPolicy(File f) throws PolicyStoreException {
        return addPolicy(f, null);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.io.File, java.lang.String)
     */
    @Override
    public String addPolicy(File f, String name)
    throws PolicyStoreException {
        try {
            return addPolicy(utils.fileToString(f), name);
        } catch (MelcoePDPException e) {
            throw new PolicyStoreException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.lang.String)
     */
    @Override
    public String addPolicy(String document) throws PolicyStoreException {
        return addPolicy(document, null);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.lang.String, java.lang.String)
     */
    @Override
    public String addPolicy(String document, String name)
    throws PolicyStoreException {

        String policyName;

        if (name == null || name.equals("")) {
            // no policy name, derive from document
            // (note: policy ID is mandatory according to schema)
            try {
                policyName = utils.getPolicyName(document);
            } catch (MelcoePDPException e) {
                throw new PolicyStoreException("Could not get policy name from policy", e);
            }
            // if name from document contains pid separator, escape it
            if (name.contains(":")) {
                name = name.replace(":", PID_SEPARATOR_ESCAPED);
            }

        } else {
            policyName = name;
        }
        String pid = this.getPID(policyName);

        ObjectProfile objectProfile = null;
        try {
            objectProfile =
                this.apiAService.getObjectProfile(getContext(), pid, null);
        } catch (ObjectNotInLowlevelStorageException e) {
        } catch (ServerException e) {
            throw new PolicyStoreException("Add: error getting object profile for "
                                                 + pid
                                                 + " - "
                                                 + e.getMessage(),
                                                 e);
        }

        if (objectProfile != null) {
            // object exists, check state
            if (objectProfile.objectState != "D") {
                throw new PolicyStoreException("Add:  attempting to add policy "
                                                     + pid + " but it already exists");
            }
            // deleted object:  set state to active and do an update instead
            try {
                this.apiMService
                .modifyObject(getContext(),
                              pid,
                              "A",
                              objectProfile.objectLabel,
                              objectProfile.objectOwnerId,
                "Fedora policy manager:  Adding policy by activating deleted object",null);
            } catch (ServerException e) {
                throw new PolicyStoreException("Add: " + e.getMessage(),
                                                     e);
            }
            this.updatePolicy(policyName, document);
            return pid;
        } else {
            // create new object

            // if control group is M - managed - we need a temp location for the datastream
            String dsLocationOrContent = null;
            if (datastreamControlGroup.equals("M")) {
                try {
                    ByteArrayInputStream is =
                        new ByteArrayInputStream(document.getBytes("UTF-8"));
                    dsLocationOrContent =
                        apiMService.putTempStream(getContext(), is);
                } catch (Exception e) {
                    throw new PolicyStoreException("Add: error generating temp datastream location - "
                                                         + e
                                                         .getMessage(),
                                                         e);
                }
            } else {
                dsLocationOrContent = document;
            }

            try {
                return apiMService
                .ingest(getContext(),
                        new ByteArrayInputStream(getFOXMLPolicyTemplate(pid,
                                                                        "XACML policy " + policyName,
                                                                        contentModel,
                                                                        collection,
                                                                        collectionRelationship,
                                                                        dsLocationOrContent,
                                                                        datastreamControlGroup)
                                                                        .getBytes("UTF-8")),
                                                                        "Fedora Policy Manager creating policy",
                                                                        Constants.FOXML1_1.uri,
                                                                        "UTF-8",
                                                                        "");
            } catch (Exception e) {
                throw new PolicyStoreException("Add: error ingesting "
                                                     + pid + " - " + e.getMessage(), e);
            }

        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#deletePolicy
     * (java.lang.String)
     */
    @Override
    public boolean deletePolicy(String name) throws PolicyStoreException {
        String pid = this.getPID(name);
        if (!contains(name)) {
            throw new PolicyStoreException("Delete: object " + pid
                                                 + " not found.");
        }
        try {
            this.apiMService.modifyObject(getContext(),
                                          pid,
                                          "D",
                                          null,
                                          null,
                                          "Deleting policy " + pid,
                                          null);
        } catch (ServerException e) {
            throw new PolicyStoreException("Delete: error deleting policy "
                                                 + pid
                                                 + " - "
                                                 + e.getMessage(),
                                                 e);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#updatePolicy
     * (java.lang.String, java.lang.String)
     */
    @Override
    public boolean updatePolicy(String name, String newDocument)
    throws PolicyStoreException {

        String pid = this.getPID(name);

        if (!contains(name)) {
            throw new PolicyStoreException("Update:  policy " + pid
                                                 + " not found");
        }

        if (datastreamControlGroup.equals("X")) {
            // inline, modify by value
            try {
                this.apiMService
                .modifyDatastreamByValue(getContext(),
                                         pid,
                                         FESL_POLICY_DATASTREAM,
                                         null,
                                         null,
                                         null,
                                         null,
                                         new ByteArrayInputStream(newDocument
                                                                  .getBytes("UTF-8")),
                                                                  "DISABLED",
                                                                  null,
                                                                  "Modifying policy " + pid,
                                                                  null);
            } catch (Exception e) {
                throw new PolicyStoreException("Update:  error modifying datastream by value for "
                                                     + pid
                                                     + " - "
                                                     + e.getMessage(),
                                                     e);
            }

        } else if (datastreamControlGroup.equals("M")) {
            // managed, generate temp location, modify by reference
            String dsLocation = null;
            try {
                ByteArrayInputStream is =
                    new ByteArrayInputStream(newDocument.getBytes("UTF-8"));
                dsLocation = apiMService.putTempStream(getContext(), is);
            } catch (Exception e) {
                throw new PolicyStoreException("Update: error generating temp datastream location - "
                                                     + e.getMessage(),
                                                     e);
            }
            try {
                apiMService.modifyDatastreamByReference(getContext(),
                                                        pid,
                                                        FESL_POLICY_DATASTREAM,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        dsLocation,
                                                        "DISABLED",
                                                        null,
                                                        "Modifying policy "
                                                        + pid,
                                                        null);
            } catch (ServerException e) {
                throw new PolicyStoreException("Update:  error modifying datastream by reference for "
                                                     + pid
                                                     + " - "
                                                     + e.getMessage(),
                                                     e);
            }

        } else {
            throw new PolicyStoreException("Update:  Invalid datastream control group "
                                                 + datastreamControlGroup + " - use M or X");
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#getPolicy
     * (java.lang.String)
     */
    @Override
    public byte[] getPolicy(String name) throws PolicyStoreException {
        String pid = getPID(name);
        if (!contains(name)) {
            throw new PolicyStoreException("Get: policy " + pid
                                                 + " does not exist.");
        }

        try {
            InputStream is =
                apiAService.getDatastreamDissemination(getContext(),
                                                       pid,
                                                       FESL_POLICY_DATASTREAM,
                                                       null).getStream();
            return IOUtils.toByteArray(is);
        } catch (Exception e) {
            throw new PolicyStoreException("Get: error reading policy "
                                                 + pid + " - " + e.getMessage(), e);
        }

    }

    /**
     * Check if the policy identified by policyName exists.
     *
     * @param policyName
     * @return true iff the policy store contains a policy identified as
     *         policyName
     * @throws PolicyStoreException
     */
    @Override
    public boolean contains(String policyName)
    throws PolicyStoreException {

        // search for policy - active and inactive
        String pid = this.getPID(policyName);

        ObjectProfile objectProfile = null;
        try {
            objectProfile =
                this.apiAService.getObjectProfile(getContext(), pid, null);
        } catch (ObjectNotInLowlevelStorageException e) {
        } catch (ServerException e) {
            throw new PolicyStoreException("Add: error getting object profile for "
                                                 + pid
                                                 + " - "
                                                 + e.getMessage(),
                                                 e);

        }

        if (objectProfile == null) {
            // no object found
            return false;
        } else {
            if (objectProfile.objectState.equals("A")
                    || objectProfile.objectState.equals("I")) {
                // active or inactive object found - policy exists
                return true;
            } else {
                // deleted object - policy does not exist
                return false;
            }
        }
    }

    /**
     * Check if the policy identified by policyName exists.
     *
     * @param policy
     * @return true iff the policy store contains a policy with the same
     *         PolicyId
     * @throws PolicyStoreException
     */
    @Override
    public boolean contains(File policy) throws PolicyStoreException {

        try {
            return contains(utils.getPolicyName(policy));
        } catch (MelcoePDPException e) {
            throw new PolicyStoreException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#listPolicies
     * ()
     */
    @Override
    public List<String> listPolicies() throws PolicyStoreException {
        // not implemented (is it ever used?)

        return null;
    }

    /**
     * Given a policy name (corresponds to Policy identifier in XACML), generate a PID
     *
     * If the name already contains a pid namespace, use that, otherwise use the default
     *
     * @param name
     * @return normalized name (a PID)
     * @throws PolicyStoreException
     * @throws MalformedPIDException
     */
    private String getPID(String name) throws PolicyStoreException {

        String pid;
        // only bootstrap policies specify the PID namespace, all others follow the config
        if (name.startsWith(FESL_BOOTSTRAP_POLICY_NAMESPACE + ":")) {
            pid = name;
        } else {
            // TODO: would be nice to have the PID class contain a method for this
            // (could be used as a generic method instead of the from-filename etc methods)

            // name might contain non-legal PID characters so encode them
            StringBuffer out = new StringBuffer();
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                // valid pid characters
                if (isAlphaNum(c) || c == '-' || c == '.' || c == '~'
                    || c == '_') {
                out.append(c);
                } else {
                    // do each byte
                    // FIXME: percent-encoding causing various issues
                    // (not least with web admin client)
                    out.append("_");
                    /*
                    byte[] bytes;
                    try {
                        bytes = Character.toString(c).getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        // should never happen
                        throw new RuntimeException(e);
                    }
                    for (byte b : bytes ) {
                        // percent-encode the byte
                        out.append("%");
                        out.append(hexChar[(b >>> 4) & 0xf]);
                        out.append(hexChar[b & 0xf]);
                    }
                    */
                }
            }
            pid = pidNamespace + ":" + out.toString();
        }

        try {
            // just in case...
            return PID.normalize(pid);
        } catch (MalformedPIDException e) {
            throw new PolicyStoreException("Invalid policy name '" + name
                                                 + "'.  Could not create a valid PID from this name: " + e.getMessage(), e);
        }

    }

    private Context getContext() throws PolicyStoreException {

        try {
            return ReadOnlyContext.getContext(null,
                                              "fedoraBootstrap",
                                              null,
                                              false);
        } catch (Exception e) {
            throw new PolicyStoreException(e.getMessage(), e);
        }
    }

    /**
     * Generate FOXML for a new policy object
     * @param pid
     * @param label
     * @param contentModel
     * @param policyOrLocation
     * @param controlGroup
     * @return
     * @throws PolicyStoreException
     */
    private static String getFOXMLPolicyTemplate(String pid,
                                                 String label,
                                                 String contentModel,
                                                 String collection,
                                                 String collectionRelationship,
                                                 String policyOrLocation,
                                                 String controlGroup)
    throws PolicyStoreException {
        StringBuilder foxml = new StringBuilder(1024);
        foxml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        foxml.append("<foxml:digitalObject VERSION=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        foxml.append("    xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"\n");
        foxml.append("           xsi:schemaLocation=\"" + Constants.FOXML.uri
                     + " " + Constants.FOXML1_1.xsdLocation + "\"");
        foxml.append("\n           PID=\"");
        StreamUtility.enc(pid, foxml);
        foxml.append("\">\n  <foxml:objectProperties>\n");
        foxml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>\n");
        foxml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\"");
        StreamUtility.enc(label, foxml);
        foxml.append("\"/>\n  </foxml:objectProperties>\n");


        // RELS-EXT specifying content model - if present, collection relationship if present
        // but not for bootstrap policies
        if (!pid.startsWith(FESL_BOOTSTRAP_POLICY_NAMESPACE + ":")) {
            if (!contentModel.equals("") || !collection.equals("")) {
                foxml.append("<foxml:datastream ID=\"RELS-EXT\" CONTROL_GROUP=\"X\">");
                foxml.append("<foxml:datastreamVersion FORMAT_URI=\"info:fedora/fedora-system:FedoraRELSExt-1.0\" ID=\"RELS-EXT.0\" MIMETYPE=\"application/rdf+xml\" LABEL=\"RDF Statements about this object\">");
                foxml.append("  <foxml:xmlContent>");
                foxml.append("   <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\" xmlns:rel=\"info:fedora/fedora-system:def/relations-external#\">");
                foxml.append("      <rdf:Description rdf:about=\"" + "info:fedora/");
                StreamUtility.enc(pid, foxml);
                foxml.append("\">");
                if (!contentModel.equals("")) {
                    foxml.append("        <fedora-model:hasModel rdf:resource=\"");
                    StreamUtility.enc(contentModel, foxml);
                    foxml.append("\"/>");
                }
                if (!collection.equals("")) {
                    foxml.append("        <rel:");
                    StreamUtility.enc(collectionRelationship, foxml);
                    foxml.append(" rdf:resource=\"");
                    StreamUtility.enc(collection, foxml);
                    foxml.append("\"/>");
                }
                foxml.append("       </rdf:Description>");
                foxml.append("      </rdf:RDF>");
                foxml.append("    </foxml:xmlContent>");
                foxml.append("  </foxml:datastreamVersion>");
                foxml.append("</foxml:datastream>");
            }
        }
        // the POLICY datastream
        foxml.append("<foxml:datastream ID=\"" + FESL_POLICY_DATASTREAM
                     + "\" CONTROL_GROUP=\"" + controlGroup + "\">");
        foxml.append("<foxml:datastreamVersion ID=\"POLICY.0\" MIMETYPE=\"text/xml\" LABEL=\"XACML policy datastream\">");
        if (controlGroup.equals("M")) {
            foxml.append("  <foxml:contentLocation REF=\"" + policyOrLocation
                         + "\" TYPE=\"" + org.fcrepo.server.storage.types.Datastream.DS_LOCATION_TYPE_URL + "\"/>");

        } else if (controlGroup.equals("X")) {
            foxml.append("  <foxml:xmlContent>");
            foxml.append(policyOrLocation);
            foxml.append("    </foxml:xmlContent>");

        } else {
            throw new PolicyStoreException("Generating new object XML:  Invalid control group: "
                                                 + controlGroup + " - use X or M.");
        }
        foxml.append("  </foxml:datastreamVersion>");
        foxml.append("</foxml:datastream>");
        foxml.append("</foxml:digitalObject>");

        return foxml.toString();
    }

    private static boolean isAlphaNum(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A'
                && c <= 'Z';
    }


}