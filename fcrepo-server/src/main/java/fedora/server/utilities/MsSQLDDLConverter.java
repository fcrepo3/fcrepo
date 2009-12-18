/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A DDLConverter for MS SQL Server.
 *
 * @author David Handy
 */
public class MsSQLDDLConverter
        implements DDLConverter {

    public MsSQLDDLConverter() {
    }

    public boolean supportsTableType() {
        return true;
    }

    public String getDropDDL(String command) {
        String[] parts = command.split(" ");
        String tableName = parts[2];
        return "DROP TABLE " + tableName;
    }

    public List<String> getDDL(TableSpec spec) {
        List<String> list = new ArrayList<String>();
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
            out.append(" ");
            out.append(cs.getName());
            out.append(' ');
            if (cs.getType().equalsIgnoreCase("varchar")) {
                if (cs.getBinary()) {
                    out.append("BINARY");
                } else {
                    out.append(cs.getType());
                }
            } else if (cs.getType().toLowerCase().startsWith("int(")) {
                out.append("int");
            } else if (cs.getType().toLowerCase().startsWith("smallint(")) {
                out.append("smallint");
            } else {
                out.append(cs.getType());
            }
            if (cs.isNotNull()) {
                out.append(" NOT NULL");
            }
            if (cs.isAutoIncremented()) {
                out.append(" IDENTITY (1, 1)");
            }
            if (cs.getDefaultValue() != null) {
                out.append(" DEFAULT '");
                out.append(cs.getDefaultValue());
                out.append("'");
            }
            if (cs.isUnique()) {
                if (!end.toString().equals("")) {
                    end.append(",\n");
                }
                end.append(" CONSTRAINT ");
                end.append(cs.getName());
                end.append("_unique UNIQUE NONCLUSTERED (");
                end.append(cs.getName());
                end.append(")");
            }
            if (cs.getIndexName() != null) {
                list.add("CREATE INDEX " + cs.getIndexName() + " ON " 
                        + spec.getName() + " (" + cs.getName() + ")");
            }
            if (cs.getForeignTableName() != null) {
                if (!end.toString().equals("")) {
                    end.append(",\n");
                }
                end.append(" CONSTRAINT ");
                end.append(cs.getName());
                end.append("_fk FOREIGN KEY (");
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
            if (!end.toString().equals("")) {
                end.append(",\n");
            }
            end.append(" CONSTRAINT ");
            end.append(spec.getName() + "_pk");
            end.append(" PRIMARY KEY CLUSTERED (");
            end.append(spec.getPrimaryColumnName());
            end.append(")");
        }
        out.append(")");
	list.add(0, out.toString());
        if (!end.toString().equals("")) {
            list.add(1, "ALTER TABLE " + spec.getName() + " ADD" + end.toString());
        }
        return list;
    }
}
