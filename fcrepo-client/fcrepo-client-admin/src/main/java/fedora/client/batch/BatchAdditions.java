/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import fedora.common.Constants;

/**
 * @author Bill Niebel
 */
class BatchAdditions
        implements Constants {

    static final String FS = File.separator;

    private final Properties dataProperties;

    private final Properties metadataProperties;

    //set by arguments to constructor
    private boolean mediaFileContainsUrl = false;

    private String mediaPath = null;

    private String keyPath = null;

    private String metadataPath = null;

    private String stringPrefix = null;

    private String additionsPath = null;

    private String objectNameSpace = null;

    private String namespaceDeclarations = null;

    private int startObject = 0;

    //map METS elements names to directory names used in staging tree
    private static final Hashtable metadataCategories = new Hashtable();

    //populated in constructor
    String[] datastreams = null;

    String[] objectnames = null;

    private static final String getPath(File file) { //<===================
        String temp;
        try {
            temp = file.getCanonicalPath();
        } catch (Exception eCaughtFiles) {
            temp = "";
        }
        return temp;
    }

    static final int BUFFERLENGTH = 1024;

    private static final String getContents(String filePrefix) {
        String contents = null;
        try {
            File file = new File(filePrefix);
            FileInputStream fileInputStream = null;
            if (!file.exists()) {
                //LOG THIS
                throw new Exception("x");
            }
            if (!file.canRead()) {
                //LOG THIS
                throw new Exception("x");
            }
            {
                long lFileLength;
                try {
                    lFileLength = file.length();
                } catch (Exception eCaughtStatFile) { //<== make specific
                    throw new Exception("file " + getPath(file)
                            + "couldn't be statted for reading");
                }
                if (lFileLength > Integer.MAX_VALUE) {
                    throw new Exception("file " + getPath(file)
                            + "too large for reading");
                }
            }
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[BUFFERLENGTH];
            int bytesRead = 0;
            StringBuffer sbuffer = new StringBuffer();
            while ((bytesRead = fileInputStream.read(buffer, 0, BUFFERLENGTH)) != -1) {
                String temp = new String(buffer, 0, bytesRead);
                sbuffer.append(temp);
            }
            contents = new String(sbuffer);
        } catch (Exception e) {
            System.err.println("exception in content read " + e.getMessage());
        }
        return contents;
    }

    private static final void packageMetadata(PrintStream out,
                                              Properties metadataProperties,
                                              String context,
                                              String objectname,
                                              int indents) {
        Enumeration elementNames = metadataProperties.propertyNames(); //metadataCategories.keys();
        String tabs = "STRING NOT ASSIGNED TO";
        {
            StringBuffer temp = new StringBuffer();
            for (int i = 0; i < indents; i++) {
                temp.append("\t");
            }
            tabs = new String(temp);
        }
        if (elementNames.hasMoreElements()) {
            out.println(tabs + "<metadata>");
            while (elementNames.hasMoreElements()) {
                String elementName = (String) elementNames.nextElement();
                String directoryName =
                        metadataProperties.getProperty(elementName); //metadataCategories.get(elementName);
                String parentPath = context + directoryName;
                String fileName = getFilename(parentPath, objectname);
                if (fileName != null && !fileName.equals("")) {
                    String metadata = getContents(parentPath + FS + fileName);
                    if (metadata != null && !metadata.equals("")) {
                        out.println(tabs + "\t<metadata id=\"" + elementName
                                + "\">");
                        out.println(metadata);
                        out.println(tabs + "\t</metadata>"); //"+ elementName +">");
                    }
                }
            }
            out.println(tabs + "</metadata>");
        }
    }

    //batch builders can subclass, overloading this method
    protected String getHref(String webPrefix, String path) {
        return webPrefix + "/" + path;
    }

    static String getFilename(String parentPath, String objectName) {
        String filename = null;
        {
            File temp = new File(parentPath);
            if (temp.exists()) {
                if (!temp.isDirectory()) {
                    System.err.println("bad directory structure " + parentPath
                            + " " + objectName);
                } else {
                    String[] filenames = (new File(parentPath)).list();
                    int nFound = 0;
                    int iFound = -1;
                    for (int i = 0; i < filenames.length; i++) {
                        if (filenames[i].startsWith(objectName)) {
                            nFound++;
                            iFound = i;
                        }
                    }
                    if (nFound == 1) {
                        filename = filenames[iFound];
                    }
                }
            }
        }
        return filename;
    }

    /* package */BatchAdditions(Properties optValues,
                                 Properties dataProperties,
                                 Properties metadataProperties)
            throws Exception {

        this.dataProperties = dataProperties;
        this.metadataProperties = metadataProperties;
        String temp = optValues.getProperty(BatchTool.STARTOBJECT);
        String[] parts = temp.split(":");
        objectNameSpace = parts[0];
        String startObjectAsString = parts[1];

        String urlPath = optValues.getProperty(BatchTool.URLPATH);
        String dataPath = optValues.getProperty(BatchTool.DATAPATH);

        stringPrefix = optValues.getProperty(BatchTool.STRINGPREFIX);

        keyPath = optValues.getProperty(BatchTool.KEYPATH);

        namespaceDeclarations = optValues.getProperty(BatchTool.DECLARATIONS);
        metadataPath = optValues.getProperty(BatchTool.METADATAPATH);
        additionsPath = optValues.getProperty(BatchTool.ADDITIONSPATH);

        if (!BatchTool.argOK(namespaceDeclarations)) {
            System.err.println("namespaceDeclarations required");
            throw new Exception();
        }

        if (!BatchTool.argOK(objectNameSpace)) {
            System.err.println("objectNameSpace required");
            throw new Exception();
        }

        if (!BatchTool.argOK(startObjectAsString)) {
            System.err.println("startObject required");
            throw new Exception();
        } else {
            try {
                startObject = Integer.parseInt(startObjectAsString);
            } catch (Exception e) {
                System.err.println("startObject must be integer");
                throw new Exception();
            }
        }

        if (!BatchTool.argOK(keyPath)) {
            System.err.println("keyPath required");
            throw new Exception();
        }

        if (!BatchTool.argOK(metadataPath)) {
            System.err.println("metadataPath required");
            throw new Exception();
        }

        if (!BatchTool.argOK(additionsPath)) {
            System.err.println("additionsPath required");
            throw new Exception();
        }

        if (BatchTool.argOK(urlPath)) {
            if (BatchTool.argOK(dataPath)) {
                System.err
                        .println("use either data or url path -- both provided");
                throw new Exception();
            } else {
                mediaPath = urlPath;
                mediaFileContainsUrl = true;
            }
        } else if (BatchTool.argOK(dataPath)) {
            mediaPath = dataPath;
            mediaFileContainsUrl = false;
        } else {
            System.err
                    .println("use either data or url path -- neither provided");
            throw new Exception();
        }

        if (!BatchTool.argOK(stringPrefix)) {
            System.err.println("stringprefix required");
            throw new Exception();
        }
    }

    private boolean good2go = false;

    final void prep() throws Exception {
        //get datastream labels from mediaDirectory
        File mediaDirectory = new File(mediaPath);
        File[] datastreamDirectories = mediaDirectory.listFiles();
        datastreams = new String[datastreamDirectories.length];
        for (int i = 0; i < datastreamDirectories.length; i++) {
            if (datastreamDirectories[i].isDirectory()) {
                String directoryName = datastreamDirectories[i].getName();
                if (metadataCategories.get(directoryName) != null) {
                    throw new Exception();
                }
                datastreams[i] = datastreamDirectories[i].getName();
            }
        }

        //get objectnames from keyDirectory
        File[] files = null;
        {
            File keyDirectory = new File(keyPath);
            files = keyDirectory.listFiles();
        }
        objectnames = new String[files.length];

        for (int i = 0; i < files.length; i++) {
            String objectname = null;
            {
                String filename = files[i].getName();
                int j = filename.lastIndexOf('.');
                objectname = j >= 0 ? filename.substring(0, j) : filename;
            }
            objectnames[i] = objectname;
        }
        good2go = true;
    }

    final void process() throws Exception {
        if (good2go) {
            int object = startObject;
            for (String objectname : objectnames) {
                String objid = objectNameSpace + ":" + object++;
                PrintStream out =
                        new PrintStream(new FileOutputStream(additionsPath + FS
                                + objectname));
                out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
                out.print("<input OBJID=\"" + objid + "\"");
                out.println(" xmlns:METS=\"" + METS.uri + "\" "
                        + namespaceDeclarations + " >");
                packageMetadata(out,
                                metadataProperties,
                                metadataPath + FS,
                                objectname,
                                1);
                out.println("\t<datastreams>");
                Enumeration ddatastreams = dataProperties.propertyNames();
                while (ddatastreams.hasMoreElements()) {
                    String ndatastream = (String) ddatastreams.nextElement();
                    String datastream = dataProperties.getProperty(ndatastream);
                    String href =
                            mediaFileContainsUrl ? getContents(mediaPath + FS
                                    + datastream + FS + objectname)
                                    : getHref(stringPrefix, datastream
                                            + "/"
                                            + getFilename(mediaPath + "/"
                                                    + datastream, objectname));
                    out.println("\t\t<datastream id=\"" + ndatastream /* fixup(datastream) */
                            + "\" href=\"" + href + "\">");
                    out.println("\t\t</datastream>");
                }
                out.println("\t</datastreams>");
                out.println("</input>");
                out.close();
            }
        }
    }

    public static final void main(String[] args) {
        try {
            Properties miscProperties = new Properties();
            Properties datastreamProperties = new Properties();
            Properties metadataProperties = new Properties();
            miscProperties
                    .load(new FileInputStream("c:\\batchdemo\\batchtool.properties"));
            datastreamProperties
                    .load(new FileInputStream("c:\\batchdemo\\datastream.properties"));
            metadataProperties
                    .load(new FileInputStream("c:\\batchdemo\\metadata.properties"));
            BatchAdditions batchAdditions =
                    new BatchAdditions(miscProperties,
                                       datastreamProperties,
                                       metadataProperties);
            batchAdditions.prep();
            batchAdditions.process();
        } catch (Exception e) {
        }
    }
}
