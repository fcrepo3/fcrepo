/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.readerwriter.multifile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import fedora.server.journal.JournalException;
import fedora.server.journal.helpers.FileMovingUtil;

/**
 * Encapsulate the information that goes with consuming a Journal file.
 * 
 * @author Jim Blake
 */
class JournalInputFile {

    private final File file;

    private final FileReader fileReader;

    private final XMLEventReader xmlReader;

    public JournalInputFile(File file)
            throws JournalException {
        if (!file.isFile()) {
            throw new JournalException("Journal file '" + file.getPath()
                    + "' is not a file.");
        }
        if (!file.canRead()) {
            throw new JournalException("Journal file '" + file.getPath()
                    + "' is not readable.");
        }

        try {
            this.file = file;
            XMLInputFactory factory = XMLInputFactory.newInstance();
            fileReader = new FileReader(file);
            xmlReader = factory.createXMLEventReader(fileReader);
        } catch (FileNotFoundException e) {
            throw new JournalException(e);
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        }
    }

    public String getFilename() {
        return file.getPath();
    }

    /**
     * When we have processed the file, move it to the archive directory.
     */
    public void closeAndRename(File archiveDirectory) throws JournalException {
        try {
            xmlReader.close();
            fileReader.close();
            File archiveFile = new File(archiveDirectory, file.getName());

            /*
             * java.io.File.renameTo() has a known bug when working across
             * file-systems, see:
             * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4073756 So
             * instead of this call: file.renameTo(archiveFile); We use the
             * following line, and check for exception...
             */
            try {
                FileMovingUtil.move(file, archiveFile);
            } catch (IOException e) {
                throw new JournalException("Failed to rename file from '"
                        + file.getPath() + "' to '" + archiveFile.getPath()
                        + "'", e);
            }
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        } catch (IOException e) {
            throw new JournalException(e);
        }
    }

    public XMLEventReader getReader() {
        return xmlReader;
    }

}
