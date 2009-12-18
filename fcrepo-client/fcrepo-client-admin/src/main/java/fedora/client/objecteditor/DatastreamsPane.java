/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.objecteditor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import fedora.client.Administrator;

import fedora.server.types.gen.Datastream;

/**
 * Shows a tabbed pane, one for each datastream in the object, and one special
 * tab for "New...", which handles the creation of new datastreams.
 *
 * @author Chris Wilper
 */
public class DatastreamsPane
        extends JPanel
        implements PotentiallyDirty, TabDrawer {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DatastreamsPane.class.getName());

    private static final long serialVersionUID = 1L;

    private final String m_pid;

    private final JTabbedPane m_tabbedPane;

    private DatastreamPane[] m_datastreamPanes;

    private final ObjectEditorFrame m_owner;

    private final ArrayList m_dsListeners;

    private final Map m_currentVersionMap;

    public String[] ALL_KNOWN_MIMETYPES =
            new String[] {"text/xml", "text/plain", "text/html",
                    "text/html+xml", "text/svg+xml", "text/rtf", "image/jpeg",
                    "image/jp2", "image/gif", "image/bmp", "image/png",
                    "image/tiff", "audio/mpeg", "audio/x-aiff", "audio/x-wav",
                    "audio/x-pn-realaudio", "video/mpeg", "video/quicktime",
                    "application/postscript", "application/pdf",
                    "application/rdf+xml", "application/ms-word",
                    "application/ms-excel", "application/ms-powerpoint",
                    "application/smil", "application/octet-stream",
                    "application/x-tar", "application/zip",
                    "application/x-gtar", "application/x-gzip",
                    "application/xml", "application/xhtml+xml",
                    "application/xslt+xml", "application/xml-dtd"};

    public String[] XML_MIMETYPE = new String[] {"text/xml"};

    ImageIcon newIcon = null;

    //static ImageIcon newIcon =
    //        new ImageIcon(Administrator.cl
    //                .getResource("images/standard/general/New16.gif"));

    /**
     * Build the pane.
     */
    public DatastreamsPane(ObjectEditorFrame owner, String pid)
            throws Exception {
        m_pid = pid;
        m_owner = owner;
        m_currentVersionMap = new HashMap();
        // this(m_tabbedPane)
        m_dsListeners = new ArrayList();

        newIcon =
            new ImageIcon(this.getClass().getClassLoader().getSystemClassLoader()
                    .getSystemResource("images/client/standard/general/New16.gif"));

        // m_tabbedPane(DatastreamPane[])

        m_tabbedPane = new JTabbedPane(SwingConstants.LEFT);
        Datastream currentVersions[] =
                Administrator.APIM.getDatastreams(pid, null, null);
        m_datastreamPanes = new DatastreamPane[currentVersions.length];
        for (int i = 0; i < currentVersions.length; i++) {
            m_currentVersionMap.put(currentVersions[i].getID(),
                                    currentVersions[i]);
            m_datastreamPanes[i] =
                    new DatastreamPane(owner, pid, Administrator.APIM
                            .getDatastreamHistory(pid, currentVersions[i]
                                    .getID()), this);
            StringBuffer tabLabel = new StringBuffer();
            tabLabel.append(currentVersions[i].getID());
            m_tabbedPane.add(tabLabel.toString(), m_datastreamPanes[i]);
            m_tabbedPane.setToolTipTextAt(i, currentVersions[i].getMIMEType()
                    + " - " + currentVersions[i].getLabel() + " ("
                    + currentVersions[i].getControlGroup().toString() + ")");
            colorTabForState(currentVersions[i].getID(), currentVersions[i]
                    .getState());
        }
        m_tabbedPane.add("New...", new JPanel());

        setLayout(new BorderLayout());
        add(m_tabbedPane, BorderLayout.CENTER);
        doNew(XML_MIMETYPE, false);
        updateNewRelsExt(m_pid);
    }

    private boolean hasRelsExt() {
        if (m_currentVersionMap.get("RELS-EXT") != null) {
            return true;
        }
        return false;
    }

    public Map getCurrentVersionMap() {
        return m_currentVersionMap;
    }

    public void colorTabForState(String id, String s) {
        int i = getTabIndex(id);
        if (s.equals("I")) {
            m_tabbedPane.setBackgroundAt(i, Administrator.INACTIVE_COLOR);
        } else if (s.equals("D")) {
            m_tabbedPane.setBackgroundAt(i, Administrator.DELETED_COLOR);
        } else {
            m_tabbedPane.setBackgroundAt(i, Administrator.ACTIVE_COLOR);
        }
    }

    /**
     * Set the content of the "New..." JPanel to a fresh new datastream entry
     * panel, and switch to it, if needed.
     */
    public void doNew(String[] dropdownMimeTypes, boolean makeSelected) {
        int i = getTabIndex("New...");

        m_tabbedPane
                .setComponentAt(i, new NewDatastreamPane(dropdownMimeTypes));
        i = getTabIndex("New...");
        m_tabbedPane.setToolTipTextAt(i, "Add a new datastream to this object");
        m_tabbedPane.setIconAt(i, newIcon);
        m_tabbedPane.setBackgroundAt(i, Administrator.DEFAULT_COLOR);
        if (makeSelected) {
            m_tabbedPane.setSelectedIndex(i);
        }
    }

    /**
     * Set the content of the "New Rels-Ext..." JPanel to a fresh new datastream
     * entry panel, and switch to it, if needed.
     *
     * @throws Exception
     */
    public void updateNewRelsExt(String pid) throws Exception {
        if (!hasRelsExt() && getTabIndex("New RELS-EXT...") == -1) {
            m_tabbedPane.insertTab("New RELS-EXT...",
                                   newIcon,
                                   new NewRelsExtDatastreamPane(m_owner,
                                                                pid,
                                                                this),
                                   "Add a RELS-EXT datastream to this object",
                                   getTabIndex("New..."));
            int i = getTabIndex("New RELS-EXT...");
            m_tabbedPane.setBackgroundAt(i, Administrator.DEFAULT_COLOR);
        } else if (hasRelsExt() && getTabIndex("New RELS-EXT...") != -1) {
            m_tabbedPane.remove(getTabIndex("New RELS-EXT..."));
        }
    }

    private int getTabIndex(String id) {
        int i = m_tabbedPane.indexOfTab(id);
        if (i != -1) {
            return i;
        }
        return m_tabbedPane.indexOfTab(id + "*");
    }

    /**
     * Gets the index of the pane containing the
     * datastream with the given id.
     * @return index, or -1 if index is not found
     */
    private int getDatastreamPaneIndex(String id) {
        int index = -1;
        for (int i=0; i < m_datastreamPanes.length; i++)
        {
            if(m_datastreamPanes[i].getItemId().equals(id)){
                index = i;
                break;
            }
        }
        return index;
    }

    public void setDirty(String id, boolean isDirty) {
        int i = getTabIndex(id);
        if (isDirty) {
            m_tabbedPane.setTitleAt(i, id + "*");
        } else {
            m_tabbedPane.setTitleAt(i, id);
        }
    }

    /**
     * Refresh the content of the tab for the indicated datastream with the
     * latest information from the server.
     */
    protected void refresh(String dsID) {
        int i = getTabIndex(dsID);
        try {
            Datastream[] versions =
                    Administrator.APIM.getDatastreamHistory(m_pid, dsID);
            m_currentVersionMap.put(dsID, versions[0]);
            LOG.debug("New create date is: " + versions[0].getCreateDate());
            DatastreamPane replacement =
                    new DatastreamPane(m_owner, m_pid, versions, this);
            m_datastreamPanes[i] = replacement;
            m_tabbedPane.setComponentAt(i, replacement);
            m_tabbedPane.setToolTipTextAt(i, versions[0].getMIMEType() + " - "
                    + versions[0].getLabel() + " ("
                    + versions[0].getControlGroup().toString() + ")");
            colorTabForState(dsID, versions[0].getState());
            setDirty(dsID, false);
        } catch (Exception e) {
            Administrator
                    .showErrorDialog(Administrator.getDesktop(),
                                     "Error while refreshing",
                                     e.getMessage()
                                             + "\nTry re-opening the object viewer.",
                                     e);
        }
    }

    /**
     * Add a new tab with a new datastream.
     */
    protected void addDatastreamTab(String dsID, boolean reInitNewPanel)
            throws Exception {
        DatastreamPane[] newArray =
                new DatastreamPane[m_datastreamPanes.length + 1];
        for (int i = 0; i < m_datastreamPanes.length; i++) {
            newArray[i] = m_datastreamPanes[i];
        }
        Datastream[] versions =
                Administrator.APIM.getDatastreamHistory(m_pid, dsID);
        m_currentVersionMap.put(dsID, versions[0]);
        newArray[m_datastreamPanes.length] =
                new DatastreamPane(m_owner, m_pid, versions, this);
        // swap the arrays
        m_datastreamPanes = newArray;
        int newIndex = getTabIndex("New...");
        m_tabbedPane.add(m_datastreamPanes[m_datastreamPanes.length - 1],
                         newIndex);
        m_tabbedPane.setTitleAt(newIndex, dsID);
        m_tabbedPane.setToolTipTextAt(newIndex, versions[0].getMIMEType()
                + " - " + versions[0].getLabel() + " ("
                + versions[0].getControlGroup().toString() + ")");
        colorTabForState(dsID, versions[0].getState());
        if (reInitNewPanel) {
            doNew(XML_MIMETYPE, false);
        }

        updateNewRelsExt(m_pid);
        m_tabbedPane.setSelectedIndex(getTabIndex(dsID));
    }

    protected void remove(String dsID) {
        int i = getTabIndex(dsID);
        m_tabbedPane.remove(i);
        m_currentVersionMap.remove(dsID);

        // also remove it from the array
        i = getDatastreamPaneIndex(dsID);
        DatastreamPane[] newArray =
                new DatastreamPane[m_datastreamPanes.length - 1];
        for (int x = 0; x < m_datastreamPanes.length; x++) {
            if (x < i) {
                newArray[x] = m_datastreamPanes[x];
            } else if (x > i) {
                newArray[x - 1] = m_datastreamPanes[x];
            }
        }
        m_datastreamPanes = newArray;

        // then make sure dirtiness indicators are corrent
        m_owner.indicateDirtiness();
        // add the new rels-ext tab back if the RELS-EXT datastream was deleted.
        try {
            updateNewRelsExt(m_pid);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean isDirty() {
        for (DatastreamPane element : m_datastreamPanes) {
            if (element.isDirty()) {
                return true;
            }
        }
        return false;
    }

    public void addRows(JComponent[] left,
                        JComponent[] right,
                        GridBagLayout gridBag,
                        Container container) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 4, 4, 4);
        for (int i = 0; i < left.length; i++) {
            c.anchor = GridBagConstraints.NORTHWEST;
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE; //reset to default
            c.weightx = 0.0; //reset to default
            gridBag.setConstraints(left[i], c);
            container.add(left[i]);

            c.gridwidth = GridBagConstraints.REMAINDER; //end row
            if (!(right[i] instanceof JComboBox)) {
                c.fill = GridBagConstraints.HORIZONTAL;
            } else {
                c.anchor = GridBagConstraints.NORTHWEST;
            }
            c.weightx = 1.0;
            gridBag.setConstraints(right[i], c);
            container.add(right[i]);
        }

    }

    public class NewDatastreamPane
            extends JPanel
            implements ActionListener {

        private static final long serialVersionUID = 1L;

        JTextField m_labelTextField;

        JTextField m_idTextField;

        JTextField m_formatURITextField;

        JTextField m_altIDsTextField;

        JTextField m_referenceTextField;

        JTextArea m_controlGroupTextArea;

        JComboBox m_mimeComboBox;

        CardLayout m_contentCard;

        JPanel m_specificPane;

        TextContentEditor m_xEditor = null;

        TextContentEditor m_mEditor = null;

        JPanel m_erPane;

        JButton m_erViewButton;

        ContentViewer m_erViewer;

        JPanel m_checksumPanel;

        JComboBox m_checksumTypeComboBox;

        JTextField m_checksumValue;

        String m_controlGroup;

        String m_lastSelectedMimeType;

        File m_managedFile;

        JComponent m_mCenter;

        JPanel m_mPane;

        static final String X_DESCRIPTION =
                "Metadata that is stored and managed inside the "
                        + "repository.  This must be well-formed XML and will be "
                        + "stripped of processing instructions and comments."
                        + "Use of XML namespaces is optional and schema validity is "
                        + "not enforced by the repository.";

        static final String M_DESCRIPTION =
                "Arbitary content that is stored and managed inside the "
                        + "repository.  This is similar to internal XML metadata, but it does not have "
                        + "any format restrictions, and is delieved as-is from the repository.";

        static final String E_DESCRIPTION =
                "Content that is not managed by Fedora, "
                        + "and is ultimately hosted on some other server.  Each time the "
                        + "content is accessed, Fedora will request it from its host and "
                        + "send it to the client.";

        static final String R_DESCRIPTION =
                "Fedora will send clients a redirect to the URL "
                        + "you specify for this datastream.  This is useful in situations where the content "
                        + "must be delivered by a special streaming server, it contains "
                        + "relative hyperlinks, or there are licensing or access restrictions that prevent "
                        + "it from being proxied.";

        private final JComboBox m_stateComboBox;

        private final JComboBox m_versionableComboBox;

        private String m_initialState;

        private final static int NEW_VERSION_ON_UPDATE = 0;

        public NewDatastreamPane(String[] dropdownMimeTypes) {

            JComponent[] left =
                    new JComponent[] {new JLabel("ID"),
                            new JLabel("Control Group"), new JLabel("State"),
                            new JLabel("Versionable"), new JLabel("MIME Type"),
                            new JLabel("Label"), new JLabel("Format URI"),
                            new JLabel("Alternate IDs"), new JLabel("Checksum")};

            m_stateComboBox =
                    new JComboBox(new String[] {"Active", "Inactive", "Deleted"});
            m_initialState = "A";
            m_stateComboBox.setBackground(Administrator.ACTIVE_COLOR);
            Administrator.constrainHeight(m_stateComboBox);
            m_stateComboBox.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    m_initialState =
                            ((String) m_stateComboBox.getSelectedItem())
                                    .substring(0, 1);
                    if (m_initialState.equals("A")) {
                        m_stateComboBox
                                .setBackground(Administrator.ACTIVE_COLOR);
                    } else if (m_initialState.equals("I")) {
                        m_stateComboBox
                                .setBackground(Administrator.INACTIVE_COLOR);
                    } else if (m_initialState.equals("D")) {
                        m_stateComboBox
                                .setBackground(Administrator.DELETED_COLOR);
                    }
                }
            });
            String[] comboBoxStrings2 =
                    {"Updates will create new version",
                            "Updates will replace most recent version"};
            m_versionableComboBox = new JComboBox(comboBoxStrings2);
            Administrator.constrainHeight(m_versionableComboBox);
            m_versionableComboBox.setSelectedIndex(NEW_VERSION_ON_UPDATE);

            m_labelTextField = new JTextField("Enter a label here.");

            m_idTextField = new JTextField("");
            m_formatURITextField = new JTextField("");
            m_altIDsTextField = new JTextField("");

            m_mimeComboBox = new JComboBox(dropdownMimeTypes);
            Administrator.constrainHeight(m_mimeComboBox);
            m_mimeComboBox.setEditable(true);
            JPanel controlGroupPanel = new JPanel();
            JRadioButton xButton = new JRadioButton("Internal XML Metadata");
            xButton.setSelected(true);
            m_controlGroup = "X";
            xButton.setActionCommand("X");
            xButton.addActionListener(this);
            JRadioButton mButton = new JRadioButton("Managed Content");
            mButton.setActionCommand("M");
            mButton.addActionListener(this);
            JRadioButton eButton =
                    new JRadioButton("External Referenced Content");
            eButton.setActionCommand("E");
            eButton.addActionListener(this);
            JRadioButton rButton = new JRadioButton("Redirect");
            rButton.setActionCommand("R");
            rButton.addActionListener(this);
            ButtonGroup group = new ButtonGroup();
            group.add(xButton);
            group.add(mButton);
            group.add(eButton);
            group.add(rButton);
            controlGroupPanel.setLayout(new GridLayout(0, 1));
            controlGroupPanel.add(xButton);
            controlGroupPanel.add(mButton);
            controlGroupPanel.add(eButton);
            controlGroupPanel.add(rButton);
            JPanel controlGroupOuterPanel = new JPanel(new BorderLayout());
            controlGroupOuterPanel.add(controlGroupPanel, BorderLayout.WEST);
            m_controlGroupTextArea = new JTextArea(X_DESCRIPTION);
            m_controlGroupTextArea.setLineWrap(true);
            m_controlGroupTextArea.setEditable(false);
            m_controlGroupTextArea.setWrapStyleWord(true);
            m_controlGroupTextArea.setBackground(controlGroupOuterPanel
                    .getBackground());

            controlGroupOuterPanel.add(m_controlGroupTextArea,
                                       BorderLayout.CENTER);
            m_checksumPanel = new JPanel();
            m_checksumPanel.setLayout(new BorderLayout());
            m_checksumTypeComboBox =
                    new JComboBox(new String[] {"Default", "DISABLED", "MD5",
                            "SHA-1", "SHA-256", "SHA-384", "SHA-512"});

            m_checksumValue = null;
            m_checksumPanel.add(m_checksumTypeComboBox, BorderLayout.WEST);
            m_checksumTypeComboBox.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    String csType =
                            m_checksumTypeComboBox.getSelectedItem().toString();
                    if (csType.equals("Default") || csType.equals("DISABLED")) {
                        if (m_checksumValue != null) {
                            m_checksumPanel.remove(m_checksumValue);
                            m_checksumValue = null;
                            m_checksumPanel.validate();
                            m_checksumPanel.repaint();
                        }
                    } else {
                        if (m_checksumValue != null) {
                            m_checksumPanel.remove(m_checksumValue);
                        }
                        m_checksumValue = new JTextField("");
                        m_checksumPanel.add(m_checksumValue,
                                            BorderLayout.CENTER);
                        m_checksumPanel.validate();
                    }
                }
            });

            JComponent[] right =
                    new JComponent[] {m_idTextField, controlGroupOuterPanel,
                            m_stateComboBox, m_versionableComboBox,
                            m_mimeComboBox, m_labelTextField,
                            m_formatURITextField, m_altIDsTextField,
                            m_checksumPanel};

            JPanel commonPane = new JPanel();
            GridBagLayout grid = new GridBagLayout();
            commonPane.setLayout(grid);
            addRows(left, right, grid, commonPane);

            m_lastSelectedMimeType = (String) m_mimeComboBox.getSelectedItem();
            m_mimeComboBox.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    String cur = (String) m_mimeComboBox.getSelectedItem();
                    if (!cur.equals(m_lastSelectedMimeType)) {
                        // X: remove the xml parsing restriction if needed
                        m_xEditor.setXML(cur.endsWith("+xml")
                                || cur.endsWith("/xml"));
                        // E/R: in any case, remove the prior viewer
                        if (m_erViewer != null) {
                            m_erPane.remove(m_erViewer.getComponent());
                            m_erPane.add(new JLabel(), BorderLayout.CENTER);
                            m_erPane.validate();
                        }
                        if (ContentHandlerFactory.hasViewer(cur)) {
                            m_erViewButton.setEnabled(true);
                        } else {
                            m_erViewButton.setEnabled(false);
                        }
                        // remember the mime type
                        m_lastSelectedMimeType = cur;
                    }
                }
            });

            /*
             * right=new JComponent[] { m_mdClassComboBox, m_mdTypeComboBox };
             * JPanel xTopPane=new JPanel(); grid=new GridBagLayout();
             * xTopPane.setLayout(grid); addRows(left, right, grid, xTopPane);
             */
            try {
                m_xEditor = new TextContentEditor();
                m_xEditor
                        .init("text/plain",
                              new ByteArrayInputStream(new String("Enter content here, or click \"Import\" below.")
                                      .getBytes("UTF-8")),
                              false);
                m_xEditor.setXML(true); // inline xml is always going to be xml,
                // initted as text/plain because empty!=valid xml
            } catch (Exception e) {
            }
            JPanel xBottomPane = new JPanel();
            xBottomPane.setLayout(new FlowLayout());
            JButton xImportButton = new JButton("Import...");
            Administrator.constrainHeight(xImportButton);
            xImportButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    ImportDialog imp = new ImportDialog();
                    if (imp.file != null) {
                        try {
                            m_xEditor.setContent(new FileInputStream(imp.file));
                        } catch (Exception e) {
                            String msg = e.getMessage();
                            if (msg.indexOf("Error parsing as XML") != -1) {
                                msg =
                                        "Imported text does not contain valid XML.\n"
                                                + "Inline XML Metadata datastreams must contain valid XML.";
                            }
                            Administrator.showErrorDialog(Administrator
                                    .getDesktop(), "Import Error", msg, e);
                        }
                    }
                }
            });
            xBottomPane.add(xImportButton);
            JPanel xPane = new JPanel();
            xPane.setLayout(new BorderLayout());
            //            xPane.add(xTopPane, BorderLayout.NORTH);
            xPane.add(m_xEditor.getComponent(), BorderLayout.CENTER);
            xPane.add(xBottomPane, BorderLayout.SOUTH);

            // Managed Content Datastream....
            // SOUTH: [Import]
            JPanel mBottomPane = new JPanel(new FlowLayout());
            JButton mImportButton = new JButton("Import...");
            Administrator.constrainHeight(mImportButton);
            mImportButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    ImportDialog imp = new ImportDialog();
                    if (imp.file != null) {
                        try {
                            // see if we should put a viewer up, or just
                            // a label that says they're importing.
                            JComponent newCenter;
                            String curMime =
                                    (String) m_mimeComboBox.getSelectedItem();
                            if (ContentHandlerFactory.hasViewer(curMime)) {
                                ContentViewer viewer =
                                        ContentHandlerFactory
                                                .getViewer(curMime,
                                                           new FileInputStream(imp.file));
                                newCenter = viewer.getComponent();
                            } else {
                                String importString;
                                if (imp.url != null) {
                                    importString = "Will import " + imp.url;
                                } else {
                                    importString =
                                            "Will import " + imp.file.getPath();
                                }
                                newCenter = new JLabel(importString);
                            }
                            // now remove the old center component (if needed),
                            // and add newCenter, then validate
                            if (m_mCenter != null) {
                                m_mPane.remove(m_mCenter);
                            }
                            m_mCenter = newCenter;
                            m_mPane.add(m_mCenter, BorderLayout.CENTER);
                            m_mPane.validate();
                            // lastly, set the file we're importing
                            m_managedFile = imp.file;
                        } catch (Exception e) {
                            Administrator.showErrorDialog(Administrator
                                    .getDesktop(), "Import Error", e
                                    .getMessage(), e);
                        }
                    }
                }
            });
            mBottomPane.add(mImportButton);

            m_mPane = new JPanel(new BorderLayout());
            m_mPane.add(mBottomPane, BorderLayout.SOUTH);

            // External Referenced or Redirect Datastream....
            //
            // NORTH: Location  __________________
            // SOUTH:        [View]
            // preview button's actionlistener will only pull up a viewer
            // if the selected mime type is something we have a viewer for.
            JPanel erTopPane = new JPanel(new BorderLayout());
            erTopPane.add(new JLabel("Location  "), BorderLayout.WEST);
            m_referenceTextField = new JTextField("http://");
            erTopPane.add(m_referenceTextField, BorderLayout.CENTER);

            JPanel erBottomPane = new JPanel(new FlowLayout());
            m_erViewButton = new JButton("View");
            Administrator.constrainHeight(m_erViewButton);
            m_erViewButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    // get a viewer and put it in the middle of m_erPane
                    // we assume we can get a viewer here because
                    // the view button wouldn't be enabled if that weren't
                    // the case
                    try {
                        String mimeType =
                                (String) m_mimeComboBox.getSelectedItem();
                        m_erViewer =
                                ContentHandlerFactory
                                        .getViewer(mimeType,
                                                   Administrator.DOWNLOADER
                                                           .get(m_referenceTextField
                                                                   .getText()));
                        m_erPane.add(m_erViewer.getComponent(),
                                     BorderLayout.CENTER);
                        m_erPane.validate();
                    } catch (Exception e) {
                        Administrator.showErrorDialog(Administrator
                                .getDesktop(), "View error", e.getMessage(), e);
                    }
                }
            });
            erBottomPane.add(m_erViewButton);
            m_erPane = new JPanel(new BorderLayout());
            m_erPane.add(erTopPane, BorderLayout.NORTH);
            m_erPane.add(erBottomPane, BorderLayout.SOUTH);

            m_specificPane = new JPanel();
            m_contentCard = new CardLayout();
            m_specificPane.setLayout(m_contentCard);
            m_specificPane.add(xPane, "X");
            m_specificPane.add(m_mPane, "M");
            m_specificPane.add(m_erPane, "ER");

            JPanel entryPane = new JPanel();
            entryPane.setLayout(new BorderLayout());
            entryPane.setBorder(BorderFactory
                    .createCompoundBorder(BorderFactory.createEtchedBorder(),
                                          BorderFactory.createEmptyBorder(4,
                                                                          4,
                                                                          4,
                                                                          4)));
            entryPane.add(commonPane, BorderLayout.NORTH);
            entryPane.add(m_specificPane, BorderLayout.CENTER);

            JButton saveButton = new JButton("Save Datastream");
            Administrator.constrainHeight(saveButton);
            saveButton.setActionCommand("Save");
            saveButton.addActionListener(this);

            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout());
            buttonPane.add(saveButton);

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(entryPane, BorderLayout.CENTER);
            add(buttonPane, BorderLayout.SOUTH);
        }

        public void actionPerformed(ActionEvent evt) {
            String cmd = evt.getActionCommand();
            if (cmd.equals("X")) {
                m_controlGroupTextArea.setText(X_DESCRIPTION);
                m_contentCard.show(m_specificPane, "X");
                m_controlGroup = "X";
                removeMIMETypeItems();
            } else if (cmd.equals("M")) {
                m_controlGroupTextArea.setText(M_DESCRIPTION);
                m_contentCard.show(m_specificPane, "M");
                m_controlGroup = "M";
                if (m_mimeComboBox.getItemCount() == 1) {
                    addMIMETypeItems();
                }
            } else if (cmd.equals("E")) {
                m_controlGroupTextArea.setText(E_DESCRIPTION);
                m_contentCard.show(m_specificPane, "ER");
                m_controlGroup = "E";
                if (m_mimeComboBox.getItemCount() == 1) {
                    addMIMETypeItems();
                }
            } else if (cmd.equals("R")) {
                m_controlGroupTextArea.setText(R_DESCRIPTION);
                m_contentCard.show(m_specificPane, "ER");
                m_controlGroup = "R";
                if (m_mimeComboBox.getItemCount() == 1) {
                    addMIMETypeItems();
                }
            } else if (cmd.equals("Save")) {
                try {
                    // try to save... first set common values for call
                    String pid = m_pid;
                    String dsID = m_idTextField.getText().trim();
                    if (dsID.equals("")) {
                        dsID = null;
                    }
                    String trimmed = m_altIDsTextField.getText().trim();
                    String[] altIDs;
                    if (trimmed.length() == 0) {
                        altIDs = new String[0];
                    } else if (trimmed.indexOf(" ") == -1) {
                        altIDs = new String[] {trimmed};
                    } else {
                        altIDs = trimmed.split("\\s");
                    }
                    String formatURI = m_formatURITextField.getText().trim();
                    if (formatURI.length() == 0) {
                        formatURI = null;
                    }
                    String label = m_labelTextField.getText();
                    String mimeType = (String) m_mimeComboBox.getSelectedItem();
                    String location = null;
                    if (m_controlGroup.equals("X")) {
                        // m_xEditor
                        location =
                                Administrator.UPLOADER.upload(m_xEditor
                                        .getContent());
                    } else if (m_controlGroup.equals("M")) {
                        // get the imported file
                        if (m_managedFile == null) {
                            throw new IOException("Content must be specified first.");
                        }
                        location = Administrator.UPLOADER.upload(m_managedFile);
                    } else { // must be E/R
                        location = m_referenceTextField.getText();
                    }
                    String csType =
                            m_checksumTypeComboBox.getSelectedItem().toString();
                    String checksum = null;
                    if (csType.equals("Default")) {
                        csType = null;
                    } else if (csType.equals("DISABLED")) {
                        checksum = null;
                    } else if (m_checksumValue.getText().length() == 0) {
                        checksum = null;
                    } else if (m_checksumValue.getText()
                            .equalsIgnoreCase("none")) {
                        checksum = null;
                    } else {
                        checksum = m_checksumValue.getText();
                    }
                    boolean versionable =
                            m_versionableComboBox.getSelectedIndex() == NEW_VERSION_ON_UPDATE ? true
                                    : false;
                    String newID =
                            Administrator.APIM
                                    .addDatastream(pid,
                                                   dsID,
                                                   altIDs,
                                                   label,
                                                   versionable, // DEFAULT_VERSIONABLE
                                                   mimeType,
                                                   formatURI,
                                                   location,
                                                   m_controlGroup,
                                                   m_initialState,
                                                   csType,
                                                   checksum, // checksum type and checksum
                                                   "DatastreamsPane generated this logMessage."); // DEFAULT_LOGMESSAGE
                    addDatastreamTab(newID, true);
                } catch (Exception e) {
                    e.printStackTrace();
                    String msg = e.getMessage();
                    if (msg.indexOf("Content is not allowed in prolog") != -1) {
                        msg =
                                "Text entered is not valid XML.\n"
                                        + "Internal XML Metadata datastreams must contain valid XML.";
                    }
                    Administrator
                            .showErrorDialog(Administrator.getDesktop(),
                                             "Error saving new datastream",
                                             msg,
                                             e);
                }
            }
        }

        public void addMIMETypeItems() {
            for (int i = 1; i < ALL_KNOWN_MIMETYPES.length; i++) {
                m_mimeComboBox.addItem(ALL_KNOWN_MIMETYPES[i]);
            }
            m_mimeComboBox.setPreferredSize(new Dimension(150, 20));
        }

        public void removeMIMETypeItems() {
            for (int i = 1; i < ALL_KNOWN_MIMETYPES.length; i++) {
                m_mimeComboBox.removeItem(ALL_KNOWN_MIMETYPES[i]);
            }
            m_mimeComboBox.setPreferredSize(new Dimension(150, 20));
        }
    }

}
