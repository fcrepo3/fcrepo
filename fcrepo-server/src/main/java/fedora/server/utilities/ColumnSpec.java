/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

/**
 * @author Chris Wilper
 */
public class ColumnSpec {

    private final String m_name;

    private final boolean m_binary;

    private final String m_type;

    private final String m_defaultValue;

    private final boolean m_isAutoIncremented;

    private final String m_indexName;

    private final boolean m_isUnique;

    private final boolean m_isNotNull;

    private final String m_foreignTableName;

    private final String m_foreignColumnName;

    private final String m_onDeleteAction;

    public ColumnSpec(String name,
                      String type,
                      boolean binary,
                      String defaultValue,
                      boolean isAutoIncremented,
                      String indexName,
                      boolean isUnique,
                      boolean isNotNull,
                      String foreignTableName,
                      String foreignColumnName,
                      String onDeleteAction) {
        m_name = name;
        m_type = type;
        m_binary = binary;
        m_defaultValue = defaultValue;
        m_isAutoIncremented = isAutoIncremented;
        m_indexName = indexName;
        m_isUnique = isUnique;
        m_isNotNull = isNotNull;
        m_foreignTableName = foreignTableName;
        m_foreignColumnName = foreignColumnName;
        m_onDeleteAction = onDeleteAction;
    }

    public String getName() {
        return m_name;
    }

    public boolean getBinary() {
        return m_binary;
    }

    public String getType() {
        return m_type;
    }

    public String getForeignTableName() {
        return m_foreignTableName;
    }

    public String getForeignColumnName() {
        return m_foreignColumnName;
    }

    public String getOnDeleteAction() {
        return m_onDeleteAction;
    }

    public boolean isUnique() {
        return m_isUnique;
    }

    public boolean isNotNull() {
        return m_isNotNull;
    }

    public String getIndexName() {
        return m_indexName;
    }

    public boolean isAutoIncremented() {
        return m_isAutoIncremented;
    }

    public String getDefaultValue() {
        return m_defaultValue;
    }

}
