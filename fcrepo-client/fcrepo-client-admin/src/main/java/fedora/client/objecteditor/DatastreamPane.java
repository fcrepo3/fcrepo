/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.objecteditor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import fedora.client.Administrator;

import fedora.server.types.gen.Datastream;
import fedora.server.utilities.StreamUtility;

/**
 * Displays a datastream's attributes, allowing the editing of its state, and
 * some of the most recent version's attributes. Also provides buttons for
 * working with the content of the datastream, depending on its type.
 *
 * @author Chris Wilper
 * @version $Id$
 */
public class DatastreamPane
        extends EditingPane
        implements ChangeListener {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DatastreamPane.class.getName());

    private static final long serialVersionUID = 1L;

    protected String m_pid;

    private final Datastream m_mostRecent;

    protected JComboBox m_stateComboBox;

    static protected String s_stateComboBoxValues[] = {"A", "I", "D"};

    protected JComboBox m_versionableComboBox;

    private JSlider m_versionSlider;

    private final JPanel m_valuePane;

    private final CardLayout m_versionCardLayout;

    protected CurrentVersionPane m_currentVersionPane;

    protected DatastreamsPane m_owner;

    private final PurgeButtonListener m_purgeButtonListener;

    private boolean m_done;

    private final Dimension m_labelDims;

    private JTextArea m_dtLabel;

    private JPanel m_dateLabelAndValue;

    private final Datastream[] m_versions;

    protected final static int NEW_VERSION_ON_UPDATE = 0;

    protected final static int REPLACE_ON_UPDATE = 1;

    /**
     * Build the pane.
     */
    public DatastreamPane(ObjectEditorFrame gramps,
                          String pid,
                          Datastream[] versions,
                          DatastreamsPane owner)
            throws Exception {
        super(gramps, owner, versions[0].getID());
        m_pid = pid;
        m_versions = versions;
        Datastream mostRecent = versions[0];
        m_mostRecent = mostRecent;
        m_owner = owner;
        m_labelDims = new JLabel("Control Group").getPreferredSize();
        new TextContentEditor(); // causes it to be registered if not already
        new ImageContentViewer(); // causes it to be registered if not already
        new SVGContentViewer(); // causes it to be registered if not already
        new RDFTupleEditor(); // causes it to be registered if not already
        // mainPane(commonPane, versionPane)

        // NORTH: commonPane(state, controlGroup)

        // LEFT: labels
        JLabel idLabel = new JLabel("ID");
        JLabel stateLabel = new JLabel("State");
        JLabel versionableLabel = new JLabel("Versionable");
        JLabel controlGroupLabel = new JLabel("Control Group");
        JLabel[] leftCommonLabels =
                new JLabel[] {idLabel, controlGroupLabel, stateLabel,
                        versionableLabel};

        // RIGHT: values

        String[] comboBoxStrings = {"Active", "Inactive", "Deleted"};
        m_stateComboBox = new JComboBox(comboBoxStrings);
        Administrator.constrainHeight(m_stateComboBox);
        if (mostRecent.getState().equals("A")) {
            m_stateComboBox.setSelectedIndex(0);
            m_stateComboBox.setBackground(Administrator.ACTIVE_COLOR);
        } else if (mostRecent.getState().equals("I")) {
            m_stateComboBox.setSelectedIndex(1);
            m_stateComboBox.setBackground(Administrator.INACTIVE_COLOR);
        } else {
            m_stateComboBox.setSelectedIndex(2);
            m_stateComboBox.setBackground(Administrator.DELETED_COLOR);
        }
        m_stateComboBox.addActionListener(dataChangeListener);
        m_stateComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                String curState;
                if (m_stateComboBox.getSelectedIndex() == 1) {
                    curState = "I";
                    m_stateComboBox.setBackground(Administrator.INACTIVE_COLOR);
                } else if (m_stateComboBox.getSelectedIndex() == 2) {
                    curState = "D";
                    m_stateComboBox.setBackground(Administrator.DELETED_COLOR);
                } else {
                    curState = "A";
                    m_stateComboBox.setBackground(Administrator.ACTIVE_COLOR);
                }
                m_owner.colorTabForState(m_mostRecent.getID(), curState);
            }
        });

        String[] comboBoxStrings2 =
                {"Updates will create new version",
                        "Updates will replace most recent version"};
        m_versionableComboBox = new JComboBox(comboBoxStrings2);
        Administrator.constrainHeight(m_versionableComboBox);
        m_versionableComboBox
                .setSelectedIndex(mostRecent.isVersionable() ? NEW_VERSION_ON_UPDATE
                        : REPLACE_ON_UPDATE);
        m_versionableComboBox.addActionListener(dataChangeListener);

        JTextArea controlGroupValueLabel =
                new JTextArea(getControlGroupString(mostRecent
                        .getControlGroup().toString()));
        controlGroupValueLabel.setBackground(Administrator.BACKGROUND_COLOR);
        controlGroupValueLabel.setEditable(false);
        JComponent[] leftCommonValues =
                new JComponent[] {new JLabel(mostRecent.getID()),
                        controlGroupValueLabel, m_stateComboBox,
                        m_versionableComboBox};

        JPanel leftCommonPane = new JPanel();
        GridBagLayout leftCommonGridBag = new GridBagLayout();
        leftCommonPane.setLayout(leftCommonGridBag);
        addLabelValueRows(leftCommonLabels,
                          leftCommonValues,
                          leftCommonGridBag,
                          leftCommonPane);

        JPanel commonPane = leftCommonPane;

        // CENTER: versionPane(m_versionSlider, m_valuePane)

        // NORTH: m_versionSlider

        // set up the shared button listener for purge
        m_purgeButtonListener = new PurgeButtonListener(versions);

        // do the slider if needed
        if (versions.length > 1) {
            m_versionSlider =
                    new JSlider(JSlider.HORIZONTAL, 0, versions.length - 1, 0);
            m_versionSlider.addChangeListener(this);
            m_versionSlider.setMajorTickSpacing(1);
            m_versionSlider.setSnapToTicks(true);
            m_versionSlider.setPaintTicks(true);
        }

        // CENTER: m_valuePane(one card for each version)

        m_valuePane = new JPanel();
        m_versionCardLayout = new CardLayout();
        m_valuePane.setLayout(m_versionCardLayout);
        JPanel[] valuePanes = new JPanel[versions.length];

        // CARD: valuePanes[0](versionValuePane, versionActionPane)

        m_currentVersionPane = new CurrentVersionPane(mostRecent);
        valuePanes[0] = m_currentVersionPane;

        m_valuePane.add(valuePanes[0], "0");

        // CARD: valuePanes[1 to i](versionValuePane, versionActionPane)

        for (int i = 1; i < versions.length; i++) {
            valuePanes[i] = new PriorVersionPane(versions[i]);

            m_valuePane.add(valuePanes[i], "" + i);
        }

        JPanel versionPane = new JPanel();
        versionPane.setLayout(new BorderLayout());
        if (versions.length > 1) {
            // Add a panel to versionPane.NORTH
            // FlowLayout(SwingConstants.LEFT)
            // Created   Date   m_versionSlider
            m_dateLabelAndValue =
                    new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            JLabel createdLabel = new JLabel("Created");
            createdLabel.setPreferredSize(m_labelDims);
            m_dateLabelAndValue.add(createdLabel);
            m_dateLabelAndValue.add(Box.createHorizontalStrut(0));
            m_dtLabel = new JTextArea(versions[0].getCreateDate() + " ");
            m_dtLabel.setBackground(Administrator.BACKGROUND_COLOR);
            m_dtLabel.setEditable(false);
            m_dateLabelAndValue.add(m_dtLabel);

            JPanel stretch = new JPanel(new BorderLayout());
            stretch.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            stretch.add(m_dateLabelAndValue, BorderLayout.WEST);
            stretch.add(m_versionSlider, BorderLayout.CENTER);
            versionPane.add(stretch, BorderLayout.NORTH);
        }
        versionPane.add(m_valuePane, BorderLayout.CENTER);

        mainPane.setLayout(new BorderLayout());
        mainPane.add(commonPane, BorderLayout.NORTH);
        mainPane.add(versionPane, BorderLayout.CENTER);
    }

    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        if (!source.getValueIsAdjusting()) {
            m_versionCardLayout.show(m_valuePane, "" + source.getValue());
            m_dtLabel.setText(m_versions[source.getValue()].getCreateDate());
        }
    }

    public boolean isDirty() {
        if (m_done) {
            return false;
        }
        int stateIndex = 0;
        if (m_mostRecent.getState().equals("I")) {
            stateIndex = 1;
        }
        if (m_mostRecent.getState().equals("D")) {
            stateIndex = 2;
        }
        if (stateIndex != m_stateComboBox.getSelectedIndex()) {
            return true;
        }
        int versionableIndex =
                m_mostRecent.isVersionable() ? NEW_VERSION_ON_UPDATE
                        : REPLACE_ON_UPDATE;
        if (versionableIndex != m_versionableComboBox.getSelectedIndex()) {
            return true;
        }
        if (m_currentVersionPane.isDirty()) {
            return true;
        }
        return false;
    }

    private String getControlGroupString(String abbrev) {
        if (abbrev.equals("M")) {
            return "Managed Content";
        } else if (abbrev.equals("X")) {
            return "Internal XML Metadata";
        } else if (abbrev.equals("R")) {
            return "Redirect";
        } else {
            return "External Reference";
        }
    }

    public static String getFormattedChecksumTypeAndChecksum(Datastream m_ds) {
        if (m_ds.getChecksumType() == null || m_ds.getChecksumType().equals("")
                || m_ds.getChecksumType().equals("none")) {
            return "";
        }
        if (m_ds.getChecksumType().equals("DISABLED")) {
            return "DISABLED";
        }
        return m_ds.getChecksumType() + ": " + m_ds.getChecksum();
    }

    @Override
    public void saveChanges(String logMessage) throws Exception {
        String state = null;
        int i = m_stateComboBox.getSelectedIndex();
        state = s_stateComboBoxValues[i]; // "A" "I"  or "D"
        if (!state.equals(m_mostRecent.getState())) {
            Administrator.APIM.setDatastreamState(m_pid,
                                                  m_mostRecent.getID(),
                                                  state,
                                                  logMessage);
        }
        if ((m_mostRecent.isVersionable() ? NEW_VERSION_ON_UPDATE
                : REPLACE_ON_UPDATE) != m_versionableComboBox
                .getSelectedIndex()) {
            boolean newVersionableSetting =
                    m_versionableComboBox.getSelectedIndex() == NEW_VERSION_ON_UPDATE ? true
                            : false;
            Administrator.APIM.setDatastreamVersionable(m_pid, m_mostRecent
                    .getID(), newVersionableSetting, logMessage);
        }
        if (m_currentVersionPane.isDirty()) {
            // defer to the currentVersionPane if anything else changed
            try {
                m_currentVersionPane.saveChanges(logMessage, false);
            } catch (Exception e) {
                if (e.getMessage() == null
                        || e.getMessage().indexOf(" would invalidate ") == -1) {
                    throw e;
                }
                // ask if they want to force it.
                Object[] options = {"Yes", "No"};
                int selected =
                        JOptionPane
                                .showOptionDialog(null,
                                                  e.getMessage()
                                                          + "\n\nForce it?",
                                                  "Warning",
                                                  JOptionPane.DEFAULT_OPTION,
                                                  JOptionPane.WARNING_MESSAGE,
                                                  null,
                                                  options,
                                                  options[1]);
                if (selected == 0) {
                    m_currentVersionPane.saveChanges(logMessage, true);
                }
            }
        }
    }

    @Override
    public void changesSaved() {
        m_owner.refresh(m_mostRecent.getID());
        m_done = true;
    }

    @Override
    public void undoChanges() {
        if (m_mostRecent.getState().equals("A")) {
            m_stateComboBox.setSelectedIndex(0);
            m_stateComboBox.setBackground(Administrator.ACTIVE_COLOR);
        } else if (m_mostRecent.getState().equals("I")) {
            m_stateComboBox.setSelectedIndex(1);
            m_stateComboBox.setBackground(Administrator.INACTIVE_COLOR);
        } else if (m_mostRecent.getState().equals("D")) {
            m_stateComboBox.setSelectedIndex(2);
            m_stateComboBox.setBackground(Administrator.DELETED_COLOR);
        }
        if ((m_mostRecent.isVersionable() ? NEW_VERSION_ON_UPDATE
                : REPLACE_ON_UPDATE) != m_versionableComboBox
                .getSelectedIndex()) {
            m_versionableComboBox
                    .setSelectedIndex(m_mostRecent.isVersionable() ? NEW_VERSION_ON_UPDATE
                            : REPLACE_ON_UPDATE);
        }
        m_owner.colorTabForState(m_mostRecent.getID(), m_mostRecent.getState());
        m_currentVersionPane.undoChanges();
    }

    protected String getFedoraURL(Datastream ds, boolean withDate) {
        StringBuffer buf = new StringBuffer();
        buf.append(Administrator.getProtocol() + "://");
        buf.append(Administrator.getHost());
        if (Administrator.getPort() != 80) {
            buf.append(':');
            buf.append(Administrator.getPort());
        }
        buf.append("/" + Administrator.getAppServContext() + "/get/");
        buf.append(m_pid);
        buf.append('/');
        buf.append(ds.getID());
        if (withDate) {
            buf.append('/');
            buf.append(ds.getCreateDate());
        }
        return buf.toString();
    }

    public class CurrentVersionPane
            extends JPanel
            implements PotentiallyDirty {

        private static final long serialVersionUID = 1L;

        protected Datastream m_ds;

        private JTextField m_locationTextField;

        protected JTextField m_labelTextField;

        private String m_origLabel;

        protected JTextField m_MIMETextField;

        private String m_origMIME;

        protected JTextField m_formatURITextField;

        private String m_origFormatURI;

        protected JTextField m_altIDsTextField;

        private final String m_origAltIDs;

        protected JButton m_editButton;

        private JButton m_viewButton;

        protected JButton m_editCustomButton;

        private JButton m_viewCustomButton;

        private final JButton m_importButton;

        protected JButton m_exportButton;

        private JButton m_separateViewButton;

        protected JComboBox m_checksumTypeComboBox;

        protected JTextField m_checksumTextField;

        private final JPanel m_checksumPanel;

        protected ContentEditor m_editor;

        private ContentViewer m_viewer;

        private boolean m_canEdit;

        private boolean m_hasCustomEditor;

        private final boolean m_canView;

        private File m_importFile;

        private JLabel m_importLabel;

        protected JPanel m_actionPane;

        protected JButton m_purgeButton;

        private boolean X;

        private boolean M;

        private boolean E;

        private boolean R;

        public CurrentVersionPane(Datastream ds) {
            m_ds = ds;
            // clean up attribute values for presentation in text boxes...
            // set a null ds label to ""
            m_origLabel = m_ds.getLabel();
            if (m_origLabel == null) {
                m_origLabel = "";
            }
            // set a null mime type to ""
            m_origMIME = m_ds.getMIMEType();
            if (m_origMIME == null) {
                m_origMIME = "";
            }
            // set a null format_uri to ""
            m_origFormatURI = m_ds.getFormatURI();
            if (m_origFormatURI == null) {
                m_origFormatURI = "";
            }
            // create a string from alt ids array
            m_origAltIDs = getAltIdsString();

            if (ds.getControlGroup().toString().equals("X")) {
                X = true;
            } else if (ds.getControlGroup().toString().equals("M")) {
                M = true;
            } else if (ds.getControlGroup().toString().equals("E")) {
                E = true;
            } else if (ds.getControlGroup().toString().equals("R")) {
                R = true;
            }
            // editing is possible if it's XML or Managed content and
            // not a special datastream and hasEditor(mimeType)
            // AND the initial state wasn't "D"
            boolean noEdits = ds.getState().equals("D");
            //            boolean specialDatastream = ds.getID().equals("METHODMAP") ||
            //                                        ds.getID().equals("DSINPUTSPEC") ||
            //                                        ds.getID().equals("WSDL");
            String dsMimetype = getCustomMimeType(m_ds);
            if ((X || M) && !noEdits) {
                m_canEdit = ContentHandlerFactory.hasEditor(dsMimetype);
            }
            if (!dsMimetype.equals(m_ds.getMIMEType())) {
                m_hasCustomEditor = true;
            }
            m_canView = ContentHandlerFactory.hasViewer(dsMimetype);
            // whether they're used or not, create these here
            if (m_hasCustomEditor) {
                m_editCustomButton = new JButton("Edit");
                Administrator.constrainHeight(m_editCustomButton);
                m_viewCustomButton = new JButton("View");
                Administrator.constrainHeight(m_viewCustomButton);
                m_editButton = new JButton("Edit as Text");
                Administrator.constrainHeight(m_editButton);
                m_viewButton = new JButton("View as Text");
                Administrator.constrainHeight(m_viewButton);
            } else {
                m_editButton = new JButton("Edit");
                Administrator.constrainHeight(m_editButton);
                m_viewButton = new JButton("View");
                Administrator.constrainHeight(m_viewButton);
                m_editCustomButton = new JButton("Unused Edit");
                Administrator.constrainHeight(m_editCustomButton);
                m_viewCustomButton = new JButton("Unused View");
                Administrator.constrainHeight(m_viewCustomButton);

            }
            m_importButton = new JButton("Import...");
            Administrator.constrainHeight(m_importButton);
            m_exportButton = new JButton("Export...");
            Administrator.constrainHeight(m_exportButton);
            // How we set this JPanel up depends on:
            // what control group it is in and
            // whether it can be edited or viewed
            setLayout(new BorderLayout());

            // do the field panel (NORTH)
            JLabel labelLabel = new JLabel("Label");
            labelLabel.setPreferredSize(m_labelDims);
            JLabel MIMELabel = new JLabel("MIME Type");
            MIMELabel.setPreferredSize(m_labelDims);
            JLabel formatURILabel = new JLabel("Format URI");
            formatURILabel.setPreferredSize(m_labelDims);
            JLabel altIDsLabel = new JLabel("Alternate IDs");
            altIDsLabel.setPreferredSize(m_labelDims);
            JLabel urlLabel = new JLabel("Fedora URL");
            urlLabel.setPreferredSize(m_labelDims);
            JLabel checksumLabel = new JLabel("Checksum");
            checksumLabel.setPreferredSize(m_labelDims);
            JLabel[] labels;
            if (R || E) {
                JLabel locationLabel = new JLabel("Location");
                locationLabel.setPreferredSize(m_labelDims);
                if (m_versionSlider != null) {
                    labels =
                            new JLabel[] {labelLabel, MIMELabel,
                                    formatURILabel, altIDsLabel, locationLabel,
                                    urlLabel, checksumLabel};
                } else {
                    labels =
                            new JLabel[] {new JLabel("Created"), labelLabel,
                                    MIMELabel, formatURILabel, altIDsLabel,
                                    locationLabel, urlLabel, checksumLabel};
                }
            } else {
                if (m_versionSlider != null) {
                    labels =
                            new JLabel[] {labelLabel, MIMELabel,
                                    formatURILabel, altIDsLabel, urlLabel,
                                    checksumLabel};
                } else if (m_ds.getCreateDate() == null) {
                    labels =
                            new JLabel[] {labelLabel, MIMELabel,
                                    formatURILabel, altIDsLabel, checksumLabel};
                } else {
                    labels =
                            new JLabel[] {new JLabel("Created"), labelLabel,
                                    MIMELabel, formatURILabel, altIDsLabel,
                                    urlLabel, checksumLabel};
                }
            }
            // set up text fields for ds attributes at version level
            JComponent[] values;
            // ds label text field
            m_labelTextField = new JTextField(m_origLabel);
            m_labelTextField.getDocument()
                    .addDocumentListener(dataChangeListener);
            // ds MIME text field
            m_MIMETextField = new JTextField(m_origMIME);
            m_MIMETextField.getDocument()
                    .addDocumentListener(dataChangeListener);
            // ds format URI text field
            m_formatURITextField = new JTextField(m_origFormatURI);
            m_formatURITextField.getDocument()
                    .addDocumentListener(dataChangeListener);
            // ds alternate ids text field
            m_altIDsTextField = new JTextField(m_origAltIDs);
            m_altIDsTextField.getDocument()
                    .addDocumentListener(dataChangeListener);
            // disable text fields for special datastreams that cannot be edited
            if (noEdits) {
                // disable formatURI changes for special datastreams
                m_labelTextField.setEnabled(false);
                m_MIMETextField.setEnabled(false);
                m_formatURITextField.setEnabled(false);
                m_altIDsTextField.setEnabled(false);
            }
            // Fedora URL text field
            JTextField urlTextField = new JTextField(getFedoraURL(m_ds, false));
            urlTextField.setEditable(false); // so they can copy, but not modify
            // Datastream checksum field
            m_checksumTypeComboBox =
                    new JComboBox(new String[] {"DISABLED", "MD5", "SHA-1",
                            "SHA-256", "SHA-384", "SHA-512"});
            setSelectedChecksumType(m_checksumTypeComboBox, ds
                    .getChecksumType());
            m_checksumTextField = new JTextField(m_ds.getChecksum());
            m_checksumTextField.setEditable(false);
            m_checksumPanel = new JPanel();
            m_checksumPanel.setLayout(new BorderLayout());
            m_checksumPanel.add(m_checksumTypeComboBox, BorderLayout.WEST);
            m_checksumPanel.add(m_checksumTextField, BorderLayout.CENTER);
            m_checksumTypeComboBox.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    String csType =
                            m_checksumTypeComboBox.getSelectedItem().toString();
                    dataChangeListener.dataChanged();
                    if (csType.equals("DISABLED")
                            || !csType.equals(m_ds.getChecksumType())) {
                        if (m_checksumTextField != null) {
                            m_checksumPanel.remove(m_checksumTextField);
                            m_checksumTextField = null;
                            m_checksumPanel.validate();
                            m_checksumPanel.repaint();
                        }
                    } else {
                        if (m_checksumTextField != null) {
                            m_checksumPanel.remove(m_checksumTextField);
                        }
                        m_checksumTextField =
                                new JTextField(m_ds.getChecksum());
                        m_checksumTextField.setEditable(false);
                        m_checksumPanel.add(m_checksumTextField,
                                            BorderLayout.CENTER);
                        m_checksumPanel.validate();
                        m_checksumPanel.repaint();
                    }
                }
            });

            //            JTextField checksumTextField=new JTextField(getFormattedChecksumTypeAndChecksum(m_ds));
            //            checksumTextField.setEditable(false);  // so they can copy, but not modify
            // ds location URL text field (R and E datastreams only)
            if (R || E) {
                m_locationTextField = new JTextField(m_ds.getLocation());
                m_locationTextField.getDocument()
                        .addDocumentListener(dataChangeListener);
                if (noEdits) {
                    m_locationTextField.setEnabled(false);
                }
                if (m_versionSlider != null) {
                    values =
                            new JComponent[] {m_labelTextField,
                                    m_MIMETextField, m_formatURITextField,
                                    m_altIDsTextField, m_locationTextField,
                                    urlTextField, m_checksumPanel};

                } else {
                    JTextArea cDateTextArea =
                            new JTextArea(m_ds.getCreateDate());
                    cDateTextArea.setBackground(Administrator.BACKGROUND_COLOR);
                    cDateTextArea.setEditable(false);
                    values =
                            new JComponent[] {cDateTextArea, m_labelTextField,
                                    m_MIMETextField, m_formatURITextField,
                                    m_altIDsTextField, m_locationTextField,
                                    urlTextField, m_checksumPanel};
                }
            } else {
                if (m_versionSlider != null) {
                    values =
                            new JComponent[] {m_labelTextField,
                                    m_MIMETextField, m_formatURITextField,
                                    m_altIDsTextField, urlTextField,
                                    m_checksumPanel};
                } else if (m_ds.getCreateDate() == null) {
                    values =
                            new JComponent[] {m_labelTextField,
                                    m_MIMETextField, m_formatURITextField,
                                    m_altIDsTextField, m_checksumPanel};
                } else {
                    JTextArea cDateTextArea =
                            new JTextArea(m_ds.getCreateDate());
                    cDateTextArea.setBackground(Administrator.BACKGROUND_COLOR);
                    cDateTextArea.setEditable(false);
                    values =
                            new JComponent[] {cDateTextArea, m_labelTextField,
                                    m_MIMETextField, m_formatURITextField,
                                    m_altIDsTextField, urlTextField,
                                    m_checksumPanel};
                }
            }

            JPanel fieldPane = new JPanel();
            GridBagLayout grid = new GridBagLayout();
            fieldPane.setLayout(grid);
            addLabelValueRows(labels, values, grid, fieldPane);
            add(fieldPane, BorderLayout.NORTH);

            // Do the buttons!
            m_actionPane = new JPanel();
            m_actionPane.setLayout(new FlowLayout());
            if (m_canEdit) {
                if (m_hasCustomEditor) {
                    m_editCustomButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent evt) {
                            // add the editor, and disable the button
                            try {
                                startCustomEditor();
                            } catch (Exception e) {
                                Administrator.showErrorDialog(Administrator
                                        .getDesktop(), "Content Edit Error", e
                                        .getMessage(), e);
                            }
                        }
                    });
                    m_actionPane.add(m_editCustomButton);

                }
                // we know it's editable... add a button
                m_editButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent evt) {
                        // add the editor, and disable the button
                        try {
                            startEditor();
                        } catch (Exception e) {
                            Administrator.showErrorDialog(Administrator
                                    .getDesktop(), "Content Edit Error", e
                                    .getMessage(), e);
                        }
                    }
                });
                m_actionPane.add(m_editButton);
                // if a *separate* viewer is also available, add a view button
                if (!ContentHandlerFactory.viewerIsEditor(dsMimetype)) {
                    m_separateViewButton = new JButton("View");
                    Administrator.constrainHeight(m_separateViewButton);
                    m_separateViewButton
                            .addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent evt) {
                                    // open a separate viewing window, using the content
                                    // from the *server* if the text is "View", and the
                                    // content from the editor if the text is "Preview"
                                    try {
                                        startSeparateViewer();
                                    } catch (Exception e) {
                                        Administrator
                                                .showErrorDialog(Administrator
                                                                         .getDesktop(),
                                                                 "Content View Error",
                                                                 e.getMessage(),
                                                                 e);
                                    }
                                }
                            });
                    m_actionPane.add(m_separateViewButton);
                }
            } else if (m_canView) {
                if (m_hasCustomEditor) {
                    m_viewCustomButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent evt) {
                            // add the editor, and disable the button
                            try {
                                startCustomViewer();
                            } catch (Exception e) {
                                Administrator.showErrorDialog(Administrator
                                        .getDesktop(), "Content Edit Error", e
                                        .getMessage(), e);
                            }
                        }
                    });
                    m_actionPane.add(m_viewCustomButton);

                }
                // it's not editable, but it's VIEWable... add a button
                m_viewButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent evt) {
                        // add the viewer, and disable the view button
                        try {
                            startViewer();
                        } catch (Exception e) {
                            Administrator.showErrorDialog(Administrator
                                    .getDesktop(), "Content View Error", e
                                    .getMessage(), e);
                        }
                    }
                });
                m_actionPane.add(m_viewButton);
            }
            // should we add the Import button?  If we can set content, yes.
            if ((X || M) && !noEdits) {
                m_actionPane.add(m_importButton);
                m_importButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent evt) {
                        // remember what we did so we can back out if needed
                        boolean startedEditor = false;
                        boolean startedViewer = false;
                        try {
                            // prompt for the file or URL:
                            ImportDialog imp = new ImportDialog();

                            if (imp.file != null) {
                                File file = imp.file;
                                String url = imp.url;
                                Administrator.setLastDir(file.getParentFile()); // remember the dir for next time
                                if (m_canEdit) {
                                    if (m_editor == null) {
                                        if (m_hasCustomEditor) {
                                            startCustomEditor();
                                        } else {
                                            startEditor();
                                        }
                                        startedEditor = true;
                                    }
                                    // set content of existing edit widget
                                    m_editor
                                            .setContent(new FileInputStream(file));
                                    // if that went ok, then remember the file
                                    m_importFile = file;
                                    // and send the signal
                                    dataChangeListener.dataChanged();
                                } else if (m_canView) {
                                    if (m_viewer == null) {
                                        if (m_hasCustomEditor) {
                                            startCustomViewer();
                                        } else {
                                            startViewer();
                                        }
                                        startedViewer = true;
                                    }
                                    // set the content of the existing viewer widget
                                    m_viewer
                                            .setContent(new FileInputStream(file));
                                    // if that went ok, then remember the file
                                    m_importFile = file;
                                    // and send the signal
                                    dataChangeListener.dataChanged();
                                } else {
                                    // can't view or edit, so put a label
                                    if (url != null) {
                                        m_importLabel =
                                                new JLabel("Will import " + url);
                                    } else {
                                        m_importLabel =
                                                new JLabel("Will import "
                                                        + file.getPath());
                                    }
                                    add(m_importLabel, BorderLayout.CENTER);
                                    validate();
                                    // if that went ok, then remember the file
                                    m_importFile = file;
                                    // and send the signal
                                    dataChangeListener.dataChanged();
                                }
                            }
                        } catch (Exception e) {
                            if (startedEditor) {
                                // restore the original ui state
                                m_editButton.setEnabled(true);
                                remove(m_editor.getComponent());
                                m_editor = null;
                            }
                            if (startedViewer) {
                                // restore the original ui state
                                m_viewButton.setEnabled(true);
                                remove(m_viewer.getComponent());
                                m_viewer = null;
                            }
                            Administrator.showErrorDialog(Administrator
                                    .getDesktop(), "Content Import Failure", e
                                    .getMessage(), e);
                        }
                    }
                });
            }
            // export is always possible!
            m_actionPane.add(m_exportButton);
            m_exportButton.addActionListener(new ExportActionListener(m_ds));
            // and purge is, too
            m_purgeButton = new JButton("Purge...");
            Administrator.constrainHeight(m_purgeButton);
            m_purgeButton.addActionListener(m_purgeButtonListener);
            m_purgeButton.setActionCommand(m_ds.getCreateDate());
            m_actionPane.add(m_purgeButton);
            add(m_actionPane, BorderLayout.SOUTH);
        }

        private String getAltIdsString() {
            String altIDStr = "";
            String[] altIDs = m_ds.getAltIDs();
            if (altIDs != null) {
                for (int z = 0; z < altIDs.length; z++) {
                    if (z > 0) {
                        altIDStr += " ";
                    }
                    altIDStr += altIDs[z];
                }
            }
            return altIDStr;
        }

        private void setSelectedChecksumType(JComboBox typeComboBox,
                                             String checksumType) {
            for (int i = 0; i < typeComboBox.getItemCount(); i++) {
                if (typeComboBox.getItemAt(i).toString().equals(checksumType)) {
                    typeComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        public Datastream getDatastream() {
            return m_ds;
        }

        /**
         * Bring up the editing pane, initialized with this datastream's
         * content.
         */
        private void startEditor() throws Exception {
            InputStream curContent = null;
            if (m_editor != null) {
                if (m_editor.isDirty()) {
                    curContent = m_editor.getContent();
                }
                remove(m_editor.getComponent());
                m_editor = null;
            }
            InputStream origContent =
                    getDatastreamContent(m_pid, m_ds.getID(), m_ds
                            .getCreateDate());
            m_editor =
                    ContentHandlerFactory.getEditor(m_ds.getMIMEType(),
                                                    origContent);
            m_editor.setContentChangeListener(dataChangeListener);
            if (curContent != null) {
                m_editor.setContent(curContent);
            }
            m_editor.setPIDAndDSID(m_pid, m_ds.getID());

            add(m_editor.getComponent(), BorderLayout.CENTER);
            m_editButton.setEnabled(false);
            m_editCustomButton.setEnabled(true);
            validate();
        }

        public void startViewer() throws Exception {
            if (m_viewer != null) {
                remove(m_viewer.getComponent());
                m_viewer = null;
            }
            m_viewer =
                    ContentHandlerFactory
                            .getViewer(m_ds.getMIMEType(),
                                       getDatastreamContent(m_pid,
                                                            m_ds.getID(),
                                                            m_ds
                                                                    .getCreateDate()));
            add(m_viewer.getComponent(), BorderLayout.CENTER);
            m_viewButton.setEnabled(false);
            m_viewCustomButton.setEnabled(true);
            validate();
        }

        /**
         * Bring up the editing pane, initialized with this datastream's
         * content.
         */
        private void startCustomEditor() throws Exception {
            InputStream curContent = null;
            if (m_editor != null) {
                if (m_editor.isDirty()) {
                    curContent = m_editor.getContent();
                }
                remove(m_editor.getComponent());
                m_editor = null;
            }
            InputStream origContent =
                    getDatastreamContent(m_pid, m_ds.getID(), m_ds
                            .getCreateDate());
            m_editor =
                    ContentHandlerFactory.getEditor(getCustomMimeType(m_ds),
                                                    origContent);
            m_editor.setContentChangeListener(dataChangeListener);
            if (curContent != null) {
                m_editor.setContent(curContent);
            }
            m_editor.setPIDAndDSID(m_pid, m_ds.getID());
            add(m_editor.getComponent(), BorderLayout.CENTER);
            m_editCustomButton.setEnabled(false);
            m_editButton.setEnabled(true);
            validate();
        }

        public void startCustomViewer() throws Exception {
            if (m_viewer != null) {
                remove(m_viewer.getComponent());
                m_viewer = null;
            }
            m_viewer =
                    ContentHandlerFactory
                            .getViewer(getCustomMimeType(m_ds),
                                       getDatastreamContent(m_pid,
                                                            m_ds.getID(),
                                                            m_ds
                                                                    .getCreateDate()));
            add(m_viewer.getComponent(), BorderLayout.CENTER);
            m_viewCustomButton.setEnabled(false);
            m_viewButton.setEnabled(true);
            validate();
        }

        public void startSeparateViewer() throws Exception {
            InputStream contentStream;
            if (m_separateViewButton.getText().equals("Preview")) {
                // the editor will provide the content
                contentStream = m_editor.getContent();
            } else {
                // the server will provide the content
                contentStream =
                        getDatastreamContent(m_pid, m_ds.getID(), m_ds
                                .getCreateDate());
            }
            ContentViewer separateViewer =
                    ContentHandlerFactory.getViewer(getCustomMimeType(m_ds),
                                                    contentStream);
            // now open up a new JInternalFrame and put the v.getComponent()
            // in it.
            JInternalFrame viewFrame =
                    new JInternalFrame(m_separateViewButton.getText() + "ing "
                                               + m_ds.getID()
                                               + " datastream from object "
                                               + m_pid,
                                       true,
                                       true,
                                       true,
                                       true);
            //viewFrame.setFrameIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/standard/general/Edit16.gif")));
            JPanel myPanel = new JPanel();
            myPanel.setLayout(new BorderLayout());
            myPanel.add(separateViewer.getComponent(), BorderLayout.CENTER);
            viewFrame.getContentPane().add(myPanel);
            viewFrame.setSize(720, 520);
            Administrator.getDesktop().add(viewFrame);
            viewFrame.setVisible(true);
            viewFrame.toFront();
        }

        public void saveChanges(String logMessage, boolean force)
                throws Exception {
            String label = m_labelTextField.getText().trim();
            String mimeType = m_MIMETextField.getText().trim();
            String formatURI = m_formatURITextField.getText().trim();
            String[] altIDs = m_altIDsTextField.getText().trim().split(" ");
            String checksumType =
                    m_checksumTypeComboBox.getSelectedItem().toString();
            if (checksumType.equals(m_ds.getChecksumType())) {
                checksumType = null;
            }
            if (X) {
                byte[] content = new byte[0];
                if (m_editor != null && m_editor.isDirty()) {
                    InputStream in = m_editor.getContent();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    StreamUtility.pipeStream(in, out, 4096);
                    content = out.toByteArray();
                }
                Administrator.APIM.modifyDatastreamByValue(m_pid,
                                                           m_ds.getID(),
                                                           altIDs,
                                                           label,
                                                           mimeType,
                                                           formatURI,
                                                           content,
                                                           checksumType,
                                                           null, // checksum
                                                           logMessage,
                                                           force);
            } else if (M) {
                String loc = null; // if not set, server will not change content
                if (m_importFile != null) {
                    // upload the import file, getting a temporary ref
                    loc = Administrator.UPLOADER.upload(m_importFile);
                } else if (m_editor != null && m_editor.isDirty()) {
                    // They've edited managed content that came up in an editor...
                    // use its content
                    loc = Administrator.UPLOADER.upload(m_editor.getContent());
                }
                Administrator.APIM.modifyDatastreamByReference(m_pid,
                                                               m_ds.getID(),
                                                               altIDs,
                                                               label,
                                                               mimeType,
                                                               formatURI,
                                                               loc,
                                                               checksumType,
                                                               null, // checksum
                                                               logMessage,
                                                               force);
            } else {
                // external ref or redirect
                Administrator.APIM
                        .modifyDatastreamByReference(m_pid,
                                                     m_ds.getID(),
                                                     altIDs,
                                                     label,
                                                     mimeType,
                                                     formatURI,
                                                     m_locationTextField
                                                             .getText(),
                                                     checksumType,
                                                     null, // checksum
                                                     logMessage,
                                                     force);
            }
        }

        public boolean isDirty() {
            if (m_editor != null) {
                if (m_editor.isDirty()) {
                    // ensure the button label for view is right, if it's there
                    if (m_separateViewButton != null) {
                        if (m_separateViewButton.getText().equals("View")) {
                            m_separateViewButton.setText("Preview");
                        }
                    }
                    return true;
                } else {
                    // ensure the button label for view is right, if it's there
                    if (m_separateViewButton != null) {
                        if (m_separateViewButton.getText().equals("Preview")) {
                            m_separateViewButton.setText("View");
                        }
                    }
                }
            }
            if (!m_ds.getLabel().equals(m_labelTextField.getText())) {
                return true;
            }
            if (!m_origMIME.equals(m_MIMETextField.getText())) {
                return true;
            }
            if (!m_origFormatURI.equals(m_formatURITextField.getText())) {
                return true;
            }
            if (!m_origAltIDs.equals(m_altIDsTextField.getText())) {
                return true;
            }
            if (m_locationTextField != null
                    && !m_locationTextField.getText()
                            .equals(m_ds.getLocation())) {
                return true;
            }
            if (!m_checksumTypeComboBox.getSelectedItem().toString()
                    .equals(m_ds.getChecksumType())) {
                return true;
            }
            if (m_importFile != null) {
                return true;
            }
            return false;
        }

        public void undoChanges() {
            m_labelTextField.setText(m_origLabel);
            m_MIMETextField.setText(m_origMIME);
            m_formatURITextField.setText(m_origFormatURI);
            m_altIDsTextField.setText(m_origAltIDs);
            if (m_locationTextField != null) {
                m_locationTextField.setText(m_ds.getLocation());
            }
            if (m_editor != null) {
                m_editor.undoChanges();
            }
            setSelectedChecksumType(m_checksumTypeComboBox, m_ds
                    .getChecksumType());
            if (m_importFile != null) {
                m_importFile = null;
                // and remove the viewer if it's up, and re-enable the view
                // button
                if (m_canView) {
                    // must be viewing, so remove the viewer and re-enable the
                    // view button
                    m_viewButton.setEnabled(true);
                    remove(m_viewer.getComponent());
                    m_viewer = null;
                } else {
                    // remove the JLabel
                    remove(m_importLabel);
                    m_importLabel = null;
                }
            }
        }
    }

    public class PriorVersionPane
            extends JPanel {

        private static final long serialVersionUID = 1L;

        private boolean X;

        private boolean M;

        private boolean E;

        private boolean R;

        private final Datastream m_ds;

        private String m_priorLabel;

        private String m_priorMIME;

        private String m_priorFormatURI;

        private String m_priorAltIDs;

        private JButton m_viewButton;

        private JButton m_viewTextButton;

        public PriorVersionPane(Datastream ds) {
            m_ds = ds;
            // clean up attribute values for presentation in text boxes...
            // set a null ds label to ""
            m_priorLabel = m_ds.getLabel();
            if (m_priorLabel == null) {
                m_priorLabel = "";
            }
            // set a null MIME type to ""
            m_priorMIME = m_ds.getMIMEType();
            if (m_priorMIME == null) {
                m_priorMIME = "";
            }
            // set a null format_uri to ""
            m_priorFormatURI = m_ds.getFormatURI();
            if (m_priorFormatURI == null) {
                m_priorFormatURI = "";
            }
            // create a string from alt ids array
            m_priorAltIDs = "";
            String[] altIDs = m_ds.getAltIDs();
            if (altIDs != null) {
                for (int z = 0; z < altIDs.length; z++) {
                    if (z > 0) {
                        m_priorAltIDs += " ";
                    }
                    m_priorAltIDs += altIDs[z];
                }
            }

            if (ds.getControlGroup().toString().equals("X")) {
                X = true;
            } else if (ds.getControlGroup().toString().equals("M")) {
                M = true;
            } else if (ds.getControlGroup().toString().equals("E")) {
                E = true;
            } else if (ds.getControlGroup().toString().equals("R")) {
                R = true;
            }
            setLayout(new BorderLayout());
            // NORTH: fieldPanel
            // disabled labels and values
            // ds label...
            JLabel labelLabel = new JLabel("Label");
            labelLabel.setMinimumSize(m_labelDims);
            JTextField labelValue = new JTextField();
            labelValue.setText(m_priorLabel);
            labelValue.setEditable(false);
            // ds MIME type...
            JLabel MIMELabel = new JLabel("MIME Type");
            MIMELabel.setMinimumSize(m_labelDims);
            JTextField MIMEValue = new JTextField();
            MIMEValue.setText(m_priorMIME);
            MIMEValue.setEditable(false);
            // ds format URI...
            JLabel formatURILabel = new JLabel("Format URI");
            formatURILabel.setMinimumSize(m_labelDims);
            JTextField formatURIValue = new JTextField();
            formatURIValue.setText(m_priorFormatURI);
            formatURIValue.setEditable(false);
            // ds alternate ids...
            JLabel altIDsLabel = new JLabel("Alternate IDs");
            altIDsLabel.setMinimumSize(m_labelDims);
            JTextField altIDsValue = new JTextField();
            altIDsValue.setText(m_priorAltIDs);
            altIDsValue.setEditable(false);
            // ds Fedora URL...
            JLabel urlLabel = new JLabel("Fedora URL");
            urlLabel.setPreferredSize(m_labelDims);
            JTextField urlTextField = new JTextField(getFedoraURL(m_ds, true));
            urlTextField.setEditable(false); // so they can copy, but not modify
            // Datastream checksum field
            JLabel checksumLabel = new JLabel("Checksum");
            checksumLabel.setPreferredSize(m_labelDims);
            JTextField checksumTextField =
                    new JTextField(getFormattedChecksumTypeAndChecksum(m_ds));
            checksumTextField.setEditable(false); // so they can copy, but not modify

            JLabel[] labels;
            JComponent[] values;
            if (E || R) {
                labels =
                        new JLabel[] {labelLabel, MIMELabel, formatURILabel,
                                altIDsLabel, new JLabel("Location"), urlLabel,
                                checksumLabel};
                JTextField refValue = new JTextField();
                refValue.setText(ds.getLocation());
                refValue.setEditable(false);
                values =
                        new JComponent[] {labelValue, MIMEValue,
                                formatURIValue, altIDsValue, refValue,
                                urlTextField, checksumTextField};
            } else {
                labels =
                        new JLabel[] {labelLabel, MIMELabel, formatURILabel,
                                altIDsLabel, urlLabel, checksumLabel};
                values =
                        new JComponent[] {labelValue, MIMEValue,
                                formatURIValue, altIDsValue, urlTextField,
                                checksumTextField};
            }

            JPanel fieldPanel = new JPanel();
            GridBagLayout fieldGrid = new GridBagLayout();
            fieldPanel.setLayout(fieldGrid);
            addLabelValueRows(labels, values, fieldGrid, fieldPanel);
            add(fieldPanel, BorderLayout.NORTH);

            // SOUTH: buttonPanel
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout());
            if (ds.getMIMEType() != getCustomMimeType(ds)) // Has Custom Viuwer
            {
                if (ContentHandlerFactory.hasViewer(ds.getMIMEType())) {
                    m_viewButton = new JButton("View");
                    Administrator.constrainHeight(m_viewButton);
                    // CENTER: populated on view
                    m_viewButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent evt) {
                            JButton btn = (JButton) evt.getSource();
                            try {
                                ContentViewer v =
                                        ContentHandlerFactory
                                                .getViewer(getCustomMimeType(m_ds),
                                                           Administrator.DOWNLOADER
                                                                   .getDatastreamContent(m_pid,
                                                                                         m_ds
                                                                                                 .getID(),
                                                                                         m_ds
                                                                                                 .getCreateDate()));
                                add(v.getComponent(), BorderLayout.CENTER);
                                btn.setEnabled(false);
                                m_viewTextButton.setEnabled(true);
                                validate();
                            } catch (Exception e) {
                                Administrator
                                        .showErrorDialog(Administrator
                                                                 .getDesktop(),
                                                         "Content View Failure",
                                                         e.getMessage(),
                                                         e);
                            }
                        }
                    });
                    buttonPanel.add(m_viewButton);
                    m_viewTextButton = new JButton("View as Text");
                    Administrator.constrainHeight(m_viewTextButton);
                    // CENTER: populated on view
                    m_viewTextButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent evt) {
                            JButton btn = (JButton) evt.getSource();
                            try {
                                ContentViewer v =
                                        ContentHandlerFactory
                                                .getViewer(m_ds.getMIMEType(),
                                                           Administrator.DOWNLOADER
                                                                   .getDatastreamContent(m_pid,
                                                                                         m_ds
                                                                                                 .getID(),
                                                                                         m_ds
                                                                                                 .getCreateDate()));
                                add(v.getComponent(), BorderLayout.CENTER);
                                btn.setEnabled(false);
                                m_viewButton.setEnabled(true);
                                validate();
                            } catch (Exception e) {
                                Administrator
                                        .showErrorDialog(Administrator
                                                                 .getDesktop(),
                                                         "Content View Failure",
                                                         e.getMessage(),
                                                         e);
                            }
                        }
                    });
                    buttonPanel.add(m_viewTextButton);
                }
            } else // No Custom Viewer
            {
                if (ContentHandlerFactory.hasViewer(ds.getMIMEType())) {
                    m_viewButton = new JButton("View");
                    Administrator.constrainHeight(m_viewButton);
                    // CENTER: populated on view
                    m_viewButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent evt) {
                            JButton btn = (JButton) evt.getSource();
                            try {
                                ContentViewer v =
                                        ContentHandlerFactory
                                                .getViewer(getCustomMimeType(m_ds),
                                                           Administrator.DOWNLOADER
                                                                   .getDatastreamContent(m_pid,
                                                                                         m_ds
                                                                                                 .getID(),
                                                                                         m_ds
                                                                                                 .getCreateDate()));
                                add(v.getComponent(), BorderLayout.CENTER);
                                btn.setEnabled(false);
                                validate();
                            } catch (Exception e) {
                                Administrator
                                        .showErrorDialog(Administrator
                                                                 .getDesktop(),
                                                         "Content View Failure",
                                                         e.getMessage(),
                                                         e);
                            }
                        }
                    });
                    buttonPanel.add(m_viewButton);
                }
            }
            JButton exportButton = new JButton("Export...");
            Administrator.constrainHeight(exportButton);
            exportButton.addActionListener(new ExportActionListener(m_ds));
            buttonPanel.add(exportButton);
            JButton purgeButton = new JButton("Purge...");
            Administrator.constrainHeight(purgeButton);
            purgeButton.addActionListener(m_purgeButtonListener);
            purgeButton.setActionCommand(m_ds.getCreateDate());
            buttonPanel.add(purgeButton);
            add(buttonPanel, BorderLayout.SOUTH);

        }

        public Datastream getDatastream() {
            return m_ds;
        }

    }

    protected class PurgeButtonListener
            implements ActionListener {

        Datastream[] m_versions;

        Object[] m_dateStrings;

        HashMap<String, Integer> m_dsIndex;

        public PurgeButtonListener(Datastream[] versions) {
            m_versions = versions;
            m_dateStrings = new Object[versions.length];
            m_dsIndex = new HashMap<String, Integer>();
            for (int i = 0; i < versions.length; i++) {
                m_dateStrings[i] = versions[i].getCreateDate();
                m_dsIndex.put(versions[i].getCreateDate(), new Integer(i));
            }
        }

        public void actionPerformed(ActionEvent evt) {
            int sIndex1 = 0;
            int sIndex2 = 0;
            boolean canceled = false;
            if (m_versions.length > 1) {
                String defaultValue = evt.getActionCommand(); // default date string
                PurgeDataStreamDialog purgeDialog =
                        new PurgeDataStreamDialog(Administrator.getInstance(),
                                                  m_versions[0].getID(),
                                                  defaultValue,
                                                  m_dateStrings);
                if (purgeDialog.isCanceled()) {
                    canceled = true;
                } else {
                    sIndex1 =
                            (m_dsIndex.get(purgeDialog.getStartDate()))
                                    .intValue();
                    sIndex2 =
                            (m_dsIndex.get(purgeDialog.getEndDate()))
                                    .intValue();
                }
            }
            if (!canceled) {
                // do warning
                boolean removeAll = false;
                String detail;
                if (sIndex1 == 0 && sIndex2 == m_dsIndex.size() - 1) {
                    detail = "the entire datastream.";
                    removeAll = true;
                } else if (sIndex1 == sIndex2) {
                    detail = "one version of the datastream.";
                } else {
                    int num = sIndex2 - sIndex1 + 1;
                    detail = "" + num + " versions of the datastream.";
                }
                int n =
                        JOptionPane
                                .showOptionDialog(Administrator.getDesktop(),
                                                  "This will permanently remove "
                                                          + detail
                                                          + "\n"
                                                          + "Are you sure you want to do this?",
                                                  "Confirmation",
                                                  JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.WARNING_MESSAGE,
                                                  null, //don't use a custom Icon
                                                  new Object[] {"Yes", "No"}, //the titles of buttons
                                                  "Yes"); //default button title
                if (n == 0) {
                    try {
                        Administrator.APIM
                                .purgeDatastream(m_pid,
                                                 m_versions[sIndex1].getID(),
                                                 m_versions[sIndex2]
                                                         .getCreateDate(),
                                                 m_versions[sIndex1]
                                                         .getCreateDate(),
                                                 "DatastreamPane generated this logMessage.", // DEFAULT_LOGMESSAGE
                                                 false); // DEFAULT_FORCE_PURGE
                        if (removeAll) {
                            m_owner.remove(m_versions[0].getID());
                            m_done = true;
                        } else {
                            m_owner.refresh(m_versions[0].getID());
                            m_done = true;
                        }
                    } catch (Exception e) {
                        Administrator
                                .showErrorDialog(Administrator.getDesktop(),
                                                 "Purge error",
                                                 e.getMessage(),
                                                 e);
                    }
                }
            }
        }
    }

    public class PurgeDataStreamDialog
            extends JDialog
            implements ActionListener {

        private static final long serialVersionUID = 1L;

        Object startDate, endDate;

        boolean canceled;

        private final JList list;

        public PurgeDataStreamDialog(JFrame parent,
                                     String datastreamName,
                                     String defaultVal,
                                     Object[] dateStrings) {
            super(parent, "Purge Datastream", true);
            JLabel label =
                    new JLabel("Choose versions of datastream "
                            + datastreamName + " to purge:");
            getContentPane().add(label, BorderLayout.NORTH);
            label.setBorder(new EmptyBorder(10, 10, 0, 10));
            list = new JList(dateStrings);
            list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            JScrollPane scroll = new JScrollPane(list);
            scroll
                    .setBorder(new CompoundBorder(new EmptyBorder(10,
                                                                  10,
                                                                  10,
                                                                  10),
                                                  new LineBorder(Color.BLACK)));
            getContentPane().add(scroll, BorderLayout.CENTER);
            for (int i = 0; i < dateStrings.length; i++) {
                if (dateStrings[i].toString().equals(defaultVal)) {
                    list.setSelectionInterval(i, i);
                }
            }
            JPanel buttons = new JPanel();
            getContentPane().add(buttons, BorderLayout.SOUTH);
            JButton purge = new JButton("Purge");
            purge.addActionListener(this);
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(this);
            buttons.add(purge);
            buttons.add(cancel);
            list.setSize(500, 600);
            pack();
            setLocation(Administrator.getInstance().getCenteredPos(getWidth(),
                                                                   getHeight()));
            canceled = true;
            setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == "Purge") {
                canceled = false;
                Object sel[] = list.getSelectedValues();
                startDate = sel[0];
                endDate = sel[sel.length - 1];
                setVisible(false);
            }
            if (e.getActionCommand() == "Cancel") {
                canceled = true;
                setVisible(false);
            }
        }

        public Object getEndDate() {
            return endDate;
        }

        public Object getStartDate() {
            return startDate;
        }

        public boolean isCanceled() {
            return canceled;
        }
    }

    public class ExportActionListener
            implements ActionListener {

        Datastream m_ds;

        public ExportActionListener(Datastream ds) {
            m_ds = ds;
        }

        public void actionPerformed(ActionEvent evt) {
            try {
                FileDialog dlg =
                        new FileDialog(Administrator.INSTANCE,
                                       "Export Datastream Content to...",
                                       FileDialog.SAVE);
                if (Administrator.getLastDir() != null) {
                    dlg.setDirectory(Administrator.getLastDir().getPath());
                }
                dlg.setVisible(true);
                if (dlg.getFile() != null) {
                    File file =
                            new File(new File(dlg.getDirectory()), dlg
                                    .getFile());
                    LOG.debug("Exporting to " + file.getPath());
                    Administrator.setLastDir(file.getParentFile()); // remember the dir for next time
                    Administrator.DOWNLOADER
                            .getDatastreamContent(m_pid,
                                                  m_ds.getID(),
                                                  m_ds.getCreateDate(),
                                                  new FileOutputStream(file));
                }
            } catch (Exception e) {
                Administrator.showErrorDialog(Administrator.getDesktop(),
                                              "Content Export Failure",
                                              e.getMessage(),
                                              e);
            }
        }
    }

    public String getCustomMimeType(Datastream ds) {
        String dsMimetype = ds.getMIMEType();
        if (ds.getID().equals("RELS-EXT")) {
            dsMimetype = "application/rdf+xml";
        }
        return dsMimetype;
    }

    public InputStream getDatastreamContent(String pid,
                                            String id,
                                            String createDate)
            throws IOException {
        return Administrator.DOWNLOADER.getDatastreamContent(pid,
                                                             id,
                                                             createDate);
    }
}
