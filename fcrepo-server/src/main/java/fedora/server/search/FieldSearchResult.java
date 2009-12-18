/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.search;

import java.util.Date;
import java.util.List;

/**
 * @author Chris Wilper
 */
public interface FieldSearchResult {

    public List<ObjectFields> objectFieldsList();

    public String getToken();

    public long getCursor();

    public long getCompleteListSize();

    public Date getExpirationDate();

}
