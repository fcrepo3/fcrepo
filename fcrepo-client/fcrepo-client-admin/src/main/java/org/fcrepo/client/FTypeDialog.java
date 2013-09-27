/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.client;

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

        throw new UnsupportedOperationException("This operation uses obsolete field search semantics");
    }

    // null means nothing selected or selection canceled
    public String getResult() {
        return selections;
    }

}
