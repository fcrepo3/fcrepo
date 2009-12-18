/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.swing.jtable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * SortArrowIcon.
 * 
 * <p>NOTICE: Portions created by Claude Duguay are Copyright &copy; 
 * Claude Duguay, originally made available at 
 * http://www.fawcette.com/javapro/2002_08/magazine/columns/visualcomponents/
 * 
 * @author Claude Duguay
 * @author Chris Wilper
 */
public class SortArrowIcon
        implements Icon {

    public static final int NONE = 0;

    public static final int DECENDING = 1;

    public static final int ASCENDING = 2;

    protected int direction;

    protected int width = 8;

    protected int height = 8;

    public SortArrowIcon(int direction) {
        this.direction = direction;
    }

    public int getIconWidth() {
        return width;
    }

    public int getIconHeight() {
        return height;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Color bg = c.getBackground();
        Color light = bg.brighter();
        Color shade = bg.darker();

        int w = width;
        int h = height;
        int m = w / 2;
        if (direction == ASCENDING) {
            g.setColor(shade);
            g.drawLine(x, y, x + w, y);
            g.drawLine(x, y, x + m, y + h);
            g.setColor(light);
            g.drawLine(x + w, y, x + m, y + h);
        }
        if (direction == DECENDING) {
            g.setColor(shade);
            g.drawLine(x + m, y, x, y + h);
            g.setColor(light);
            g.drawLine(x, y + h, x + w, y + h);
            g.drawLine(x + m, y, x + w, y + h);
        }
    }
}
