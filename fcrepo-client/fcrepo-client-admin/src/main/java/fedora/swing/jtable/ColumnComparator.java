/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.swing.jtable;

import java.util.Comparator;
import java.util.Vector;

/**
 * ColumnComparator.
 * 
 * <p>NOTICE: Portions created by Claude Duguay are Copyright &copy; 
 * Claude Duguay, originally made available at 
 * http://www.fawcette.com/javapro/2002_08/magazine/columns/visualcomponents/
 * 
 * @author Claude Duguay
 * @author Chris Wilper
 */
public class ColumnComparator
        implements Comparator {

    protected int index;

    protected boolean ascending;

    public ColumnComparator(int index, boolean ascending) {
        this.index = index;
        this.ascending = ascending;
    }

    public int compare(Object one, Object two) {
        if (one instanceof Vector && two instanceof Vector) {
            Vector vOne = (Vector) one;
            Vector vTwo = (Vector) two;
            Object oOne = vOne.elementAt(index);
            Object oTwo = vTwo.elementAt(index);
            if (oOne instanceof Comparable && oTwo instanceof Comparable) {
                Comparable cOne = (Comparable) oOne;
                Comparable cTwo = (Comparable) oTwo;
                if (ascending) {
                    return cOne.compareTo(cTwo);
                } else {
                    return cTwo.compareTo(cOne);
                }
            }
        }
        return 1;
    }
}
