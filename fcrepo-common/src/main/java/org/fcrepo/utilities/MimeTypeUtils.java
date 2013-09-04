/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <tt>MimeTypeUtils</tt> provides the
 * {@link #fileExtensionForMIMEType fileExtensionForMIMEType()} method, which
 * converts a MIME type to a file extension. That method uses a traditional
 * <tt>mime.types</tt> files, similar to the file shipped with with web
 * servers such as Apache. It looks for a suitable file in the following
 * locations:
 * <ol>
 * <li> First, it looks for the file <tt>.mime.types</tt> in the user's home
 * directory.
 * <li> Next, it looks for <tt>mime.types</tt> (no leading ".") in all the
 * directories in the CLASSPATH
 * </ol>
 * <p>
 * It loads all the matching files it finds; the first mapping found for a given
 * MIME type is the one that is used. The files are only loaded once within a
 * given running Java VM.
 * </p>
 *
 * <p>This class is derived from org.clapper.util.misc.MIMETypeUtil, originally
 * authored by Brian M. Clapper and made available under a BSD-style license.</p>
 *
 * @author Brian M. Clapper
 * @author Edwin Shin *
 * @version $Id$
 */
public class MimeTypeUtils {

    /**
     * Default MIME type, when a MIME type cannot be determined from a file's
     * extension.
     *
     * @see #MIMETypeForFile
     * @see #MIMETypeForFileName
     */
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final Logger logger =
            LoggerFactory.getLogger(MimeTypeUtils.class);

    /**
     * Table for converting MIME type strings to file extensions. The table is
     * initialized on class load.
     */
    private static final Map<String, String> mimeTypeToExtensionMap =
            loadMappings();

    /**
     * Resource bundle containing MIME type mappings
     */
    private static final String MIME_MAPPINGS_BUNDLE =
            "org.fcrepo.server.resources.MIMETypes";

    private MimeTypeUtils() {
        // Can't be instantiated
    }

    /**
     * Get an appropriate extension for a MIME type.
     *
     * @param mimeType
     *        the String MIME type
     * @return the appropriate file name extension, or a default extension if
     *         not found. The extension will not have the leading "." character.
     */
    public static String fileExtensionForMIMEType(String mimeType) {
        loadMappings();

        String ext = mimeTypeToExtensionMap.get(mimeType);

        if (ext == null) ext = "dat";

        return ext;
    }

    /**
     * Load the MIME type mappings into memory.
     */
    private static HashMap<String, String> loadMappings() {

        HashMap<String, String> mimeTypeToExtensionMap =
                new HashMap<String, String>();

        // First, check the user's home directory.

        String fileSep = System.getProperty("file.separator");
        StringBuffer buf = new StringBuffer();

        buf.append(System.getProperty("user.home"));
        buf.append(fileSep);
        buf.append(".mime.types");

        loadMIMETypesFile(buf.toString());

        // Now, check every directory in the classpath.

        String pathSep = System.getProperty("path.separator");
        String[] pathComponents = pathSep.split(" ");
        int i;

        for (i = 0; i < pathComponents.length; i++) {
            buf.setLength(0);
            buf.append(pathComponents[i]);
            buf.append(fileSep);
            buf.append("mime.types");

            loadMIMETypesFile(buf.toString());
        }

        // Finally, load the resource bundle.

        ResourceBundle bundle = ResourceBundle.getBundle(MIME_MAPPINGS_BUNDLE);
        for (Enumeration<?> e = bundle.getKeys(); e.hasMoreElements();) {
            String type = (String) e.nextElement();
            try {
                String[] extensions = bundle.getString(type).split(" ");

                if (mimeTypeToExtensionMap.get(type) == null) {
                    logger.debug("Internal: " + type + " -> \"" + extensions[0]
                            + "\"");
                    mimeTypeToExtensionMap.put(type, extensions[0]);
                }
            }

            catch (MissingResourceException ex) {
                logger.error("While reading internal bundle \""
                                  + MIME_MAPPINGS_BUNDLE
                                  + "\", got unexpected error on key \"" + type
                                  + "\"",
                          ex);
            }
        }
        return mimeTypeToExtensionMap;
    }

    /**
     * Attempt to load a MIME types file. Throws no exceptions.
     *
     * @param path
     *        path to the file
     * @param map
     *        map to load
     */
    private static void loadMIMETypesFile(String path) {
        try {
            File f = new File(path);

            logger.debug("Attempting to load MIME types file \"" + path + "\"");
            if (!(f.exists() && f.isFile()))
                logger.debug("Regular file \"" + path + "\" does not exist.");

            else {
                LineNumberReader r = new LineNumberReader(new FileReader(f));
                String line;

                while ((line = r.readLine()) != null) {
                    line = line.trim();

                    if ((line.length() == 0) || (line.startsWith("#")))
                        continue;

                    String[] fields = line.split(" ");

                    // Skip lines without at least two tokens.

                    if (fields.length < 2) continue;

                    // Special case: Scan the extensions, and make sure we
                    // have at least one valid extension. Some .mime.types
                    // files have entries like this:
                    //
                    // mime/type  desc="xxx" exts="jnlp"
                    //
                    // We don't handle those.

                    List<String> extensions = new ArrayList<String>();

                    for (int i = 1; i < fields.length; i++) {
                        if (fields[i].indexOf('=') != -1) continue;
                        if (fields[i].indexOf('"') != -1) continue;

                        // Treat as valid. Remove any leading "."

                        if (fields[i].startsWith(".")) {
                            if (fields[i].length() == 1) continue;

                            fields[i] = fields[i].substring(1);
                        }

                        extensions.add(fields[i]);
                    }

                    if (extensions.size() == 0) continue;

                    // If the MIME type doesn't have a "/", skip it

                    String mimeType = fields[0];
                    String extension;

                    if (mimeType.indexOf('/') == -1) continue;

                    // The first field is the preferred extension. Keep any
                    // existing mapping for the MIME type.

                    if (mimeTypeToExtensionMap.get(mimeType) == null) {
                        extension = extensions.get(0);
                        logger.debug("File \"" + path + "\": " + mimeType
                                + " -> \"" + extension + "\"");

                        mimeTypeToExtensionMap.put(mimeType, extension);
                    }
                }

                r.close();
            }
        } catch (IOException ex) {
            logger.debug("Error reading \"" + path + "\"", ex);
        }
    }

}
