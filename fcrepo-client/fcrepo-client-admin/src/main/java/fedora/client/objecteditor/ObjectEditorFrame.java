/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.objecteditor;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.util.Collection;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import fedora.client.Administrator;

import fedora.server.types.gen.Datastream;
import fedora.server.types.gen.ObjectFields;

/**
 * An editing window that includes facilities for editing and viewing everything
 * about a digital object.
 *
 * @author Chris Wilper
 */
public class ObjectEditorFrame
        extends JInternalFrame
        implements PotentiallyDirty {

    private static final long serialVersionUID = 1L;

    private final ObjectPane m_objectPane;

    private final DatastreamsPane m_datastreamsPane;

    //   private DisseminatorsPane m_disseminatorsPane;
    private final JTabbedPane m_tabbedPane;

    private final String m_pid;

    //static ImageIcon objIcon =
    //        new ImageIcon(Administrator.cl
    //                .getResource("images/standard/general/Information16.gif"));

    //static ImageIcon dsIcon =
    //        new ImageIcon(Administrator.cl
    //                .getResource("images/standard/general/Copy16.gif"));

    //static ImageIcon dissIcon =
    //        new ImageIcon(Administrator.cl
    //                .getResource("images/standard/general/Refresh16.gif"));

    /**
     * Constructor. Queries the server for the object, builds the object and
     * component tabs, and populates them with the appropriate panels.
     */
    public ObjectEditorFrame(String pid, int startTab)
            throws Exception {
        super(pid, true, true, true, true);
        m_pid = pid;
        // query the server for key object fields
        ObjectFields o =
                Util.getObjectFields(pid, new String[] {"pid", "state",
                        "label", "cDate", "mDate", "ownerId"});
        String state = o.getState();
        String label = o.getLabel();
        String cDate = o.getCDate();
        String mDate = o.getMDate();
        String ownerId = o.getOwnerId();

        doTitle(false);

        // set up dirtiness check on close
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(new ObjectEditorClosingListener(pid));

        // outerPane(tabbedPane)
        ImageIcon objIcon =
            new ImageIcon(this.getClass().getClassLoader().getSystemClassLoader()
                          .getSystemResource("images/client/standard/general/Information16.gif"));

        ImageIcon dsIcon =
            new ImageIcon(this.getClass().getClassLoader().getSystemClassLoader()
                          .getSystemResource("images/client/standard/general/Copy16.gif"));

        ImageIcon dissIcon =
            new ImageIcon(this.getClass().getClassLoader().getSystemClassLoader()
                          .getSystemResource("images/client/standard/general/Refresh16.gif"));

        // tabbedPane(ObjectPane, DatastreamsPane, DisseminatorsPane)
        m_objectPane =
                new ObjectPane(this,
                               pid,
                               state,
                               label,
                               cDate,
                               mDate,
                               ownerId);
        m_datastreamsPane = new DatastreamsPane(this, pid);

        m_tabbedPane = new JTabbedPane();
        m_tabbedPane.addTab("Properties", m_objectPane);
        m_tabbedPane.setBackgroundAt(0, Administrator.DEFAULT_COLOR);
        m_tabbedPane.setIconAt(0, objIcon);
        m_tabbedPane.addTab("Datastreams", m_datastreamsPane);
        m_tabbedPane.setBackgroundAt(1, Administrator.DEFAULT_COLOR);
        m_tabbedPane.setIconAt(1, dsIcon);
        m_tabbedPane.setSelectedIndex(startTab);

        JPanel outerPane = new JPanel();
        outerPane.setLayout(new BorderLayout());
        outerPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        outerPane.add(m_tabbedPane, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(outerPane, BorderLayout.CENTER);
        //ImageIcon openIcon =
        //    new ImageIcon(ClassLoader.
        //                  getSystemResource("images/client/standard/general/Open16.gif"));
        //setFrameIcon(openIcon);
        pack();
        Dimension dims = getSize();
        if (dims.height < 675) {
            dims.height = 675;
        } else if (dims.height > 700) {
            dims.height = 700;
        }
        if (dims.width < 925) {
            dims.width = 925;
        } else if (dims.width > 950) {
            dims.width = 950;
        }
        setSize(dims);
        show();
    }

    public Datastream[] getCurrentDatastreamVersions() {
        Collection vColl = m_datastreamsPane.getCurrentVersionMap().values();
        Datastream[] versions = new Datastream[vColl.size()];
        Iterator iter = vColl.iterator();
        int i = 0;
        while (iter.hasNext()) {
            versions[i++] = (Datastream) iter.next();
        }
        return versions;
    }

    private void doTitle(boolean dirty) {
        String d = "";
        if (dirty) {
            d = "*";
        }

        setTitle("Object - " + m_pid + d);
    }

    public boolean isDirty() {
        return m_objectPane.isDirty() || m_datastreamsPane.isDirty();
    }

    public void indicateDirtiness() {
        int dirtyCount = 0;
        if (m_objectPane.isDirty()) {
            dirtyCount++;
            m_tabbedPane.setTitleAt(0, "Properties*");
        } else {
            m_tabbedPane.setTitleAt(0, "Properties");
        }
        if (m_datastreamsPane.isDirty()) {
            dirtyCount++;
            m_tabbedPane.setTitleAt(1, "Datastreams*");
        } else {
            m_tabbedPane.setTitleAt(1, "Datastreams");
        }
        //        if (m_disseminatorsPane!=null && m_disseminatorsPane.isDirty())
        //        {
        //            dirtyCount++;
        //            m_tabbedPane.setTitleAt(2, "Disseminators*");
        //        }
        //        else
        //        {
        //            if (m_disseminatorsPane!=null) m_tabbedPane.setTitleAt(2, "Disseminators");
        //        }
        if (dirtyCount > 0) {
            doTitle(true);
        } else {
            doTitle(false);
        }
    }

    /**
     * Listens for closing events and checks for object and component dirtiness.
     */
    protected class ObjectEditorClosingListener
            extends InternalFrameAdapter {

        private final String m_pid;

        public ObjectEditorClosingListener(String pid) {
            m_pid = pid;
        }

        /**
         * Check if any of the items being edited are dirty. If so, give the
         * user a chance to keep the editor open so they can save their changes.
         */
        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            if (isDirty()) {
                Object[] options = {"Yes", "No"};
                int selected =
                        JOptionPane
                                .showOptionDialog(null,
                                                  "Close "
                                                          + m_pid
                                                          + " without saving changes?",
                                                  "Unsaved changes",
                                                  JOptionPane.DEFAULT_OPTION,
                                                  JOptionPane.WARNING_MESSAGE,
                                                  null,
                                                  options,
                                                  options[0]);
                if (selected == 0) {
                    e.getInternalFrame().dispose();
                }
            } else {
                e.getInternalFrame().dispose();
            }
        }

    }

}
