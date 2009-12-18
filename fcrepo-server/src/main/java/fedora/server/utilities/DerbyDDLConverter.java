/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A DDLConverter that works with Derby.
 *
 * <pre>
 * The type-mapping from default-db-spec to Derby internal types is as follows:
 *
 * db-spec                  Derby-type
 * -------                  ----------
 * int(11)                  INT [4-bytes]
 * varchar(x)               VARCHAR(x)
 * varchar(x)-binary        VARCHAR(x) FOR BIT DATA // binary setting ignored
 * smallint(6)              SMALLINT [2-bytes]
 * bigint                   BIGINT [8-bytes]
 * text                     CLOB [default: 2GB]
 * </pre>
 *
 * @author Andrew Woods
 */
public class DerbyDDLConverter
        implements DDLConverter {

    public DerbyDDLConverter() {
    }

    public List<String> getDDL(TableSpec spec) {
        ArrayList<String> l = new ArrayList<String>();
        StringBuilder out = new StringBuilder();

        out.append("CREATE TABLE " + spec.getName() + " (\n");

        Iterator<ColumnSpec> csi = spec.columnSpecIterator();
        int csNum = 0;
        while (csi.hasNext()) {
            if (csNum > 0) {
                out = removeTrailingWhitespace(out);
                out.append(",\n");
            }
            csNum++;
            ColumnSpec cs = csi.next();

            out.append(getColumnName(cs));
            out.append(getDataType(cs));
            out.append(getColumnConstraint(cs));
            out.append(getDefaultValue(cs));

            if (cs.getIndexName() != null) {
                l.add(createIndexStatement(spec, cs));
            }
        }

        out = removeTrailingWhitespace(out);
        if (spec.getPrimaryColumnName() != null) {
            out.append(",\n");
            out.append(getTableConstraint(spec));
        }

        out.append(')');
        l.add(0, out.toString());
        return l;
    }

    private StringBuilder removeTrailingWhitespace(StringBuilder out) {
        while (out.charAt(out.length() - 1) == ' ') {
            out.deleteCharAt(out.length() - 1);
        }
        return out;
    }

    private StringBuilder getColumnName(ColumnSpec cs) {
        StringBuilder out = new StringBuilder();

        out.append("  ");
        out.append(cs.getName());
        out.append(' ');

        return out;
    }

    private Object getDataType(ColumnSpec cs) {
        StringBuilder out = new StringBuilder();

        if (isNumberType(cs)) {
            out.append(getTypeWithoutByteLength(cs));
        } else {
            if (cs.getType().equalsIgnoreCase("text")) {
                out.append("CLOB");
            } else {
                out.append(cs.getType());
            }
        }

        out.append(' ');

        return out;
    }

    private boolean isNumberType(ColumnSpec cs) {
        return cs.getType().toLowerCase().indexOf("int") != -1;
    }

    private String getTypeWithoutByteLength(ColumnSpec cs) {
        int end = cs.getType().indexOf('(');
        if (end == -1) end = cs.getType().length();

        return cs.getType().substring(0, end);
    }

    private Object getColumnConstraint(ColumnSpec cs) {
        StringBuilder out = new StringBuilder();

        if (cs.isUnique()) {
            out.append("UNIQUE ");
        }
        if (cs.isNotNull()) {
            out.append("NOT NULL ");
        }
        // only NUMBER types can auto-increment.
        if (cs.isAutoIncremented() && isNumberType(cs)) {
            out.append("GENERATED ALWAYS AS IDENTITY ");
        }

        return out;
    }

    private StringBuilder getDefaultValue(ColumnSpec cs) {
        StringBuilder out = new StringBuilder();

        if (cs.getDefaultValue() != null) {
            out.append("DEFAULT ");
            if (!isNumberType(cs)) {
                out.append("'");
            }
            out.append(cs.getDefaultValue());

            if (!isNumberType(cs)) {
                out.append("'");
            }
            out.append(' ');
        }

        return out;
    }

    private StringBuilder getTableConstraint(TableSpec ts) {
        StringBuilder out = new StringBuilder();

        if (ts.getPrimaryColumnName() != null) {
            out.append("  PRIMARY KEY (");
            out.append(ts.getPrimaryColumnName());
            out.append(")");
        }

        return out;
    }

    private String createIndexStatement(TableSpec ts, ColumnSpec cs) {
        StringBuilder out = new StringBuilder();

        if (cs.getIndexName() != null) {
            out.append("CREATE INDEX " + ts.getName() + "_" + cs.getName()
                    + " ON " + ts.getName() + " (" + cs.getName() + ")");
        }

        return out.toString();
    }

}
