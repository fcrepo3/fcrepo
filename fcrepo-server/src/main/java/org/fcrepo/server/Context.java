/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import java.net.URI;
import java.util.Date;
import java.util.Iterator;

/**
 * A holder of context name-value pairs.
 * 
 * @author Chris Wilper
 */
public interface Context {

    public MultiValueMap<URI> getEnvironmentAttributes();

    public Iterator<URI> environmentAttributes();

    public int nEnvironmentValues(URI name);

    public String getEnvironmentValue(URI name);

    public String[] getEnvironmentValues(URI name);

    // subject attributes may just be roleNames, and aren't necessarily URIs
    public Iterator<String> subjectAttributes();

    public int nSubjectValues(String name);

    public String getSubjectValue(String name);

    public String[] getSubjectValues(String name);

    public Iterator<URI> actionAttributes();

    public int nActionValues(URI name);

    public String getActionValue(URI name);

    public String[] getActionValues(URI name);

    public Iterator<URI> resourceAttributes();

    public int nResourceValues(URI name);

    public String getResourceValue(URI name);

    public String[] getResourceValues(URI name);

    public void setActionAttributes(MultiValueMap<URI> actionAttributes);

    public void setResourceAttributes(MultiValueMap<URI> resourceAttributes);

    public String getPassword();

    public String toString();

    public Date now();

    public boolean getNoOp();

    public static final String FEDORA_AUX_SUBJECT_ATTRIBUTES =
            "FEDORA_AUX_SUBJECT_ATTRIBUTES";

    //public boolean useCachedObject();

}
