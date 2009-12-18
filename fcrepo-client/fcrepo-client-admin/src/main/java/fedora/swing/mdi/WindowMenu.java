/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.swing.mdi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyVetoException;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * Menu component that handles the functionality expected of a standard 
 * "Windows" menu for MDI applications.
 * 
 * <p>NOTICE: Portions created by Gerald Nunn are Copyright &copy; Gerald Nunn,
 * originally made available at
 * http://www.javaworld.com/javaworld/jw-05-2001/jw-0525-mdi.html
 * 
 * @author Gerald Nunn
 * @author Chris Wilper
 */
public class WindowMenu
        extends JMenu {

    private static final long serialVersionUID = 1L;

    private final MDIDesktopPane desktop;

    private final JMenuItem cascade = new JMenuItem("Cascade");

    private final JMenuItem tile = new JMenuItem("Tile");

    private final JMenuItem minAll = new JMenuItem("Minimize All");

    private final JMenuItem restoreAll = new JMenuItem("Restore All");

    public WindowMenu(MDIDesktopPane desktop, String name) {
        super(name);
        this.desktop = desktop;

        cascade.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                WindowMenu.this.desktop.cascadeFrames();
            }
        });
        tile.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                WindowMenu.this.desktop.tileFrames();
            }
        });
        minAll.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                WindowMenu.this.desktop.minimizeFrames();
            }
        });
        restoreAll.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                WindowMenu.this.desktop.restoreFrames();
            }
        });
        addMenuListener(new MenuListener() {

            public void menuCanceled(MenuEvent e) {
            }

            public void menuDeselected(MenuEvent e) {
                removeAll();
            }

            public void menuSelected(MenuEvent e) {
                buildChildMenus();
            }
        });
    }

    /* Sets up the children menus depending on the current desktop state */
    private void buildChildMenus() {
        int i;
        JInternalFrame[] array = desktop.getAllFrames();

        add(cascade);
        add(tile);
        add(minAll);
        add(restoreAll);
        if (array.length > 0) {
            addSeparator();
        }
        cascade.setEnabled(array.length > 0);
        tile.setEnabled(array.length > 0);
        minAll.setEnabled(desktop.deIconifiedFrames() > 0);
        restoreAll.setEnabled(desktop.iconifiedFrames() > 0);

        ChildMenuItem menu;
        for (i = 0; i < array.length; i++) {
            menu = new ChildMenuItem(array[i]);
            menu.setState(i == 0);
            menu.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    JInternalFrame frame =
                            ((ChildMenuItem) ae.getSource()).getFrame();
                    frame.moveToFront();
                    try {
                        frame.setSelected(true);
                    } catch (PropertyVetoException e) {
                        e.printStackTrace();
                    }
                }
            });
            menu.setIcon(array[i].getFrameIcon());
            add(menu);
        }
    }

    /*
     * This JCheckBoxMenuItem descendant is used to track the child frame that
     * corresponds to a give menu.
     */
    class ChildMenuItem
            extends JCheckBoxMenuItem {

        private static final long serialVersionUID = 1L;

        private final JInternalFrame frame;

        public ChildMenuItem(JInternalFrame frame) {
            super(frame.getTitle());
            this.frame = frame;
        }

        public JInternalFrame getFrame() {
            return frame;
        }
    }
}
