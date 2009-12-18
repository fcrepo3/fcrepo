/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.swing.jtable;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * SortHeaderRenderer.
 * 
 * <p>NOTICE: Portions created by Claude Duguay are Copyright &copy; 
 * Claude Duguay, originally made available at 
 * http://www.fawcette.com/javapro/2002_08/magazine/columns/visualcomponents/
 * 
 * @author Claude Duguay
 * @author Chris Wilper
 */
public class SortHeaderRenderer
        extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    public static Icon NONSORTED = new SortArrowIcon(SortArrowIcon.NONE);

    public static Icon ASCENDING = new SortArrowIcon(SortArrowIcon.ASCENDING);

    public static Icon DECENDING = new SortArrowIcon(SortArrowIcon.DECENDING);

    public SortHeaderRenderer() {
        setHorizontalTextPosition(LEFT);
        setHorizontalAlignment(CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int col) {
        int index = -1;
        boolean ascending = true;
        if (table instanceof JSortTable) {
            JSortTable sortTable = (JSortTable) table;
            index = sortTable.getSortedColumnIndex();
            ascending = sortTable.isSortedColumnAscending();
        }
        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                setForeground(header.getForeground());
                setBackground(header.getBackground());
                setFont(header.getFont());
            }
        }
        Icon icon = ascending ? ASCENDING : DECENDING;
        setIcon(col == index ? icon : NONSORTED);
        setText(value == null ? "" : value.toString());
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        return this;
    }
}
