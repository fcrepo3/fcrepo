/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.rest;

import java.io.File;

import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.Server;
import fedora.server.access.Access;
import fedora.server.management.Management;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.Property;
import fedora.server.storage.types.RelationshipTuple;

/**
 * Helper class to manage getting filenames for datastreams and adding content
 * disposition headers to force downloading of datastream disseminations
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class DatastreamFilenameHelper {
    private static final Logger LOG = Logger.getLogger(DatastreamFilenameHelper.class.getName());

    // configures behaviour from fedora.fcfg
    protected static String m_datastreamContentDispositionInlineEnabled;
    protected static String m_datastreamFilenameSource;
    protected static String m_datastreamExtensionMappingLabel;
    protected static String m_datastreamExtensionMappingId;
    protected static String m_datastreamExtensionMappingRels;
    protected static String m_datastreamExtensionMappingDefault;
    protected static String m_datastreamDefaultFilename;
    protected static String m_datastreamDefaultExtension;

    // TODO: in the future this could be (in) a Fedora object.  For now it's a file in the server config directory
    private static String DATASTREAM_MAPPING_SOURCE_FILE = "mime-to-extensions.xml";

    // uri used to assert the download filename name
    protected static String FILENAME_REL = Constants.MODEL.DOWNLOAD_FILENAME.uri;

    // characters that we don't allow in filenames.
    // main:  \ / * ? < > : | "
    // other characters that can cause problems: ; , % # $
    protected static Pattern ILLEGAL_FILENAME_REGEX= Pattern.compile("[\\\\/\\*\\?<>:\\|\";,%#\\$]+");

    protected Management m_apiMService;
    protected Access m_apiAService;

    // MIMETYPE-to-extensions mappings
    protected static HashMap<String, String> m_extensionMappings;

    /**
     * Read configuration information from fedora.fcfg
     *
     * @param fedoraServer
     * @param management
     * @param access
     */
    public DatastreamFilenameHelper(Server fedoraServer, Management management, Access access) {
        m_datastreamContentDispositionInlineEnabled = fedoraServer.getParameter("datastreamContentDispositionInlineEnabled");
        m_datastreamFilenameSource = fedoraServer.getParameter("datastreamFilenameSource");
        m_datastreamExtensionMappingLabel = fedoraServer.getParameter("datastreamExtensionMappingLabel");
        m_datastreamExtensionMappingId = fedoraServer.getParameter("datastreamExtensionMappingId");
        m_datastreamExtensionMappingRels = fedoraServer.getParameter("datastreamExtensionMappingRels");
        m_datastreamExtensionMappingDefault = fedoraServer.getParameter("datastreamExtensionMappingDefault");
        m_datastreamDefaultFilename = fedoraServer.getParameter("datstreamDefaultFilename");
        m_datastreamDefaultExtension = fedoraServer.getParameter("datastreamDefaultExtension");

        m_apiMService = management;
        m_apiAService = access;
    }
    /**
     * Get the file extension for a given MIMETYPE from the extensions mappings
     *
     * @param MIMETYPE
     * @return
     * @throws Exception
     */
    private static final String getExtension(String MIMETYPE) throws Exception {
        if (m_extensionMappings == null) {
            m_extensionMappings = readExtensionMappings(Server.FEDORA_HOME + "/server/" + Server.CONFIG_DIR + "/" + DATASTREAM_MAPPING_SOURCE_FILE);
        }

        String extension = m_extensionMappings.get(MIMETYPE);
        if (extension != null) {
            return extension;
        } else {
            return "";
        }
    }

    /**
     * Read the extensions mappings from config file
     *
     * @returns new HashMap of mime-types to extensions mappings
     * @throws Exception
     */
    private static synchronized final HashMap<String, String> readExtensionMappings(String mappingFile) throws Exception {

        HashMap<String, String> extensionMappings = new HashMap<String, String>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new File(mappingFile));
        Element root = doc.getDocumentElement();
        // "mime-mapping" elements are children of root
        NodeList mappingNodes = root.getChildNodes();
        for (int i = 0; i < mappingNodes.getLength(); i++) {
            Node mappingNode = mappingNodes.item(i);
            // check it's a mime-mapping element
            if (mappingNode.getNodeType() == Node.ELEMENT_NODE && mappingNode.getNodeName().equals("mime-mapping")) {
                // look for child elements "extension" and "mime-type" and get values from text nodes
                String extension = null;
                String mimeType = null;
                NodeList nl = mappingNode.getChildNodes();
                for (int j = 0; j < nl.getLength(); j++) {
                    Node n = nl.item(j);
                    if (n.getNodeType() == Node.ELEMENT_NODE) {
                        // assuming here that extension and mime-type nodes only have text content (no attrs etc)
                        if (n.getNodeName().equals("extension")) {
                            extension = n.getFirstChild().getNodeValue();
                        } else {
                            if (n.getNodeName().equals("mime-type")) {
                                mimeType = n.getFirstChild().getNodeValue();
                            }
                        }
                    }
                }
                // if we've got an extension and mime-type, and it's not in the list, add it.
                if (extension != null && mimeType != null) {
                    if (!extensionMappings.containsKey(mimeType)) {
                        extensionMappings.put(mimeType, extension);
                    } else {
                        LOG.warn("Duplicate extension " + extension + " found for mime-type " + mimeType + " in " + mappingFile);
                    }
                } else {
                    LOG.warn("Element mime-mapping is missing child elements mime-type and/or extension in " + mappingFile);
                }
            }
        }
        return extensionMappings;
    }

    /**
     * Add a content disposition header to a MIMETypedStream based on configuration preferences.
     * Header by default specifies "inline"; if download=true then "attachment" is specified.
     *
     * @param context
     * @param pid
     * @param dsID
     * @param download
     *          true if file is to be downloaded
     * @param asOfDateTime
     * @param stream
     * @throws Exception
     */
    public final void addContentDispositionHeader(Context context, String pid, String dsID, String download, Date asOfDateTime, MIMETypedStream stream) throws Exception {
        String headerValue = null;
        String filename = null;

        // is downloading requested?
        if (download != null && download.equals("true")) {
            // generate an "attachment" content disposition header with the filename
            filename = getFilename(context, pid, dsID, asOfDateTime, stream.MIMEType);
            headerValue="attachment; filename=\"" + filename + "\"";
        } else {
            // is the content disposition header enabled in the case of not downloading?
            if (m_datastreamContentDispositionInlineEnabled.equals("true")) {
                // it is... generate the header with "inline"
                filename = getFilename(context, pid, dsID, asOfDateTime, stream.MIMEType);
                headerValue="inline; filename=\"" + filename + "\"";
            }
        }
        // create content disposition header to add
        Property[] header = { new Property ("content-disposition", headerValue) };
        // add header to existing headers if present, or set this as the new header if not
        if (stream.header != null) {
            Property headers[] = new Property[stream.header.length + 1];
            System.arraycopy(stream.header, 0, headers, 0, stream.header.length);
            headers[headers.length - 1] = header[0];
            stream.header = headers;
        } else {
            stream.header = header;
        }

    }

    /**
     * Generate a filename and extension for a datastream based on configuration preferences.
     * Filename can be based on a definition in RELS-INT, the datastream label or
     * the datastream ID.  These sources can be specified in order of preference together with
     * using a default filename.  The extension is based on a mime-type-to-extension mapping
     * configuration file; alternatively if the filename determined already includes an extension
     * that can be specified instead.
     *
     * @param context
     * @param pid
     * @param dsid
     * @param asOfDateTime
     * @param MIMETYPE
     * @return
     * @throws Exception
     */
    private final String getFilename(Context context, String pid, String dsid, Date asOfDateTime, String MIMETYPE) throws Exception {
        String filename = "";
        String extension = "";

        // check sources in order
        for (String source : m_datastreamFilenameSource.split(" ")) {
            // try and get filename and extension from specified source
            if (source.equals("rels")) {
                filename = getFilenameFromRels(context, pid, dsid, MIMETYPE);
                if (!filename.equals(""))
                    extension = getExtension(filename, m_datastreamExtensionMappingRels, MIMETYPE);
            } else {
                if (source.equals("id")) {
                    filename = getFilenameFromId(pid, dsid, MIMETYPE);
                    if (!filename.equals(""))
                        extension = getExtension(filename, m_datastreamExtensionMappingId, MIMETYPE);

                } else {
                    if (source.equals("label")) {
                        filename = getFilenameFromLabel(context, pid, dsid, asOfDateTime, MIMETYPE);
                        if (!filename.equals(""))
                            extension = getExtension(filename, m_datastreamExtensionMappingLabel, MIMETYPE);
                    } else {
                        LOG.warn("Unknown datastream filename source specified in datastreamFilenameSource in fedora.fcfg: " + source + ". Please specify zero or more of: rels id label");
                    }
                }
            }
            // if we've got one by here, quit loop
            if (!filename.equals(""))
                break;
        }
        // if not determined from above use the default
        if (filename.equals("")) {
            filename = m_datastreamDefaultFilename;
            extension = getExtension(m_datastreamDefaultFilename, m_datastreamExtensionMappingDefault, MIMETYPE);
        }

        // clean up filename - remove illegal chars
        if (extension.equals("")) {
            return ILLEGAL_FILENAME_REGEX.matcher(filename).replaceAll("");
        } else {
            return ILLEGAL_FILENAME_REGEX.matcher(filename + "." + extension).replaceAll("");
        }

    }
    /**
     * Get datastream filename as defined in RELS-INT
     *
     * @param context
     * @param pid
     * @param dsid
     * @param MIMETYPE
     * @return
     * @throws Exception
     */
    private  final String getFilenameFromRels(Context context, String pid, String dsid, String MIMETYPE) throws Exception {
        String filename = null;
        // find any "has filename" relationships with the datastream URI as the subject
        // TODO: this is based on the current state of RELS-INT; in theory it should use the state at asOfDateTime, but we don't
        // yet have the API-M calls to support this...
        RelationshipTuple[] filenameRels = m_apiMService.getRelationships(context, Constants.FEDORA.uri + pid + "/" + dsid, FILENAME_REL);
        // should only be 0 or 1
        if (filenameRels.length == 1) {
            if (filenameRels[0].isLiteral) {
                filename = filenameRels[0].object;
            } else {
                LOG.warn("Object " + pid + " datastream " + dsid + " specifies a filename which is not a literal in RELS-INT");
                filename = "";
            }
        } else {
            if (filenameRels.length > 1) {
                LOG.warn("Object " + pid + " datastream " + dsid + " specifies more than one filename in RELS-INT");
            }
            filename = "";
        }
        return filename;

    }
    /**
     * Get filename based on datastream label
     *
     * @param context
     * @param pid
     * @param dsid
     * @param asOfDateTime
     * @param MIMETYPE
     * @return
     * @throws Exception
     */
    private final String getFilenameFromLabel(Context context, String pid, String dsid, Date asOfDateTime, String MIMETYPE) throws Exception {
        Datastream ds = m_apiMService.getDatastream(context, pid, dsid, asOfDateTime);
        String filename = ds.DSLabel;
        return filename;

    }
    /**
     * Get filename from datastream id
     *
     * @param pid
     * @param dsid
     * @param MIMETYPE
     * @return
     * @throws Exception
     */
    private static final String getFilenameFromId(String pid, String dsid, String MIMETYPE) throws Exception {
        return dsid;
    }

    /**
     * Get a filename extension for a datastream based on mime-type to extension mapping.
     * mappingType may be:
     * <li>never: never look up extension
     * <li>ifmissing: if the given filename already contains an extension return nothing,
     *      otherwise look up an extension
     * <li>always: always look up an extension
     */
    private static final String getExtension(String filename, String mappingType, String MIMETYPE) throws Exception {
        String extension = "";
        if (mappingType.equals("never")) {
            extension = "";
        } else {
            // if mapping specifies ifmissing and filename contains an extension; extension is "" (filename already contains the extension)
            if (mappingType.equals("ifmissing") && filename.contains(".")) {
                extension = "";
            } else {
                // oth
                if (mappingType.equals("ifmissing") || mappingType.equals("always")) {
                    // look up extension from mapping
                    extension = getExtension(MIMETYPE);
                    // if not found in mappings, use the default
                    if (extension.equals(""))
                        extension = m_datastreamDefaultExtension;
                } else {
                    // unknown mapping type
                    LOG.warn("Unknown extension mapping type specified in fedora.fcfg");
                    extension = m_datastreamDefaultExtension;
                }
            }

        }
        return extension;
    }

}
