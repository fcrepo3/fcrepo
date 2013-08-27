/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.fcrepo.common.PID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Niebel
 */
public class BackendPolicies {

    private static final Logger logger =
            LoggerFactory.getLogger(BackendPolicies.class);

    public static final String FEDORA_INTERNAL_CALL = "fedoraInternalCall-1";

    public static final String BACKEND_SERVICE_CALL_UNSECURE =
            "fedoraInternalCall-2";

    private String inFilePath = null;

    private String outFilePath = null;

    private BackendSecuritySpec backendSecuritySpec = null;

    public BackendPolicies(String inFilePath, String outFilePath) {
        this.inFilePath = inFilePath;
        this.outFilePath = outFilePath;
    }

    public BackendPolicies(String inFilePath) {
        this(inFilePath, null);
    }

    public Hashtable<String, String> generateBackendPolicies() throws Exception {
        logger.debug("in BackendPolicies.generateBackendPolicies() 1");
        Hashtable<String, String> tempfiles = null;
        if (inFilePath.endsWith(".xml")) { // replacing code for .properties
            logger.debug("in BackendPolicies.generateBackendPolicies() .xml 1");
            BackendSecurityDeserializer bds =
                    new BackendSecurityDeserializer("UTF-8", false);
            logger.debug("in BackendPolicies.generateBackendPolicies() .xml 2");
            backendSecuritySpec = bds.deserialize(inFilePath);
            logger.debug("in BackendPolicies.generateBackendPolicies() .xml 3");
            tempfiles = writePolicies();
            logger.debug("in BackendPolicies.generateBackendPolicies() .xml 4");
        }
        return tempfiles;
    }

    private static final String[] parseForSlash(String key) throws Exception {
        int lastSlash = key.lastIndexOf("/");
        if (lastSlash + 1 == key.length()) {
            throw new Exception("BackendPolicies.newWritePolicies() "
                    + "can't handle key ending with '/'");
        }
        if (lastSlash != key.indexOf("/")) {
            throw new Exception("BackendPolicies.newWritePolicies() "
                    + "can't handle key containing multiple instances of '/'");
        }
        String[] parts = null;
        if (-1 < lastSlash && lastSlash < key.length()) {
            parts = key.split("/");
        } else {
            parts = new String[] {key};
        }
        return parts;
    }

    private static final String getExcludedRolesText(String key, Set<String> roles) {
        StringBuffer excludedRolesText = new StringBuffer();
        if ("default".equals(key) && roles.size() > 1) {
            excludedRolesText.append("\t\t<ExcludedRoles>\n");
            Iterator<String> excludedRoleIterator = roles.iterator();
            while (excludedRoleIterator.hasNext()) {
                logger.debug("in BackendPolicies.newWritePolicies() another inner it");
                String excludedRole = excludedRoleIterator.next();
                if ("default".equals(excludedRole)) {
                    continue;
                }
                logger.debug("in BackendPolicies.newWritePolicies() excludedRole="
                        + excludedRole);
                excludedRolesText.append("\t\t\t<ExcludedRole>");
                excludedRolesText.append(excludedRole);
                excludedRolesText.append("</ExcludedRole>\n");
            }
            excludedRolesText.append("\t\t</ExcludedRoles>\n");
        }
        return excludedRolesText.toString();
    }

    private static final String writeRules(String callbackBasicAuth,
                                           String callbackSsl,
                                           String iplist,
                                           String role,
                                           Set<String> roles) throws Exception {
        StringBuffer temp = new StringBuffer();
        temp.append("\t<Rule RuleId=\"1\" Effect=\"Permit\">\n");
        temp.append(getExcludedRolesText(role, roles));
        if ("true".equals(callbackBasicAuth)) {
            temp.append("\t\t<AuthnRequired/>\n");
        }
        if ("true".equals(callbackSsl)) {
            temp.append("\t\t<SslRequired/>\n");
        }
        logger.debug("DEBUGGING IPREGEX0 [" + iplist + "]");
        String[] ipRegexes = new String[0];
        if (iplist != null && !iplist.trim().isEmpty()) {
            ipRegexes = iplist.trim().split("\\s");
        }
        /*
         * if (ipRegexes.length == 1) { //fixup ipRegexes[0] =
         * ipRegexes[0].trim(); }
         */
        logger.debug("DEBUGGING IPREGEX1 [" + iplist.trim() + "]");
        if (ipRegexes.length != 0) {
            temp.append("\t\t<IpRegexes>\n");
            for (String element : ipRegexes) {
                logger.debug("DEBUGGING IPREGEX2 " + element);
                temp.append("\t\t\t<IpRegex>");
                temp.append(element);
                temp.append("</IpRegex>\n");
            }
            temp.append("\t\t</IpRegexes>\n");
        }
        temp.append("\t</Rule>\n");
        if ("true".equals(callbackBasicAuth) || "true".equals(callbackSsl)
                || ipRegexes.length != 0) {
            temp.append("\t<Rule RuleId=\"2\" Effect=\"Deny\">\n");
            temp.append(getExcludedRolesText(role, roles));
            temp.append("\t</Rule>\n");
        }

        return temp.toString();
    }

