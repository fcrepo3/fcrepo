/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import fedora.common.Constants;

import fedora.swing.mdi.MDIDesktopPane;

/**
 * Batch Ingest GUI.
 *
 * @author Bill Niebel
 * @version $Id$
 */
public class BatchIngestGUI
        extends JInternalFrame
        implements Constants {

    private static final long serialVersionUID = 1L;

    //private static File s_lastDir;
    private final JTextField m_objectsField = new JTextField("", 10);

    private final JTextField m_pidsField = new JTextField("", 10);

    private final JRadioButton m_xmlMap = new JRadioButton("xml");

    private final JRadioButton m_textMap = new JRadioButton("text");

    private final ButtonGroup buttonGroup = new ButtonGroup();

    private final JRadioButton m_foxmlMap = new JRadioButton("foxml");

    private final JRadioButton m_metsMap = new JRadioButton("mets");

    private final ButtonGroup templateButtonGroup = new ButtonGroup();

    private Dimension unitDimension = null;

    private Dimension browseMin = null;

    private Dimension browsePref = null;

    private Dimension browseMax = null;

    private Dimension textMin = null;

    private Dimension textPref = null;

    private Dimension textMax = null;

    private Dimension okMin = null;

    private Dimension okPref = null;

    private Dimension okMax = null;

    private MDIDesktopPane mdiDesktopPane = null;

    BatchOutput batchOutput = new BatchOutput("Batch Ingest Output");

    private final String host;

    private final String port;

    private final String user;

    private final String context;

    private final String pass;

    public BatchIngestGUI(JFrame parent,
                          MDIDesktopPane mdiDesktopPane,
                          String host,
                          int port,
                          String context,
                          String user,
                          String pass) {
        super("Batch Ingest", true, //resizable
              true, //closable
              true, //maximizable
              true);//iconifiable

        this.host = host;
        this.port = Integer.toString(port);
        this.user = user;
        this.pass = pass;
        this.context = context;

        this.mdiDesktopPane = mdiDesktopPane;

        JButton btn = new JButton("Ingest this batch");
        btn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ingestBatch();
            }
        });
        JPanel entryPanel = new JPanel();
        entryPanel.setLayout(new BorderLayout());
        entryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        entryPanel.add(new JLabel("Ingest Criteria"), BorderLayout.NORTH);
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new GridLayout(0, 3));

        Graphics graphicsTemp = parent.getGraphics();
        FontMetrics fmTemp = graphicsTemp.getFontMetrics();
        int maxWidth = 0;
        {
            int[] temp = fmTemp.getWidths();
            for (int element : temp) {
                if (element > maxWidth) {
                    maxWidth = element;
                }
            }
        }
        unitDimension =
                new Dimension((new Float(1.5 * maxWidth)).intValue(), fmTemp
                        .getHeight());
        browseMin =
                new Dimension(12 * unitDimension.width, unitDimension.height); // 9*unitDimension.width
        browseMax = new Dimension(2 * browseMin.width, 2 * browseMin.height);
        browsePref = browseMin;

        textMin = new Dimension(22 * unitDimension.width, unitDimension.height);
        textMax = new Dimension(2 * textMin.width, 2 * textMin.height);
        textPref = textMin;

        okMin = new Dimension(9 * unitDimension.width, unitDimension.height);
        okMax =
                new Dimension((new Float(1.5 * okMin.width)).intValue(),
                              (new Float(1.5 * okMin.height)).intValue());
        okPref = okMax;

        templateButtonGroup.add(m_foxmlMap);
        m_foxmlMap.setSelected(true);
        templateButtonGroup.add(m_metsMap);
        JPanel templatePanel = new JPanel();

        templatePanel.setLayout(new BorderLayout());
        templatePanel.add(m_foxmlMap, BorderLayout.WEST);
        templatePanel.add(new JLabel("Fedora objects (input directory)"),
                          BorderLayout.NORTH);
        templatePanel.add(m_metsMap, BorderLayout.CENTER);
        labelPanel.add(sized(templatePanel, browseMin, browsePref, browseMax));

        //labelPanel.add(new JLabel("Fedora objects (input directory)"));
        labelPanel.add(sized(m_objectsField, textMin, textPref, textMax));
        JButton objectsBtn = new JButton("browse...");
        objectsBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                objectsAction();
            }
        });
        labelPanel.add(sized(objectsBtn, browseMin, browsePref, browseMax));

        buttonGroup.add(m_xmlMap);
        m_xmlMap.setSelected(true);
        buttonGroup.add(m_textMap);
        JPanel jPanel = new JPanel();

        jPanel.setLayout(new BorderLayout());
        jPanel.add(m_xmlMap, BorderLayout.WEST);
        jPanel.add(new JLabel("object processing map (output file)"),
                   BorderLayout.NORTH);
        jPanel.add(m_textMap, BorderLayout.CENTER);
        labelPanel.add(sized(jPanel, browseMin, browsePref, browseMax));

        labelPanel.add(sized(m_pidsField, textMin, textPref, textMax));
        JButton pidsBtn = new JButton("browse...");
        pidsBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                pidsAction();
            }
        });
        labelPanel.add(sized(pidsBtn, browseMin, browsePref, browseMax));

        entryPanel.add(labelPanel, BorderLayout.WEST);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(entryPanel, BorderLayout.CENTER);
        getContentPane().add(sized(btn, okMin, okPref, okMax, true),
                             BorderLayout.SOUTH);

        ImageIcon exportIcon =
            new ImageIcon(ClassLoader.
                          getSystemResource("images/client/standard/general/Export16.gif"));
        setFrameIcon(exportIcon);

        pack();
        setSize(getSize().width + 20, getSize().height * 2);
        //setSize(400,400);
    }

    private final void sizeIt(JComponent jc,
                              Dimension min,
                              Dimension pref,
                              Dimension max) {
        jc.setMinimumSize(min);
        jc.setPreferredSize(pref);
        jc.setMaximumSize(max);
    }

    private final Box sized(JComponent jc,
                            Dimension min,
                            Dimension pref,
                            Dimension max,
                            boolean centered) {
        sizeIt(jc, min, pref, max);
        Box box = Box.createHorizontalBox();
        if (centered) {
            box.add(Box.createGlue());
        }
        box.add(jc);
        if (centered) {
            box.add(Box.createGlue());
        }
        return box;
    }

    private final Box sized(JComponent jc,
                            Dimension min,
                            Dimension pref,
                            Dimension max) {
        return sized(jc, min, pref, max, false);
    }

    public void ingestBatch() {
        try {
            if (!m_objectsField.getText().equals("")
                    && !m_pidsField.getText().equals("")) {
                Properties properties = new Properties();
                properties.setProperty("ingest", "yes");
                properties.setProperty("objects", m_objectsField.getText());
                properties.setProperty("ingested-pids", m_pidsField.getText());
                properties.setProperty("pids-format",
                                       m_xmlMap.isSelected() ? "xml" : "text");
                properties.setProperty("server-fqdn", host);
                properties.setProperty("server-port", port);
                properties.setProperty("context", context);
                properties.setProperty("username", user);
                properties.setProperty("password", pass);
                properties.setProperty("server-protocol", Administrator
                        .getProtocol());
                properties.setProperty("object-format",
                                       m_foxmlMap.isSelected() ? FOXML1_1.uri
                                               : METS_EXT1_1.uri);

                batchOutput.setDirectoryPath(properties
                        .getProperty("ingested-pids")); //2003.12.03 niebel -- duplicate output to file

                try {
                    mdiDesktopPane.add(batchOutput);
                } catch (Exception eee) { //illegal component position occurs ~ every other time ?!?
                    mdiDesktopPane.add(batchOutput);
                }

                try {
                    batchOutput.setSelected(true);
                } catch (java.beans.PropertyVetoException e) {
                    System.err.println("BatchIngestGUI"
                            + " frame select vetoed " + e.getMessage());
                }

                BatchThread batchThread = null;
                try {
                    batchThread =
                            new BatchThread(batchOutput, batchOutput
                                    .getJTextArea(), "Ingesting Batch . . .");
                } catch (Exception e) {
                    System.err.println("BatchIngestGUI"
                            + " couldn't instantiate BatchThread "
                            + e.getMessage());
                }
                batchThread.setProperties(properties);
                batchThread.start();
            }

        } catch (Exception e) {
            System.err.println("BatchIngestGUI" + " general error "
                    + e.getMessage());
        }
    }

    protected File selectFile(File lastDir, boolean directoriesOnly)
            throws Exception {
        File selection = null;
        JFileChooser browse;
        if (Administrator.batchtoolLastDir == null) {
            browse = new JFileChooser();
        } else {
            browse = new JFileChooser(Administrator.batchtoolLastDir);
        }
        if (directoriesOnly) {
            browse.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        int returnVal = browse.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selection = browse.getSelectedFile();
            Administrator.batchtoolLastDir = selection.getParentFile(); // remember the dir for next time
        }
        return selection;
    }

    protected void objectsAction() {
        try {
            File temp = selectFile(Administrator.batchtoolLastDir, true);
            if (temp != null) {
                m_objectsField.setText(temp.getPath());
            }
        } catch (Exception e) {
            m_objectsField.setText("");
        }
    }

    protected void pidsAction() {
        try {
            FileDialog dlg =
                    new FileDialog(Administrator.INSTANCE,
                                   "PIDs Output File",
                                   FileDialog.SAVE);
            if (Administrator.batchtoolLastDir != null) {
                dlg.setDirectory(Administrator.batchtoolLastDir.getPath());
            }
            dlg.setVisible(true);
            String temp = dlg.getFile();
            if (temp != null) {
                File dir = new File(dlg.getDirectory());
                m_pidsField.setText(new File(dir, temp).getPath());
                Administrator.batchtoolLastDir = dir;
            }
        } catch (Exception e) {
            m_pidsField.setText("");
        }
    }

}
