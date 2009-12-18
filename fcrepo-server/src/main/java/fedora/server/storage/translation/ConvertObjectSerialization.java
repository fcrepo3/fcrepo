/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Date;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.w3c.dom.Document;

import fedora.common.Constants;

import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DigitalObject;

/**
 * Utility class to convert objects from one serialization format to another.
 *
 * @author Edwin Shin
 * @author Chris Wilper
 * @since 3.0
 * @version $Id$
 */
public class ConvertObjectSerialization {

    private static final String ENCODING = "UTF-8";

    private final Date m_now = new Date();

    private final DODeserializer m_deserializer;

    private final DOSerializer m_serializer;

    private final boolean m_pretty;

    private final String m_inExt;

    private final String m_outExt;

    public ConvertObjectSerialization(DODeserializer deserializer,
                                      DOSerializer serializer,
                                      boolean pretty,
                                      String inExt,
                                      String outExt) {
        m_deserializer = deserializer;
        m_serializer = serializer;
        m_pretty = pretty;
        m_inExt = inExt;
        m_outExt = outExt;
    }

    private boolean convert(InputStream source, OutputStream destination) {
        DigitalObject obj = new BasicDigitalObject();
        try {
            m_deserializer.deserialize(source,
                                       obj,
                                       ENCODING,
                                       DOTranslationUtility.AS_IS);
            setObjectDefaults(obj);

            if (m_pretty) {
                prettyPrint(obj, destination);
            } else {
                m_serializer.serialize(obj,
                                       destination,
                                       ENCODING,
                                       DOTranslationUtility.AS_IS);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void prettyPrint(DigitalObject obj, OutputStream destination)
            throws Exception {
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        m_serializer.serialize(obj,
                               outBuf,
                               ENCODING,
                               DOTranslationUtility.AS_IS);
        InputStream inBuf = new ByteArrayInputStream(outBuf.toByteArray());
        prettyPrint(inBuf, destination);
    }

    private static void prettyPrint(InputStream source,
                                    OutputStream destination)
            throws Exception {
        OutputFormat fmt = new OutputFormat("XML", ENCODING, true);
        fmt.setIndent(2);
        fmt.setLineWidth(80);
        fmt.setPreserveSpace(false);
        XMLSerializer ser = new XMLSerializer(destination, fmt);
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(source);
        ser.serialize(doc);
        destination.close();
    }

    /**
     * Convert files from one format to the other.
     * Hidden directories (directories starting with a ".") are skipped.
     *
     * @param source
     * @param destination
     * @return
     */
    public boolean convert(File source, File destination) {
        boolean result = true;
        if (source.isDirectory()) {
            if (source.getName().startsWith(".")) {
                // skip "hidden" directories
                return result;
            }
            if (destination.exists()) {
                result = result && destination.isDirectory();
            } else {
                result = result && destination.mkdirs();
            }
            File[] children = source.listFiles();
            for (File element : children) {
                String inName = element.getName();
                String outName;
                if (element.isDirectory()) {
                    outName = inName;
                } else {
                    outName = inName.substring(0, inName.lastIndexOf('.') + 1)
                            + m_outExt;
                }
                result = result && convert(new File(source, inName),
                                           new File(destination, outName));
            }
            return result;
        } else {
            try {
                if (!source.getName().endsWith("." + m_inExt)) {
                    return result;
                }
                InputStream in = new FileInputStream(source);
                if (!destination.getParentFile().exists()) {
                    destination.getParentFile().mkdirs();
                }
                OutputStream out = new FileOutputStream(destination);
                result = result && convert(in, out);
                out.close();
                in.close();
                return result;
            } catch (IOException e) {
                return false;
            }
        }
    }

    // - Sets dates to current date if unset
    // - Sets datastream sizes to 0
    private void setObjectDefaults(DigitalObject obj) {
        if (obj.getCreateDate() == null) obj.setCreateDate(m_now);
        if (obj.getLastModDate() == null) obj.setLastModDate(m_now);

        Iterator<String> dsIds = obj.datastreamIdIterator();
        while (dsIds.hasNext()) {
            String dsid = dsIds.next();
            for (Datastream ds : obj.datastreams(dsid)) {
                ds.DSSize = 0;
                if (ds.DSCreateDT == null) {
                    ds.DSCreateDT = m_now;
                }
            }
        }
    }

    /**
     * Command-line utility to convert objects from one format to another.
     *
     * @param args command-line args.
     */
    public static void main(String[] args) {
        if (args.length < 4 || args.length > 7) {
            die("Expected 4 to 7 arguments", true);
        }
        File sourceDir = new File(args[0]);
        if (!sourceDir.isDirectory()) {
            die("Not a directory: " + sourceDir.getPath(), false);
        }
        File destDir = new File(args[1]);

        DODeserializer deserializer = (DODeserializer) getInstance(args[2]);
        DOSerializer serializer = (DOSerializer) getInstance(args[3]);

        // So DOTranslationUtility works...
        System.setProperty("fedora.hostname", "localhost");
        System.setProperty("fedora.port", "8080");
        System.setProperty("fedora.appServerContext", Constants.FEDORA_DEFAULT_APP_CONTEXT);

        boolean pretty = args.length > 4 && args[4].equals("true");

        String inExt = "xml";
        if (args.length > 5) inExt = args[5];

        String outExt = "xml";
        if (args.length > 6) outExt = args[6];

        ConvertObjectSerialization converter =
                new ConvertObjectSerialization(deserializer,
                                               serializer,
                                               pretty,
                                               inExt,
                                               outExt);
        converter.convert(sourceDir, destDir);
    }

    private static Object getInstance(String className) {
        try {
            return Class.forName(className).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            die("Unable to instantiate: " + className, false);
            return null; // unreachable
        }
    }

    private static void die(String message, boolean showUsage) {
        System.out.println("ERROR: " + message);
        if (showUsage) {
            System.out.println("Usage: ConvertObjectSerialization srcDir dstDir serClass deserClass");
            System.out.println("                                  [pretty] [inExt] [outExt]");
            System.out.println("Where: srcDir     : source directory");
            System.out.println("       dstDir     : destination directory (created if necessary)");
            System.out.println("       deserClass : DODeserializer class name");
            System.out.println("       serClass   : DOSerializer class name");
            System.out.println("       pretty     : if true and output is xml, it will be pretty-printed");
            System.out.println("       inExt      : extension for input files, default is xml");
            System.out.println("       outExt     : extension for output files, default is xml");
        }
        System.exit(1);
    }
}
