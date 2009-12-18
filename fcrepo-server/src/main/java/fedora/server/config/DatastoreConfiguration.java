/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config;

import java.util.List;

/**
 *
 */
public class DatastoreConfiguration
        extends Configuration {

    private final String m_id;

    private final String m_comment;

    public DatastoreConfiguration(List<Parameter> parameters,
                                  String id,
                                  String comment) {
        super(parameters);
        m_id = id;
        m_comment = comment;
    }

    public String getId() {
        return m_id;
    }

    public String getComment() {
        return m_comment;
    }

}
