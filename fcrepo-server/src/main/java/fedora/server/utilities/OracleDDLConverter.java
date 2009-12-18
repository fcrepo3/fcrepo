/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A DDLConverter that works with Oracle.
 *
 * @author Chris Wilper
 */
public class OracleDDLConverter
        implements DDLConverter {

    public OracleDDLConverter() {
    }

    public List<String> getDDL(TableSpec spec) {
        ArrayList<String> l = new ArrayList<String>();
        StringBuffer out = new StringBuffer();
        StringBuffer end = new StringBuffer();
        out.append("CREATE TABLE " + spec.getName() + " (\n");
        Iterator<ColumnSpec> csi = spec.columnSpecIterator();
        int csNum = 0;
        while (csi.hasNext()) {
            if (csNum > 0) {
                out.append(",\n");
            }
            csNum++;
            ColumnSpec cs = csi.next();
            out.append("  ");
            out.append(cs.getName());
            out.append(' ');
            if (cs.getType().toLowerCase().indexOf("int(") == 0) {
                // if precision was specified for int, use oracle's default int precision
                out.append("int");
            } else {
                if (cs.getType().toLowerCase().indexOf("smallint(") == 0) {
                    out.append("smallint");
                } else {
                    if (cs.getType().toLowerCase().equals("bigint")) {
                        out.append("NUMBER(20,0)");
                    } else if (cs.getType().toLowerCase().equals("text")) {
                        out.append("CLOB");
                    } else {
                        out.append(cs.getType());
                    }
                }
            }
            if (cs.isAutoIncremented()) {
                // oracle doesn't support auto-increment in a CREATE TABLE
                // ... but it can be done by creating the table,
                // creating a sequence, then creating a trigger that
                // inserts the sequence's next value for that column
                // upon insert.
                StringBuffer createSeq = new StringBuffer();
                createSeq.append("CREATE SEQUENCE ");
                createSeq.append(spec.getName());
                createSeq.append("_S");
                createSeq.append(csNum);
                createSeq.append("\n");
                createSeq.append("  START WITH 1\n");
                createSeq.append("  INCREMENT BY 1\n");
                createSeq.append("  NOMAXVALUE");
                l.add(createSeq.toString());
                StringBuffer createTrig = new StringBuffer();
                createTrig.append("CREATE TRIGGER ");
                createTrig.append(spec.getName());
                createTrig.append("_T");
                createTrig.append(csNum);
                createTrig.append("\n");
                createTrig.append("  BEFORE INSERT ON ");
                createTrig.append(spec.getName());
                createTrig.append("\n  FOR EACH ROW");
                createTrig.append("\n  BEGIN");
                createTrig.append("\n    SELECT ");
                createTrig.append(spec.getName());
                createTrig.append("_S");
                createTrig.append(csNum);
                createTrig.append(".NEXTVAL INTO :NEW.");
                createTrig.append(cs.getName());
                createTrig.append(" FROM DUAL;");
                createTrig.append("\n  END;");
                l.add(createTrig.toString());
            }
            if (cs.getDefaultValue() != null) {
                out.append(" DEFAULT '");
                out.append(cs.getDefaultValue());
                out.append("'");
            }
            if (cs.isNotNull()) {
                out.append(" NOT NULL");
            }
            if (cs.isUnique()) {
                if (!end.toString().equals("")) {
                    end.append(",\n");
                }
                end.append("  UNIQUE ");
                end.append(" (");
                end.append(cs.getName());
                end.append(")");
            }
            if (cs.getIndexName() != null) {
                l.add("CREATE INDEX " + spec.getName() + "_" + cs.getName()
                      + " ON " + spec.getName() + " (" + cs.getName() + ")");
            }
            if (cs.getForeignTableName() != null) {
                if (!end.toString().equals("")) {
                    end.append(",\n");
                }
                end.append("  FOREIGN KEY ");
                end.append(cs.getName());
                end.append(" (");
                end.append(cs.getName());
                end.append(") REFERENCES ");
                end.append(cs.getForeignTableName());
                end.append(" (");
                end.append(cs.getForeignColumnName());
                end.append(")");
                if (cs.getOnDeleteAction() != null) {
                    end.append(" ON DELETE ");
                    end.append(cs.getOnDeleteAction());
                }
            }
        }
        if (spec.getPrimaryColumnName() != null) {
            out.append(",\n  PRIMARY KEY (");
            out.append(spec.getPrimaryColumnName());
            out.append(")");
        }
        if (!end.toString().equals("")) {
            out.append(",\n");
            out.append(end);
        }
        out.append("\n");
        out.append(")");
        l.add(0, out.toString());
        return l;
    }

}
