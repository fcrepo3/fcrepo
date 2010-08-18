/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.security.xacml.pdp.data;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.net.URL;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.fcrepo.server.security.xacml.pdp.MelcoePDP;
import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;
import org.fcrepo.server.utilities.StreamUtility;

/**
 * A PolicyStore for managing policies stored as Fedora digital objects.
 *
 * For searching for policies see PolicyIndex.java and its implementations.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class FedoraPolicyStore
implements PolicyStore {

    private static final Logger log =
        LoggerFactory.getLogger(FedoraPolicyStore.class.getName());

    private static final String XACML20_POLICY_NS =
        "urn:oasis:names:tc:xacml:2.0:policy:schema:os";

    private static final String CONFIG_FILE = "config-pdm-fedora.xml";

    public static final String POLICY_DATASTREAM = "FESLPOLICY";

    public static String BOOTSTRAP_POLICY_NAMESPACE = "fedora-policy";


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

    private PolicyUtils utils;
    protected Server fedoraServer;

    protected Management apiMService;
    protected Access apiAService;


    protected FedoraPolicyStore()
    throws PolicyStoreException {

        // parse config file and set fields
        initConfig();

        try {
            this.fedoraServer =
                Server.getInstance(new File(Constants.FEDORA_HOME), false);
        } catch (Exception e) {
            throw new PolicyStoreException("Error initialising FedoraPolicyDataManager: "
                                                 + e.getMessage(),
                                                 e);
        }
        this.apiMService =
            (Management) fedoraServer
            .getModule("org.fcrepo.server.management.Management");
        this.apiAService =
            (Access) fedoraServer
            .getModule("org.fcrepo.server.access.Access");


        // if no pid namespace was specified, use the default specified in fedora.fcfg
        if (pidNamespace.equals("")) {
            pidNamespace = fedoraServer.getModule("org.fcrepo.server.storage.DOManager").getParameter("pidNamespace");
        }

        // check control group was supplied
        if (datastreamControlGroup.equals("")) {
            throw new PolicyStoreException("No control group for policy datastreams was specified in " + CONFIG_FILE);
        }

    }
    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.io.File)
     */
    public String addPolicy(File f) throws PolicyStoreException {
        return addPolicy(f, null);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.io.File, java.lang.String)
     */
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
    public String addPolicy(String document) throws PolicyStoreException {
        return addPolicy(document, null);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.lang.String, java.lang.String)
     */
    public String addPolicy(String document, String name)
    throws PolicyStoreException {

        try {
            utils.validate(document, name);
        } catch (MelcoePDPException e1) {
            throw new PolicyStoreException("Validation failed", e1);
        }

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
    public boolean updatePolicy(String name, String newDocument)
    throws PolicyStoreException {
        try {
            utils.validate(newDocument, name);
        } catch (MelcoePDPException e1) {
            throw new PolicyStoreException(e1);
        }

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
                                         POLICY_DATASTREAM,
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
                                                        POLICY_DATASTREAM,
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
                                                       POLICY_DATASTREAM,
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
        if (name.startsWith(BOOTSTRAP_POLICY_NAMESPACE + ":")) {
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

    private void initConfig() throws PolicyStoreException {
        if (log.isDebugEnabled()) {
            Runtime runtime = Runtime.getRuntime();
            log.debug("Total memory: " + runtime.totalMemory() / 1024);
            log.debug("Free memory: " + runtime.freeMemory() / 1024);
            log.debug("Max memory: " + runtime.maxMemory() / 1024);
        }

        try {
            String home = MelcoePDP.PDP_HOME.getAbsolutePath();

            String filename = home + "/conf/" + CONFIG_FILE;
            File f = new File(filename);
            if (!f.exists()) {
                throw new PolicyStoreException("Could not locate config file: "
                                                     + f.getAbsolutePath());
            }

            log.info("Loading config file: " + f.getAbsolutePath());

            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new FileInputStream(f));

            NodeList nodes = null;

            // get fedora policy object information
            nodes = doc.getElementsByTagName("object").item(0).getChildNodes();
            for (int x = 0; x < nodes.getLength(); x++) {
                Node node = nodes.item(x);
                if (node.getNodeName().equals("pid-namespace")) {
                    this.pidNamespace =
                        node.getAttributes().getNamedItem("name")
                        .getNodeValue();
                }
                if (node.getNodeName().equals("content-model")) {
                    this.contentModel =
                        node.getAttributes().getNamedItem("name")
                        .getNodeValue();
                }
                if (node.getNodeName().equals("datastream-control-group")) {
                    this.datastreamControlGroup =
                        node.getAttributes().getNamedItem("name")
                        .getNodeValue();
                }
                if (node.getNodeName().equals("collection")) {
                    this.collection =
                        node.getAttributes().getNamedItem("name")
                        .getNodeValue();
                }
                if (node.getNodeName().equals("collection-relationship")) {
                    this.collectionRelationship =
                        node.getAttributes().getNamedItem("name")
                        .getNodeValue();
                }
            }

            // get validation information
            Node schemaConfig =
                doc.getElementsByTagName("schemaConfig").item(0);
            nodes = schemaConfig.getChildNodes();
            if ("true".equals(schemaConfig.getAttributes()
                              .getNamedItem("validation").getNodeValue())) {
                log.info("Initialising validation");

                for (int x = 0; x < nodes.getLength(); x++) {
                    Node schemaNode = nodes.item(x);
                    if (schemaNode.getNodeType() == Node.ELEMENT_NODE) {
                        if (XACML20_POLICY_NS.equals(schemaNode.getAttributes()
                                                     .getNamedItem("namespace").getNodeValue())) {
                            if (log.isDebugEnabled()) {
                                log
                                .debug("found valid schema. Creating validator");
                            }
                            String loc =
                                schemaNode.getAttributes()
                                .getNamedItem("location")
                                .getNodeValue();
                            SchemaFactory schemaFactory =
                                SchemaFactory
                                .newInstance("http://www.w3.org/2001/XMLSchema");
                            Schema schema =
                                schemaFactory.newSchema(new URL(loc));
                            Validator validator = schema.newValidator();
                            utils = new PolicyUtils(validator);
                        }
                    }
                }
            } else {
                utils = new PolicyUtils();

            }
        } catch (Exception e) {
            log.error("Could not initialise DBXML: " + e.getMessage(), e);
            throw new PolicyStoreException("Could not initialise DBXML: "
                                                 + e.getMessage(), e);
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
        StringBuilder foxml = new StringBuilder();
        foxml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        foxml.append("<foxml:digitalObject VERSION=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        foxml.append("    xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"\n");
        foxml.append("           xsi:schemaLocation=\"" + Constants.FOXML.uri
                     + " " + Constants.FOXML1_1.xsdLocation + "\"");
        foxml.append("\n           PID=\"" + StreamUtility.enc(pid)
                         + "\">\n");
        foxml.append("  <foxml:objectProperties>\n");
        foxml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>\n");
        foxml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\""
                + StreamUtility.enc(label) + "\"/>\n");
        foxml.append("  </foxml:objectProperties>\n");


        // RELS-EXT specifying content model - if present, collection relationship if present
        // but not for bootstrap policies
        if (!pid.startsWith(BOOTSTRAP_POLICY_NAMESPACE + ":")) {
            if (!contentModel.equals("") || !collection.equals("")) {
                foxml.append("<foxml:datastream ID=\"RELS-EXT\" CONTROL_GROUP=\"X\">");
                foxml.append("<foxml:datastreamVersion FORMAT_URI=\"info:fedora/fedora-system:FedoraRELSExt-1.0\" ID=\"RELS-EXT.0\" MIMETYPE=\"application/rdf+xml\" LABEL=\"RDF Statements about this object\">");
                foxml.append("  <foxml:xmlContent>");
                foxml.append("   <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\" xmlns:rel=\"info:fedora/fedora-system:def/relations-external#\">");
                foxml.append("      <rdf:Description rdf:about=\"" + "info:fedora/"
                             + StreamUtility.enc(pid) + "\">");
                if (!contentModel.equals("")) {
                    foxml.append("        <fedora-model:hasModel rdf:resource=\""
                                 + StreamUtility.enc(contentModel) + "\"/>");
                }
                if (!collection.equals("")) {
                    foxml.append("        <rel:" + StreamUtility.enc(collectionRelationship) + " rdf:resource=\""
                                 + StreamUtility.enc(collection) + "\"/>");
                }
                foxml.append("       </rdf:Description>");
                foxml.append("      </rdf:RDF>");
                foxml.append("    </foxml:xmlContent>");
                foxml.append("  </foxml:datastreamVersion>");
                foxml.append("</foxml:datastream>");
            }
        }
        // the POLICY datastream
        foxml.append("<foxml:datastream ID=\"" + POLICY_DATASTREAM
                     + "\" CONTROL_GROUP=\"" + controlGroup + "\">");
        foxml.append("<foxml:datastreamVersion ID=\"POLICY.0\" MIMETYPE=\"text/xml\" LABEL=\"XACML policy datastream\">");
        if (controlGroup.equals("M")) {
            foxml.append("  <foxml:contentLocation REF=\"" + policyOrLocation
                         + "\" TYPE=\"URL\"/>");

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
