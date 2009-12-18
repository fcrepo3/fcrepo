/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.swing.mdi;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;

import java.beans.PropertyVetoException;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

/**
 * An extension of WDesktopPane that supports often used MDI functionality.
 * 
 * <p>This class also handles setting scroll bars for when windows move too far
 * to the left or bottom, providing the MDIDesktopPane is in a ScrollPane.
 * 
 * <p>NOTICE: Portions created by Gerald Nunn are Copyright &copy; Gerald Nunn,
 * originally made available at
 * http://www.javaworld.com/javaworld/jw-05-2001/jw-0525-mdi.html
 * 
 * @author Gerald Nunn
 * @author Chris Wilper
 */
public class MDIDesktopPane
        extends JDesktopPane {

    private static final long serialVersionUID = 1L;

    private static int FRAME_OFFSET = 20;

    private final MDIDesktopManager manager;

    public MDIDesktopPane() {
        manager = new MDIDesktopManager(this);
        setDesktopManager(manager);
        // setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        setDragMode(JDesktopPane.LIVE_DRAG_MODE);
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        checkDesktopSize();
    }

    public Component add(JInternalFrame frame) {
        JInternalFrame[] array = getAllFrames();
        Point p;
        Component retval = super.add(frame);
        checkDesktopSize();
        if (array.length > 0) {
            p = array[0].getLocation();
            p.x = p.x + FRAME_OFFSET;
            p.y = p.y + FRAME_OFFSET;
        } else {
            p = new Point(0, 0);
        }
        frame.setLocation(p.x, p.y);
        moveToFront(frame);
        frame.setVisible(true);
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException e) {
            frame.toBack();
        }
        return retval;
    }

    @Override
    public void remove(Component c) {
        super.remove(c);
        checkDesktopSize();
    }

    /**
     * Cascade all internal frames, un-iconfying any minimized first
     */
    public void cascadeFrames() {
        restoreFrames();
        int x = 0;
        int y = 0;
        JInternalFrame allFrames[] = getAllFrames();

        manager.setNormalSize();
        int frameHeight =
                getBounds().height - 5 - allFrames.length * FRAME_OFFSET;
        int frameWidth =
                getBounds().width - 5 - allFrames.length * FRAME_OFFSET;
        for (int i = allFrames.length - 1; i >= 0; i--) {
            allFrames[i].setSize(frameWidth, frameHeight);
            allFrames[i].setLocation(x, y);
            x = x + FRAME_OFFSET;
            y = y + FRAME_OFFSET;
        }
    }

    /**
     * Tile all internal frames, un-iconifying any minimized first
     */
    public void tileFrames() {
        restoreFrames();
        java.awt.Component allFrames[] = getAllFrames();
        manager.setNormalSize();
        int frameHeight = getBounds().height / allFrames.length;
        int y = 0;
        for (Component element : allFrames) {
            element.setSize(getBounds().width, frameHeight);
            element.setLocation(0, y);
            y = y + frameHeight;
        }
    }

    public void minimizeFrames() {
        JInternalFrame[] array = getAllFrames();
        for (JInternalFrame element : array) {
            try {
                element.setIcon(true);
            } catch (PropertyVetoException pve) {
            }
        }
    }

    public void restoreFrames() {
        JInternalFrame[] array = getAllFrames();
        for (JInternalFrame element : array) {
            try {
                element.setIcon(false);
            } catch (PropertyVetoException pve) {
            }
        }
    }

    public int deIconifiedFrames() {
        int c = 0;
        JInternalFrame[] array = getAllFrames();
        for (int i = 0; i < array.length; i++) {
            if (!array[i].isIcon()) {
                c++;
            }
        }
        return c;
    }

    public int iconifiedFrames() {
        int c = 0;
        JInternalFrame[] array = getAllFrames();
        for (JInternalFrame element : array) {
            if (element.isIcon()) {
                c++;
            }
        }
        return c;
    }

    /**
     * Sets all component size properties ( maximum, minimum, preferred) to the
     * given dimension.
     */
    public void setAllSize(Dimension d) {
        setMinimumSize(d);
        setMaximumSize(d);
        setPreferredSize(d);
    }

    /**
     * Sets all component size properties ( maximum, minimum, preferred) to the
     * given width and height.
     */
    public void setAllSize(int width, int height) {
        setAllSize(new Dimension(width, height));
    }

    private void checkDesktopSize() {
        if (getParent() != null && isVisible()) {
            manager.resizeDesktop();
        }
    }
}

/**
 * Private class used to replace the standard DesktopManager for JDesktopPane.
 * Used to provide scrollbar functionality.
 */
class MDIDesktopManager
        extends DefaultDesktopManager {

    private static final long serialVersionUID = 1L;

    private final MDIDesktopPane desktop;

    public MDIDesktopManager(MDIDesktopPane desktop) {
        this.desktop = desktop;
    }

    @Override
    public void endResizingFrame(JComponent f) {
        super.endResizingFrame(f);
        resizeDesktop();
    }

    @Override
    public void endDraggingFrame(JComponent f) {
        super.endDraggingFrame(f);
        resizeDesktop();
    }

    public void setNormalSize() {
        JScrollPane scrollPane = getScrollPane();
        int x = 0;
        int y = 0;
        Insets scrollInsets = getScrollPaneInsets();

        if (scrollPane != null) {
            Dimension d = scrollPane.getVisibleRect().getSize();
            if (scrollPane.getBorder() != null) {
                d
                        .setSize(d.getWidth() - scrollInsets.left
                                - scrollInsets.right, d.getHeight()
                                - scrollInsets.top - scrollInsets.bottom);
            }

            d.setSize(d.getWidth() - 20, d.getHeight() - 20);
            desktop.setAllSize(x, y);
            scrollPane.invalidate();
            scrollPane.validate();
        }
    }

    private Insets getScrollPaneInsets() {
        JScrollPane scrollPane = getScrollPane();
        if (scrollPane == null) {
            return new Insets(0, 0, 0, 0);
        } else {
            return getScrollPane().getBorder().getBorderInsets(scrollPane);
        }
    }

    private JScrollPane getScrollPane() {
        if (desktop.getParent() instanceof JViewport) {
            JViewport viewPort = (JViewport) desktop.getParent();
            if (viewPort.getParent() instanceof JScrollPane) {
                return (JScrollPane) viewPort.getParent();
            }
        }
        return null;
    }

    protected void resizeDesktop() {
        int x = 0;
        int y = 0;
        JScrollPane scrollPane = getScrollPane();
        Insets scrollInsets = getScrollPaneInsets();

        if (scrollPane != null) {
            JInternalFrame allFrames[] = desktop.getAllFrames();
            for (JInternalFrame element : allFrames) {
                if (element.getX() + element.getWidth() > x) {
                    x = element.getX() + element.getWidth();
                }
                if (element.getY() + element.getHeight() > y) {
                    y = element.getY() + element.getHeight();
                }
            }
            Dimension d = scrollPane.getVisibleRect().getSize();
            if (scrollPane.getBorder() != null) {
                d
                        .setSize(d.getWidth() - scrollInsets.left
                                - scrollInsets.right, d.getHeight()
                                - scrollInsets.top - scrollInsets.bottom);
            }

            if (x <= d.getWidth()) {
                x = (int) d.getWidth() - 20;
            }
            if (y <= d.getHeight()) {
                y = (int) d.getHeight() - 20;
            }
            desktop.setAllSize(x, y);
            scrollPane.invalidate();
            scrollPane.validate();
        }
    }
}
