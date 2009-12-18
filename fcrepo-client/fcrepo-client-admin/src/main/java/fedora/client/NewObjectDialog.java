/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import java.io.ByteArrayInputStream;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import fedora.client.actions.ViewObject;
import fedora.client.utility.ingest.AutoIngestor;
import fedora.client.utility.ingest.XMLBuilder;
import fedora.client.utility.ingest.XMLBuilder.OBJECT_TYPE;

import fedora.common.Constants;

/**
 * Launch a dialog for entering information for a new object (title, content
 * model, and possibly a specified pid), then create the object on the server
 * and launch an editor on it.
 *
 * @author Chris Wilper
 */
public class NewObjectDialog
        extends JDialog
        implements Constants, ItemListener {

    private static final long serialVersionUID = 1L;

    private final JTextField m_labelField;

    private final JCheckBox m_customPIDCheckBox;

    private final JTextField m_customPIDField;

    private final OBJECT_TYPE objectType;

    // for the checkbox
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.DESELECTED) {
            // disable text entry
            m_customPIDField.setEditable(false);
        } else if (e.getStateChange() == ItemEvent.SELECTED) {
            // enable text entry
            m_customPIDField.setEditable(true);
        }
    }

    public NewObjectDialog(OBJECT_TYPE objectType, String dialogLabel) {

        super(JOptionPane.getFrameForComponent(Administrator.getDesktop()),
              dialogLabel, true);

        this.objectType = objectType;
        JPanel inputPane = new JPanel();
        inputPane.setBorder(BorderFactory
                .createCompoundBorder(BorderFactory
                        .createCompoundBorder(BorderFactory
                                .createEmptyBorder(6, 6, 6, 6), BorderFactory
                                .createEtchedBorder()), BorderFactory
                        .createEmptyBorder(6, 6, 6, 6)));

        GridBagLayout gridBag = new GridBagLayout();
        inputPane.setLayout(gridBag);

        JLabel labelLabel = new JLabel("Label");
        m_customPIDCheckBox = new JCheckBox("Use Custom PID");
        m_customPIDCheckBox.addItemListener(this);

        m_labelField =
                new JTextField("Enter a one-line description of the object.");
        m_customPIDField = new JTextField();
        m_customPIDField.setEditable(false);

        addLabelValueRows(new JComponent[] {labelLabel, m_customPIDCheckBox},
                          new JComponent[] {m_labelField, m_customPIDField},
                          gridBag,
                          inputPane);

        CreateAction createAction = new CreateAction();
        CreateListener createListener = new CreateListener(createAction);
        JButton okButton = new JButton(createAction);
        okButton
                .registerKeyboardAction(createListener,
                                        KeyStroke
                                                .getKeyStroke(KeyEvent.VK_ENTER,
                                                              0,
                                                              false),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);
        okButton.setText("Create");
        JButton cancelButton = new JButton(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });
        cancelButton.setText("Cancel");
        JPanel buttonPane = new JPanel();
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(inputPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);

        pack();
        setLocation(Administrator.INSTANCE.getCenteredPos(getWidth(),
                                                          getHeight()));
        setVisible(true);
    }

    public void addLabelValueRows(JComponent[] labels,
                                  JComponent[] values,
                                  GridBagLayout gridBag,
                                  Container container) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 6, 6, 6);
        for (int i = 0; i < labels.length; i++) {
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last
            c.fill = GridBagConstraints.NONE; // reset to default
            c.weightx = 0.0; // reset to default
            gridBag.setConstraints(labels[i], c);
            container.add(labels[i]);

            c.gridwidth = GridBagConstraints.REMAINDER; // end row
            if (!(values[i] instanceof JComboBox)) {
                c.fill = GridBagConstraints.HORIZONTAL;
            } else {
                c.anchor = GridBagConstraints.WEST;
            }
            c.weightx = 1.0;
            gridBag.setConstraints(values[i], c);
            container.add(values[i]);
        }

    }

    public class CreateAction
            extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent evt) {
            try {
                String pid = null;
                String label = m_labelField.getText();
                boolean ok = true;
                if (m_labelField.getText().equals("")) {
                    JOptionPane.showMessageDialog(Administrator.getDesktop(),
                                                  "Label must be non-empty",
                                                  "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    ok = false;
                }
                if (m_customPIDCheckBox.isSelected()) {
                    pid = m_customPIDField.getText();
                    if (m_customPIDField.getText().indexOf(":") < 1) {
                        JOptionPane
                                .showMessageDialog(Administrator.getDesktop(),
                                                   "Custom PID should be of the form \"namespace:alphaNumericName\"",
                                                   "Error",
                                                   JOptionPane.ERROR_MESSAGE);
                        ok = false;
                    }
                }

                if (ok) {
                    dispose();
                    XMLBuilder xmlBuilder = new XMLBuilder(Administrator.APIM);
                    String objXML = xmlBuilder.createObjectXML(objectType, pid, label);

                    ByteArrayInputStream in =
                            new ByteArrayInputStream(objXML.getBytes("UTF-8"));
                    String newPID =
                            AutoIngestor
                                    .ingestAndCommit(Administrator.APIA,
                                                     Administrator.APIM,
                                                     in,
                                                     FOXML1_1.uri,
                                                     "Created with Admin GUI \"New Object\" command");
                    new ViewObject(newPID).launch();
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg == null) {
                    msg = e.getClass().getName();
                }
                Administrator.showErrorDialog(Administrator.getDesktop(),
                                              "Error Creating Object",
                                              msg,
                                              e);
            }
        }
    }

    public class CreateListener
            implements ActionListener {

        private final CreateAction m_createAction;

        public CreateListener(CreateAction createAction) {
            m_createAction = createAction;
        }

        public void actionPerformed(ActionEvent e) {
            m_createAction.actionPerformed(e);
        }
    }

}