/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.translation;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.fcrepo.common.Constants;
import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.utilities.LogConfig;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.fcrepo.utilities.XmlTransformUtility;
import org.trippi.io.TripleIteratorFactory;
import org.w3c.dom.Document;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;



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

    private static final OutputFormat fmt = new OutputFormat("XML", ENCODING, true);
    static {
        fmt.setIndent(2);
        fmt.setLineWidth(80);
        fmt.setPreserveSpace(false);
    }

    private final Date m_now = new Date();

    private final DODeserializer m_deserializer;

    private final DOSerializer m_serializer;

    private final boolean m_pretty;

    private final String m_inExt;

    private final String m_outExt;

    public ConvertObjectSerialization(Class<DODeserializer> deserializer,
                                      Class<DOSerializer> serializer,
                                      boolean pretty,
                                      String inExt,
                                      String outExt) {
        m_deserializer = getInstance(deserializer);
        m_serializer = getInstance(serializer);
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
        ReadableByteArrayOutputStream outBuf =
                new ReadableByteArrayOutputStream(4096);
        m_serializer.serialize(obj,
                               outBuf,
                               ENCODING,
                               DOTranslationUtility.AS_IS);
        outBuf.close();
        prettyPrint(outBuf.toInputStream(), destination);
    }

    private static void prettyPrint(InputStream source,
                                    OutputStream destination)
            throws Exception {
        BufferedWriter outWriter = new BufferedWriter(new PrintWriter(destination));
        XMLSerializer ser = new XMLSerializer(outWriter, fmt);
        DocumentBuilder builder = XmlTransformUtility.borrowDocumentBuilder();
        try {
            Document doc = builder.parse(source);
            ser.serialize(doc);
            outWriter.close();
        } finally {
            XmlTransformUtility.returnDocumentBuilder(builder);
        }
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
    public static void main(String[] args) throws ClassNotFoundException {
        LogConfig.initMinimal();
        if (args.length < 4 || args.length > 7) {
            die("Expected 4 to 7 arguments", true);
        }
        File sourceDir = new File(args[0]);
        if (!sourceDir.isDirectory()) {
            die("Not a directory: " + sourceDir.getPath(), false);
        }
        File destDir = new File(args[1]);

        Class<DODeserializer> deserializer = (Class<DODeserializer>) Class.forName(args[2]);
        Class<DOSerializer> serializer = (Class<DOSerializer>) Class.forName(args[3]);

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

        TripleIteratorFactory.defaultInstance().shutdown();
    }

    private static <T> T getInstance(Class<T> className) {
        try {
            return className.newInstance();
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
