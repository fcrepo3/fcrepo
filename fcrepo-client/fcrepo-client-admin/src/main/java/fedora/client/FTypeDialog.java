/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Launch a dialog for selecting which object type(s) the user is interested in:
 * sSefs, sDeps, data objects (any combination).
 * 
 * <p>Result will come back as string consisting of combination of "D", "M", 
 * and "O" characters, respectively, if selected.
 * 
 * @author Chris Wilper
 * @deprecated.  May replace/refactor with something that selects content models
 */
public class FTypeDialog
        extends JDialog {

    private static final long serialVersionUID = 1L;

    private String selections;

    private final JCheckBox dButton;

    private final JCheckBox mButton;

    private final JCheckBox cButton;

    private final JCheckBox oButton;

    public FTypeDialog() {
        super(JOptionPane.getFrameForComponent(Administrator.getDesktop()),
              "Select Object Type(s)",
              true);

        if (true) {
            /* FIXME: find some other way to do this */
            throw new UnsupportedOperationException("This operation uses obsolete field search semantics");
        }
        JPanel inputPane = new JPanel();
        inputPane.setBorder(BorderFactory
                .createCompoundBorder(BorderFactory
                        .createCompoundBorder(BorderFactory
                                .createEmptyBorder(6, 6, 6, 6), BorderFactory
                                .createEtchedBorder()), BorderFactory
                        .createEmptyBorder(6, 6, 6, 6)));

        inputPane.setLayout(new GridLayout(0, 1));
        dButton = new JCheckBox("Service Definitions");
        dButton.setMnemonic(KeyEvent.VK_D);
        mButton = new JCheckBox("Service Deployments");
        mButton.setMnemonic(KeyEvent.VK_M);
        cButton = new JCheckBox("Content Models");
        cButton.setMnemonic(KeyEvent.VK_C);
        oButton = new JCheckBox("Data Objects");
        oButton.setMnemonic(KeyEvent.VK_O);
        oButton.setSelected(true);
        inputPane.add(dButton);
        inputPane.add(mButton);
        inputPane.add(cButton);
        inputPane.add(oButton);

        JButton okButton = new JButton(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                selections = "";
                if (dButton.isSelected()) {
                    selections += "D";
                }
                if (mButton.isSelected()) {
                    selections += "M";
                }
                if (oButton.isSelected()) {
                    selections += "O";
                }
                if (cButton.isSelected()) {
                    selections += "C";
                }
                if (selections.equals("")) {
                    selections = null;
                }
                dispose();
            }
        });
        okButton.setText("OK");
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

    // null means nothing selected or selection canceled
    public String getResult() {
        return selections;
    }

}
