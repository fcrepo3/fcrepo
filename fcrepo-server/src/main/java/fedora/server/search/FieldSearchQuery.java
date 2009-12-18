/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.search;

import java.util.List;

/**
 * @author Chris Wilper
 */
public class FieldSearchQuery {

    public final static int CONDITIONS_TYPE = 1;

    public final static int TERMS_TYPE = 2;

    private List<Condition> m_conditions;

    private String m_terms;

    private final int m_type;

    public FieldSearchQuery(List<Condition> conditions) {
        m_conditions = conditions;
        m_type = CONDITIONS_TYPE;
    }

    public FieldSearchQuery(String terms) {
        m_terms = terms;
        m_type = TERMS_TYPE;
    }

    public int getType() {
        return m_type;
    }

    public List<Condition> getConditions() {
        return m_conditions;
    }

    public String getTerms() {
        return m_terms;
    }

}
