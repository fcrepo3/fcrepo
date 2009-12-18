/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server;

import java.util.Date;
import java.util.Iterator;

/**
 * A holder of context name-value pairs.
 * 
 * @author Chris Wilper
 */
public interface Context {

    public MultiValueMap getEnvironmentAttributes();

    public Iterator environmentAttributes();

    public int nEnvironmentValues(String name);

    public String getEnvironmentValue(String name);

    public String[] getEnvironmentValues(String name);

    public Iterator subjectAttributes();

    public int nSubjectValues(String name);

    public String getSubjectValue(String name);

    public String[] getSubjectValues(String name);

    public Iterator actionAttributes();

    public int nActionValues(String name);

    public String getActionValue(String name);

    public String[] getActionValues(String name);

    public Iterator resourceAttributes();

    public int nResourceValues(String name);

    public String getResourceValue(String name);

    public String[] getResourceValues(String name);

    public void setActionAttributes(MultiValueMap actionAttributes);

    public void setResourceAttributes(MultiValueMap resourceAttributes);

    public String getPassword();

    public String toString();

    public Date now();

    public boolean getNoOp();

    public static final String FEDORA_AUX_SUBJECT_ATTRIBUTES =
            "FEDORA_AUX_SUBJECT_ATTRIBUTES";

    //public boolean useCachedObject();

}
