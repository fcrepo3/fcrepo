/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip and GZip utilities.
 * 
 * @author Edwin Shin
 */
public class Zip {

    private static final int BUFFER = 2048;

    /**
     * Create a zip file.
     * 
     * @param destination
     *        The zip file to create.
     * @param source
     *        The file or directory to be zipped.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void zip(File destination, File source)
            throws FileNotFoundException, IOException {
        zip(destination, new File[] {source});
    }

    /**
     * Create a zip file.
     * 
     * @param destination
     *        The zip file to create.
     * @param source
     *        The File array to be zipped.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void zip(File destination, File[] source)
            throws FileNotFoundException, IOException {
        FileOutputStream dest = new FileOutputStream(destination);
        ZipOutputStream zout =
                new ZipOutputStream(new BufferedOutputStream(dest));
        for (File element : source) {
            zip(null, element, zout);
        }
        zout.close();
    }

    /**
     * Extracts the file given by entryName to destination.
     * 
     * @param zipFile
     * @param entryName
     * @param destination
     *        The extracted destination File.
     * @throws IOException
     */
    public static void extractFile(File zipFile,
                                   String entryName,
                                   File destination) throws IOException {
        ZipFile zip = new ZipFile(zipFile);

        try {
            ZipEntry entry = zip.getEntry(entryName);

            if (entry != null) {
                // Get an input stream for the entry.
                InputStream entryStream = zip.getInputStream(entry);
                try {
                    // Create the output file
                    File parent = destination.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }

                    FileOutputStream file = new FileOutputStream(destination);

                    try {
                        // Allocate a buffer for reading the entry data.
                        byte[] data = new byte[BUFFER];
                        int bytesRead;

                        // Read the entry data and write it to the output file.
                        while ((bytesRead = entryStream.read(data)) != -1) {
                            file.write(data, 0, bytesRead);
                        }
                    } finally {
                        file.close();
                    }
                } finally {
                    entryStream.close();
                }
            } else {
                throw new IOException(zipFile.getName() + " does not contain: "
                        + entryName);
            }
        } finally {
            zip.close();
        }
    }

    /**
     * Unzips the InputStream to the given destination directory.
     * 
     * @param is
     * @param destDir
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void unzip(InputStream is, File destDir)
            throws FileNotFoundException, IOException {
        BufferedOutputStream dest = null;
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            //System.out.println("Extracting: " + entry);
            if (entry.isDirectory()) {
                // Otherwise, empty directories do not get created
                (new File(destDir, entry.getName())).mkdirs();
            } else {
                File f = new File(destDir, entry.getName());
                f.getParentFile().mkdirs();
                int count;
                byte data[] = new byte[BUFFER];
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(f);
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
        }
        zis.close();
    }

    public static void gzip() {
        // TODO
    }

    public static void gunzip() {
        // TODO
    }

    // Convenience methods
    public static void zip(String destination, String source)
            throws FileNotFoundException, IOException {
        zip(new File(destination), new File(source));
    }

    public static void unzip(InputStream is, String destDir)
            throws FileNotFoundException, IOException {
        unzip(is, new File(destDir));
    }

    public static void main(String[] args) {
        // 2 arguments: zipfile, source/destination
        // whether or not the zipfile exists, i.e.:
        // if zipfile exists
        //         if zipfile extension == zip, then unzip
        //        if zipfile extension == gz, then gunzip
        // else
        //         if zipfile extension == zip, then zip
        //        if zipfile extension == gz, then gzip

        // valid actions are: zip, unzip, gzip, and gunzip

        // should consider making source/destination optional (i.e., assume current directory)
        // might consider taking a filefilter
    }

    private static void zip(String baseDir, File source, ZipOutputStream zout)
            throws IOException {
        ZipEntry entry = null;
        if (baseDir == null || baseDir.equals(".") || baseDir.equals("./")) {
            baseDir = "";
        }

        if (source.isDirectory()) {
            // If there's a "better" way to indicate a directory, go ahead :)
            // Perhaps at least use File.separator and not "/"
            entry = new ZipEntry(baseDir + source.getName() + "/");
        } else {
            entry = new ZipEntry(baseDir + source.getName());
        }
        zout.putNextEntry(entry);
        //System.out.println("Adding " + entry.getName());

        if (!source.isDirectory()) {
            byte data[] = new byte[BUFFER];
            FileInputStream fis = new FileInputStream(source);
            BufferedInputStream origin = new BufferedInputStream(fis, BUFFER);

            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                zout.write(data, 0, count);
            }
            fis.close();
            origin.close();
        } else {
            File files[] = source.listFiles();
            for (File element : files) {
                zip(entry.getName(), element, zout);
            }
        }
    }
}
