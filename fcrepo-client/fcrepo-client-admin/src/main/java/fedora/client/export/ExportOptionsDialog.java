/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.export;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import fedora.client.Administrator;

import fedora.common.Constants;

/**
 * Launch a dialog for selecting which XML format to ingest. Valid options are
 * FOXML1_1.uri, FOXML1_0.uri, METS_EXT1_1.uri, METS_EXT1_0, ATOM1_1.uri, and
 * ATOM_ZIP1_1.uri.
 *
 * @author Sandy Payette
 */
public class ExportOptionsDialog
        extends JDialog
        implements Constants {

    private static final long serialVersionUID = 1L;

    private JRadioButton foxml11Button;

    private JRadioButton foxml10Button;

    private JRadioButton mets11Button;

    private JRadioButton mets10Button;

    private JRadioButton atomButton;

    private JRadioButton atomZipButton;

    private final ButtonGroup fmt_buttonGroup = new ButtonGroup();

    protected String fmt_chosen;

    private JRadioButton publicButton;

    private JRadioButton migrateButton;

    private JRadioButton archiveButton;

    private final ButtonGroup ctx_buttonGroup = new ButtonGroup();

    protected String ctx_chosen;

    public ExportOptionsDialog(String title) {
        super(JOptionPane.getFrameForComponent(Administrator.getDesktop()),
              title,
              true);
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent we) {
                fmt_chosen = null;
                ctx_chosen = null;
                dispose();
            }
        });

        // Set up the options input panel
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(2, 1));
        optionsPanel.add(setFormatPanel());
        optionsPanel.add(setContextPanel());

        // Set up the OK and Cancel buttons panel
        JButton okButton = new JButton(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });
        okButton.setText("OK");

        JButton cancelButton = new JButton(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                fmt_chosen = null;
                dispose();
            }
        });
        cancelButton.setText("Cancel");

        JButton helpButton = new JButton(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                showHelp();
            }
        });
        helpButton.setText("Help");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(helpButton);
        buttonPanel.add(cancelButton);

        // Put everything together on the master pane
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(optionsPanel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocation(Administrator.INSTANCE.getCenteredPos(getWidth(),
                                                          getHeight()));
        setVisible(true);
    }

    private JPanel setFormatPanel() {
        JPanel formatPanel = new JPanel();
        formatPanel.setLayout(new GridLayout(0, 1));
        formatPanel.setBorder(BorderFactory.createCompoundBorder(
                                  BorderFactory.createCompoundBorder(
                                      BorderFactory.createEmptyBorder(12, 12, 0, 12),
                                      BorderFactory.createEtchedBorder()),
                                      BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        String text = "Select the desired export FORMAT";
        JLabel label = new JLabel(text);
        formatPanel.add(label);

        // foxml 1.1 radio button
        foxml11Button = new JRadioButton("FOXML (Fedora Object XML) version 1.1", true);
        foxml11Button.setActionCommand(FOXML1_1.uri);
        foxml11Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (foxml11Button.isSelected()) {
                    fmt_chosen = FOXML1_1.uri;
                }
            }
        });
        formatPanel.add(foxml11Button);

        // foxml 1.0 radio button
        foxml10Button = new JRadioButton("FOXML (Fedora Object XML) version 1.0", true);
        foxml10Button.setActionCommand(FOXML1_0.uri);
        foxml10Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (foxml10Button.isSelected()) {
                    fmt_chosen = FOXML1_0.uri;
                }
            }
        });
        formatPanel.add(foxml10Button);

        // mets 1.1 radio button
        mets11Button = new JRadioButton("METS (Fedora METS Extension) version 1.1", false);
        mets11Button.setActionCommand(METS_EXT1_1.uri);
        mets11Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (mets11Button.isSelected()) {
                    fmt_chosen = METS_EXT1_1.uri;
                }
            }
        });
        formatPanel.add(mets11Button);

        // mets 1.0 radio button
        mets10Button = new JRadioButton("METS (Fedora METS Extension) version 1.0", false);
        mets10Button.setActionCommand(METS_EXT1_0.uri);
        mets10Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (mets10Button.isSelected()) {
                    fmt_chosen = METS_EXT1_0.uri;
                }
            }
        });
        formatPanel.add(mets10Button);

        // atom radio button
        atomButton = new JRadioButton("ATOM (Fedora Atom)", false);
        atomButton.setActionCommand(ATOM1_1.uri);
        atomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (atomButton.isSelected()) {
                    fmt_chosen = ATOM1_1.uri;
                }
            }
        });
        formatPanel.add(atomButton);

        // atom zip radio button
        atomZipButton = new JRadioButton("ATOM Zip", false);
        atomZipButton.setActionCommand(ATOM_ZIP1_1.uri);
        atomZipButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (atomZipButton.isSelected()) {
                    fmt_chosen = ATOM_ZIP1_1.uri;
                }
            }
        });
        formatPanel.add(atomZipButton);

        // button grouping and default value
        fmt_buttonGroup.add(foxml11Button);
        fmt_buttonGroup.add(foxml10Button);
        fmt_buttonGroup.add(mets11Button);
        fmt_buttonGroup.add(mets10Button);
        fmt_buttonGroup.add(atomButton);
        fmt_buttonGroup.add(atomZipButton);
        fmt_chosen = FOXML1_1.uri;
        return formatPanel;
    }

    private JPanel setContextPanel() {
        JPanel contextPanel = new JPanel();
        contextPanel.setLayout(new GridLayout(0, 1));
        contextPanel.setBorder(BorderFactory.createCompoundBorder(
                                   BorderFactory.createCompoundBorder(
                                       BorderFactory.createEmptyBorder(12, 12, 12, 12),
                                       BorderFactory.createEtchedBorder()),
                                       BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        String text = "Select the desired export CONTEXT";
        JLabel label = new JLabel(text);
        contextPanel.add(label);

        // migrate radio button
        migrateButton = new JRadioButton("Migrate", true);
        migrateButton.setActionCommand("migrate");
        migrateButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (migrateButton.isSelected()) {
                    ctx_chosen = "migrate";
                }
            }
        });
        contextPanel.add(migrateButton);

        // public radio button
        publicButton = new JRadioButton("Public Access", false);
        publicButton.setActionCommand("public");
        publicButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (publicButton.isSelected()) {
                    ctx_chosen = "public";
                }
            }
        });
        contextPanel.add(publicButton);

        // archive radio button
        archiveButton = new JRadioButton("Archive", false);
        archiveButton.setActionCommand("archive");
        archiveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (archiveButton.isSelected()) {
                    ctx_chosen = "archive";
                }
            }
        });
        //archiveButton.setEnabled(false);
        contextPanel.add(archiveButton);

        // button grouping and default value
        ctx_buttonGroup.add(migrateButton);
        ctx_buttonGroup.add(publicButton);
        ctx_buttonGroup.add(archiveButton);
        ctx_chosen = "migrate";
        return contextPanel;
    }

    // null means nothing selected or selection canceled
    public String getFormatSelection() {
        return fmt_chosen;
    }

    // null means nothing selected or selection canceled
    public String getContextSelection() {
        return ctx_chosen;
    }

    private void showHelp() {
        JTextArea helptxt = new JTextArea();
        helptxt.setLineWrap(true);
        helptxt.setWrapStyleWord(true);
        helptxt.setBounds(0, 0, 500, 50);
        helptxt
                .append("There are two sections to the Export option dialog that"
                        + " must be completed:\n\n"
                        + " (1) Select the export FORMAT:\n\n"
                        + "     FOXML 1.1 - select this option if you want the export file\n"
                        + "             to be encoded according to the FOXML 1.1 XML schema.\n\n"
                        + "     FOXML 1.0 - select this option if you want the export file\n"
                        + "             to be encoded according to the FOXML 1.0 XML schema.\n\n"
                        + "     METS 1.1 - select this option if you want the export file\n"
                        + "             to be encoded according to version 1.1 of the Fedora\n"
                        + "             extension of the METS XML schema.\n\n"
                        + "     METS 1.0 - select this option if you want the export file\n"
                        + "             to be encoded according to version 1.0 of the Fedora\n"
                        + "             extension of the METS XML schema.\n\n"
                        + "     ATOM  - select this option if you want the export file\n"
                        + "             to be encoded according to the ATOM XML schema.\n\n"
                        + "     ATOM ZIP - select this option if you want the export file\n"
                        + "             to be encoded according to the ATOM ZIP XML schema.\n\n"
                        + " *************************************************************************\n"
                        + " (2) Select the export CONTEXT:\n\n"
                        + "     Migrate - (Default) select this option if you want the export file\n"
                        + "               to be appropriate for migration of an object from one\n"
                        + "               Fedora repository to another.  Any URLs that reference\n"
                        + "               the host:port of export repository will be specially encoded\n"
                        + "               so that the URLs will recognized by Fedora as relative\n"
                        + "               to whatever repository the object is subsequently stored.\n"
                        + "               When the export file is ingested into a new Fedora repository\n"
                        + "               the Fedora ingest process will ensure that the URLs\n"
                        + "               become local to the *new* repository.\n\n"
                        + "    Public Access - select this option if you want the export file\n"
                        + "               to be appropriate for use outside the context of a Fedora\n"
                        + "               repository.  All URLs that reference datastream content or\n"
                        + "               disseminations from the Fedora repository will be public\n"
                        + "               callback URLs to the exporting repository.\n\n"
                        + "    Archive - (Future Release) select this option if you want the export file\n"
                        + "               to serve as a self-contained archive of the object, where\n"
                        + "               all datastream content is directly in the export file.\n"
                        + "               Binary content will be base64-encoded and XML content inlined.\n");

        JOptionPane.showMessageDialog(this,
                                      helptxt,
                                      "Help for Export Options",
                                      JOptionPane.OK_OPTION);
    }
}
