/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.swing.jtable;

import java.util.Collections;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

/**
 * DefaultSortTableModel.
 * 
 * <p>NOTICE: Portions created by Claude Duguay are Copyright &copy; 
 * Claude Duguay, originally made available at 
 * http://www.fawcette.com/javapro/2002_08/magazine/columns/visualcomponents/
 * 
 * @author Claude Duguay
 * @author Chris Wilper
 */
public class DefaultSortTableModel
        extends DefaultTableModel
        implements SortTableModel {

    private static final long serialVersionUID = 1L;

    public DefaultSortTableModel() {
    }

    public DefaultSortTableModel(int rows, int cols) {
        super(rows, cols);
    }

    public DefaultSortTableModel(Object[][] data, Object[] names) {
        super(data, names);
    }

    public DefaultSortTableModel(Object[] names, int rows) {
        super(names, rows);
    }

    public DefaultSortTableModel(Vector names, int rows) {
        super(names, rows);
    }

    public DefaultSortTableModel(Vector data, Vector names) {
        super(data, names);
    }

    public boolean isSortable(int col) {
        // return true; // FIXME: columns can't be sorted till the
        // how-do-i-get-the-pid-if-its-not-part-of-the-table-model-and-the-model-has-been-sorted
        // problem is solved
        return false;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void sortColumn(int col, boolean ascending) {
        Collections.sort(getDataVector(), new ColumnComparator(col, ascending));
    }
}
