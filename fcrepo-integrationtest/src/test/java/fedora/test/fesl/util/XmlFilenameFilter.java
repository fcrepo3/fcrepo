
package fedora.test.fesl.util;

import java.io.File;
import java.io.FilenameFilter;

public class XmlFilenameFilter
        implements FilenameFilter {

    public boolean accept(File dir, String name) {
        return name.endsWith(".xml");
    }
}