    private Hashtable<String, String> writePolicies() throws Exception {
        logger.debug("in BackendPolicies.newWritePolicies() 1");
        StringBuffer sb = null;
        Hashtable<String, String> tempfiles = new Hashtable<String, String>();
        Iterator<String> coarseIterator = backendSecuritySpec.listRoleKeys().iterator();
        while (coarseIterator.hasNext()) {
            String key = (String) coarseIterator.next();
            String[] parts = parseForSlash(key);
            String filename1 = "";
            String filename2 = "";
            switch (parts.length) {
                case 2:
                    filename2 = "-method-" + parts[1];
                    //break purposely absent:  fall through
                case 1:
                    if (-1 == parts[0].indexOf(":")) {
                        filename1 = "callback-by:" + parts[0];
                    } else {
                        filename1 = "callback-by-sdep-" + parts[0];
                    }
                    if ("".equals(filename2)) {
                        if (!"default".equals(parts[0])) {
                            filename2 = "-other-methods";
                        }
                    }
                    break;
                default:
                    //bad value
                    throw new Exception("BackendPolicies.newWritePolicies() "
                            + "didn't correctly parse key " + key);
            }
            sb = new StringBuffer();
            logger.debug("in BackendPolicies.newWritePolicies() another outer it, key={}", key);
            Hashtable<String,String> properties = backendSecuritySpec.getSecuritySpec(key);
            logger.debug("in BackendPolicies.newWritePolicies() properties.size()="
                            + properties.size());
            logger.debug("in BackendPolicies.newWritePolicies() properties.get(BackendSecurityDeserializer.ROLE)="
                            + properties.get(BackendSecurityDeserializer.ROLE));
            String callbackBasicAuth =
                    (String) properties
                            .get(BackendSecurityDeserializer.CALLBACK_BASIC_AUTH);
            if (callbackBasicAuth == null) {
                callbackBasicAuth = "false";
            }
            logger.debug("in BackendPolicies.newWritePolicies() CallbackBasicAuth="
                            + callbackBasicAuth);
            String callbackSsl =
                    (String) properties
                            .get(BackendSecurityDeserializer.CALLBACK_SSL);
            if (callbackSsl == null) {
                callbackSsl = "false";
            }
            String iplist =
                    (String) properties.get(BackendSecurityDeserializer.IPLIST);
            if (iplist == null) {
                iplist = "";
            }
            logger.debug("in BackendPolicies.newWritePolicies() coarseIplist="
                    + iplist);
            String id = "generated_for_" + key.replace(':', '-');
            logger.debug("in BackendPolicies.newWritePolicies() id=" + id);
            logger.debug("in BackendPolicies.newWritePolicies() " + filename1
                    + " " + filename2);
            String filename = filename1 + filename2; //was id.replace(':','-');
            logger.debug("in BackendPolicies.newWritePolicies() " + filename);
            PID tempPid = new PID(filename);
            logger.debug("in BackendPolicies.newWritePolicies() got PID "
                    + tempPid);
            filename = tempPid.toFilename();
            logger.debug("in BackendPolicies.newWritePolicies() filename="
                    + filename);
            sb
                    .append("<Policy xmlns=\"urn:oasis:names:tc:xacml:1.0:policy\" PolicyId=\""
                            + id + "\">\n");
            sb
                    .append("\t<Description>this policy is machine-generated at each Fedora server startup.  edit beSecurity.xml to change this policy.</Description>\n");
            sb.append("\t<Target>\n");
            sb.append("\t\t<Subjects>\n");
            if ("default".equals(key)) {
                sb.append("\t\t\t<AnySubject/>\n");
            } else {
                sb.append("\t\t\t<Subject>\n");
                sb.append("\t\t\t\t<SubjectMatch>\n");
                sb.append("\t\t\t\t\t<AttributeValue>" + key
                        + "</AttributeValue>\n");
                sb.append("\t\t\t\t</SubjectMatch>\n");
                sb.append("\t\t\t</Subject>\n");
            }
            sb.append("\t\t</Subjects>\n");
            sb.append("\t</Target>\n");

            String temp =
                    writeRules(callbackBasicAuth,
                               callbackSsl,
                               iplist,
                               key,
                               backendSecuritySpec.listRoleKeys());
            sb.append(temp);

            sb.append("</Policy>\n");
            logger.debug("\ndumping policy\n" + sb + "\n");
            File outfile = null;
            if (outFilePath == null) {
                outfile = File.createTempFile(filename, ".xml");
            } else {
                outfile =
                        new File(outFilePath + File.separator + filename
                                + ".xml");
            }
            tempfiles.put(filename + ".xml", outfile.getAbsolutePath());
            PrintStream pos = new PrintStream(new FileOutputStream(outfile));
            pos.println(sb);
            pos.close();
        }
        logger.debug("finished writing temp files");
        return tempfiles;
    }

    public static void main(String[] args) throws Exception {
        BackendPolicies backendPolicies = new BackendPolicies(args[0], args[1]);
        backendPolicies.generateBackendPolicies();
    }
}
