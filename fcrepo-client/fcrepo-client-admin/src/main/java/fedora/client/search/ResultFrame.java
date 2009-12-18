/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.search;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicTableUI;

import fedora.client.Administrator;
import fedora.client.actions.ChangeObjectState;
import fedora.client.actions.ExportObject;
import fedora.client.actions.PurgeObject;
import fedora.client.actions.ViewObject;
import fedora.client.actions.ViewObjectXML;
import fedora.client.utility.AutoFinder;

import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.types.gen.ObjectFields;

import fedora.swing.jtable.DefaultSortTableModel;
import fedora.swing.jtable.JSortTable;

/**
 * @author Chris Wilper
 */
public class ResultFrame
        extends JInternalFrame {

    private static final long serialVersionUID = 1L;

    private JSortTable m_table;

    private String[] m_rowPids;

    private JButton m_moreButton;

    private AutoFinder m_finder = null;

    public ResultFrame(String frameTitle,
                       String[] displayFields,
                       String sessionToken) {
        super(frameTitle, true, true, true, true);
        try {
            m_finder = new AutoFinder(Administrator.APIA);
            searchAndDisplay(m_finder.resumeFindObjects(sessionToken),
                             displayFields);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: " + e.getClass().getName() + ":"
                    + e.getMessage());
        }
    }

    public ResultFrame(String frameTitle,
                       String[] displayFields,
                       int maxResults,
                       FieldSearchQuery query) {
        super(frameTitle, true, //resizable
              true, //closable
              true, //maximizable
              true);//iconifiable

        // Make sure resultFields has pid, even though they may not
        // want to display it. Also, signal that the pid should or
        // should not be displayed.
        boolean displayPid = false;
        for (String element : displayFields) {
            if (element.equals("pid")) {
                displayPid = true;
            }
        }
        String[] resultFields;
        if (displayPid) {
            resultFields = displayFields;
        } else {
            resultFields = new String[displayFields.length + 1];
            resultFields[0] = "pid";
            for (int i = 1; i < displayFields.length + 1; i++) {
                resultFields[i] = displayFields[i - 1];
            }
        }
        try {
            if (m_finder == null) {
                m_finder = new AutoFinder(Administrator.APIA);
            }
            searchAndDisplay(m_finder.findObjects(resultFields,
                                                  maxResults,
                                                  query), displayFields);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: " + e.getClass().getName() + ":"
                    + e.getMessage());
        }
    }

    private void searchAndDisplay(FieldSearchResult fsr, String[] displayFields)
            throws Exception {
        // put the resulting data into a structure suitable for display
        ObjectFields[] ofs = fsr.getResultList();
        Object[][] data = new Object[ofs.length][displayFields.length];
        // while adding the pids to m_rowPids so they can be used later
        m_rowPids = new String[ofs.length];
        for (int i = 0; i < ofs.length; i++) {
            ObjectFields o = ofs[i];
            m_rowPids[i] = o.getPid();
            for (int j = 0; j < displayFields.length; j++) {
                data[i][j] = getValue(o, displayFields[j]);
            }
        }

        DefaultSortTableModel model =
                new DefaultSortTableModel(data, displayFields);
        m_table = new JSortTable(model);
        m_table.setPreferredScrollableViewportSize(new Dimension(400, 400));
        m_table.setShowVerticalLines(false);
        m_table.setCellSelectionEnabled(false);
        m_table.setRowSelectionAllowed(true);
        m_table.setUI(new ResultFrame.BrowserTableUI());

        JScrollPane browsePanel = new JScrollPane(m_table);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(browsePanel, BorderLayout.CENTER);
        if (fsr.getListSession() != null
                && fsr.getListSession().getToken() != null) {
            m_moreButton = new JButton("More Results...");
            m_moreButton
                    .addActionListener(new MoreResultsListener(displayFields,
                                                               fsr
                                                                       .getListSession()
                                                                       .getToken(),
                                                               this));
            getContentPane().add(m_moreButton, BorderLayout.SOUTH);
        }
        ImageIcon zoomIcon =
            new ImageIcon(ClassLoader.
                          getSystemResource("images/client/standard/general/Zoom16.gif"));
        setFrameIcon(zoomIcon);
        pack();
        setSize(Administrator.getDesktop().getWidth() - 40, getSize().height);
    }

    protected void removeMoreResultsButton() {
        if (m_moreButton != null) {
            getContentPane().remove(m_moreButton);
        }
    }

    public String getValue(ObjectFields o, String name) {
        if (name.equals("pid")) {
            return o.getPid();
        }
        if (name.equals("label")) {
            return o.getLabel();
        }
        if (name.equals("state")) {
            return o.getState();
        }
        if (name.equals("ownerId")) {
            return o.getOwnerId();
        }
        if (name.equals("cDate")) {
            return o.getCDate();
        }
        if (name.equals("mDate")) {
            return o.getMDate();
        }
        if (name.equals("dcmDate")) {
            return o.getDcmDate();
        }
        if (name.equals("title")) {
            return getList(o.getTitle());
        }
        if (name.equals("creator")) {
            return getList(o.getCreator());
        }
        if (name.equals("subject")) {
            return getList(o.getSubject());
        }
        if (name.equals("description")) {
            return getList(o.getDescription());
        }
        if (name.equals("publisher")) {
            return getList(o.getPublisher());
        }
        if (name.equals("contributor")) {
            return getList(o.getContributor());
        }
        if (name.equals("date")) {
            return getList(o.getDate());
        }
        if (name.equals("type")) {
            return getList(o.getType());
        }
        if (name.equals("format")) {
            return getList(o.getFormat());
        }
        if (name.equals("identifier")) {
            return getList(o.getIdentifier());
        }
        if (name.equals("source")) {
            return getList(o.getSource());
        }
        if (name.equals("language")) {
            return getList(o.getLanguage());
        }
        if (name.equals("relation")) {
            return getList(o.getRelation());
        }
        if (name.equals("coverage")) {
            return getList(o.getCoverage());
        }
        if (name.equals("rights")) {
            return getList(o.getRights());
        }
        return null;
    }

    public String getList(String[] s) {
        if (s == null) {
            return "";
        }
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < s.length; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(s[i]);
        }
        return out.toString();
    }

    public class MoreResultsListener
            implements ActionListener {

        String[] m_displayFields;

        String m_sessionToken;

        ResultFrame m_parent;

        public MoreResultsListener(String[] displayFields,
                                   String sessionToken,
                                   ResultFrame parent) {
            m_displayFields = displayFields;
            m_sessionToken = sessionToken;
            m_parent = parent;
        }

        public void actionPerformed(ActionEvent e) {
            m_parent.removeMoreResultsButton();
            ResultFrame frame =
                    new ResultFrame("More Search Results",
                                    m_displayFields,
                                    m_sessionToken);
            frame.setVisible(true);
            Administrator.getDesktop().add(frame);
            try {
                frame.setSelected(true);
            } catch (java.beans.PropertyVetoException pve) {
            }
        }
    }

    public class BrowserTableUI
            extends BasicTableUI {

        @Override
        protected MouseInputListener createMouseInputListener() {
            return new BasicTableUI.MouseInputHandler() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int rowNum =
                                m_table
                                        .rowAtPoint(new Point(e.getX(), e
                                                .getY()));
                        if (rowNum >= 0) {
                            // launch object viewer to view object
                            new ViewObject(m_rowPids[rowNum]).launch();
                        }
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        int rowNum =
                                m_table
                                        .rowAtPoint(new Point(e.getX(), e
                                                .getY()));
                        if (rowNum >= 0) {
                            int[] sRows = m_table.getSelectedRows();
                            boolean clickedOnSelected = false;
                            HashSet pids = new HashSet();
                            for (int element : sRows) {
                                if (element == rowNum) {
                                    clickedOnSelected = true;
                                }
                                pids.add(m_rowPids[element]);
                            }
                            if (!clickedOnSelected) {
                                pids = new HashSet();
                                m_table.clearSelection();
                                m_table.addRowSelectionInterval(rowNum, rowNum);
                                pids.add(m_rowPids[rowNum]);
                            }
                            if (pids.size() == 1) {
                                Iterator pidIter = pids.iterator();
                                new ResultFrame.SingleSelectionPopup((String) pidIter
                                        .next()).show(e.getComponent(), e
                                        .getX(), e.getY());
                            } else {
                                new ResultFrame.MultiSelectionPopup(pids)
                                        .show(e.getComponent(), e.getX(), e
                                                .getY());
                            }
                        }
                    } else {
                        // not a right click
                        super.mousePressed(e);
                    }
                }
            };
        }
    }

    public class SingleSelectionPopup
            extends JPopupMenu {

        private static final long serialVersionUID = 1L;

        public SingleSelectionPopup(String pid) {
            super();
            JMenuItem i0 = new JMenuItem(new ViewObject(pid));
            i0.setMnemonic(KeyEvent.VK_O);
            i0.setToolTipText("Launches a viewer for the selected object.");
            JMenuItem i1 = new JMenuItem(new ViewObjectXML(pid));
            i1.setMnemonic(KeyEvent.VK_V);
            i1
                    .setToolTipText("Launches an XML viewer for the selected object.");
            JMenuItem i2 = new JMenuItem(new ExportObject(pid));
            i2.setMnemonic(KeyEvent.VK_E);
            i2.setToolTipText("Exports the selected object.");
            JMenuItem i3 = new JMenuItem(new PurgeObject(pid));
            i3.setMnemonic(KeyEvent.VK_P);
            i3
                    .setToolTipText("Removes the selected object from the repository.");
            add(i0);
            add(i1);
            add(i2);
            add(i3);
            JMenu m1 = new JMenu("Set object state to");
            JMenuItem activeItem =
                    new JMenuItem(new ChangeObjectState(pid, "Active"));
            JMenuItem inactiveItem =
                    new JMenuItem(new ChangeObjectState(pid, "Inactive"));
            JMenuItem deletedItem =
                    new JMenuItem(new ChangeObjectState(pid, "Deleted"));
            m1.add(activeItem);
            m1.add(inactiveItem);
            m1.add(deletedItem);
            add(m1);
        }
    }

    public class MultiSelectionPopup
            extends JPopupMenu {

        private static final long serialVersionUID = 1L;

        public MultiSelectionPopup(Set pids) {
            super();
            JMenuItem i0 = new JMenuItem(new ViewObject(pids));
            i0.setMnemonic(KeyEvent.VK_O);
            i0.setToolTipText("Launches a viewer for the selected objects.");
            JMenuItem i1 = new JMenuItem(new ViewObjectXML(pids));
            i1.setMnemonic(KeyEvent.VK_V);
            i1
                    .setToolTipText("Launches an XML viewer for the selected objects.");
            JMenuItem i2 = new JMenuItem(new ExportObject(pids));
            i2.setMnemonic(KeyEvent.VK_E);
            i2.setToolTipText("Exports the selected objects.");
            JMenuItem i3 = new JMenuItem(new PurgeObject(pids));
            i3.setMnemonic(KeyEvent.VK_P);
            i3
                    .setToolTipText("Removes the selected objects from the repository.");
            add(i0);
            add(i1);
            add(i2);
            add(i3);
            JMenu m1 = new JMenu("Set object states to");
            JMenuItem activeItem =
                    new JMenuItem(new ChangeObjectState(pids, "Active"));
            JMenuItem inactiveItem =
                    new JMenuItem(new ChangeObjectState(pids, "Inactive"));
            JMenuItem deletedItem =
                    new JMenuItem(new ChangeObjectState(pids, "Deleted"));
            m1.add(activeItem);
            m1.add(inactiveItem);
            m1.add(deletedItem);
            add(m1);
        }
    }
}
