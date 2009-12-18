/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Provides a workaround to the fact that
 * {@link java.io.File.renameTo(java.io.File)} doesn't work across NFS file
 * systems.
 * <p>
 * This code is taken from a workaround provided on the Sun Developer Network
 * Bug Database (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4073756), by
 * mailto:morgan.sziraki@cartesian.co.uk
 * </p>
 * <p>
 * The code is modified to protect against a situation where
 * <ul>
 * <li>We need to copy the file across NFS mounted filesystems</li>
 * <li>Another process is waiting for the file to be renamed</li>
 * </ul>
 * If we do a simple copy, we leave open the possibility that the second process
 * will see the new file created before we have a chance to copy the contents.
 * </p>
 * <p>
 * To avoid this, we create the new file with a modified filename (prefixed by
 * an underscore '_'). When the copying is complete, we rename the file to the
 * desired name. This rename is within a directory, and therefore does not
 * extend across NFS filesystem boundaries, so it should work!
 * </p>
 * <p>
 * Modify again to make sure that neither the destination file nor the temp file
 * exist - throw an exception if they do. We can't rename on top of an existing
 * file.
 * </p>
 * 
 * @author Jim Blake
 */
public class FileMovingUtil {

    private FileMovingUtil() {
        // No need to instantiate, since the only public method is static.
    }

    /**
     * <p>
     * Move a File
     * </p>
     * <p>
     * The renameTo method does not allow action across NFS mounted filesystems.
     * This method is the workaround.
     * </p>
     * 
     * @param fromFile
     *        The existing File
     * @param toFile
     *        The new File
     * @throws IOException
     *         if any problems occur
     */
    public final static void move(File fromFile, File toFile)
            throws IOException {
        // try the simple way first.
        if (fromFile.renameTo(toFile)) {
            return;
        }

        // copy to the temp file, then rename to the desired name.
        checkThatFileDoesntExist(toFile);
        File tempFile = createTempFile(toFile);
        checkThatFileDoesntExist(tempFile);
        copy(fromFile, tempFile);
        tempFile.renameTo(toFile);
        // delete the old one
        if (!fromFile.delete()) {
            throw new IOException("Failed to delete '" + fromFile.getParent()
                    + "'");
        }
    }

    /**
     * If we're trying to rename to a file that already exists, we'll throw an
     * exception. Make sure that the same thing happens if the rename is
     * accomplished by copying.
     * 
     * @throws IOException
     *         if the file exists.
     */
    private static void checkThatFileDoesntExist(File file) throws IOException {
        if (file.exists()) {
            throw new IOException("File '" + file.getPath()
                    + "' already exists.");
        }

    }

    /**
     * Create a temporary File object. Prefix the name of the base file with an
     * underscore.
     */
    private static File createTempFile(File baseFile) {
        File parentDirectory = baseFile.getParentFile();
        String filename = baseFile.getName();
        return new File(parentDirectory, '_' + filename);
    }

    /**
     * Copy a File
     * 
     * @param fromFile
     *        The existing File
     * @param toFile
     *        The new File
     * @throws IOException
     *         if any problems are encountered
     */
    private final static void copy(File fromFile, File toFile)
            throws IOException {
        FileInputStream in = new FileInputStream(fromFile);
        FileOutputStream out = new FileOutputStream(toFile);
        BufferedInputStream inBuffer = new BufferedInputStream(in);
        BufferedOutputStream outBuffer = new BufferedOutputStream(out);

        int theByte = 0;

        while ((theByte = inBuffer.read()) > -1) {
            outBuffer.write(theByte);
        }

        outBuffer.close();
        inBuffer.close();
        out.close();
        in.close();

        // cleanupif files are not the same length
        if (fromFile.length() != toFile.length()) {
            toFile.delete();

            throw new IOException("Copy failed: source file length="
                    + fromFile.length() + ", target file length="
                    + toFile.length());
        }
    }

}
