/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.helpers;

import java.io.File;

/**
 * Subclass of File is used as a marker.
 * <p>
 * An instance of this class behaves exactly as a java.io.File object would
 * behave. However, it can be tested with instanceof to reveal that it is in
 * fact a temp file, and as such can safely be deleted after use.
 * 
 * @author Jim Blake
 */
public class JournalTempFile
        extends File {

    private static final long serialVersionUID = 1L;

    public JournalTempFile(File file) {
        super(file.getPath());
    }
}
