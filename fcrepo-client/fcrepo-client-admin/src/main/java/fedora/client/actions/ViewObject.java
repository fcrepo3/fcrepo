/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.actions;

import java.awt.event.ActionEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import fedora.client.Administrator;
import fedora.client.objecteditor.ObjectEditorFrame;

/**
 * Launches an object viewer/editor window.
 * 
 * @author Chris Wilper
 */
public class ViewObject
        extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private Set m_pids;

    private boolean m_prompt;

    public ViewObject() {
        super("Open Object...");
        m_prompt = true;
    }

    public ViewObject(String pid) {
        super("Open Object");
        m_pids = new HashSet();
        m_pids.add(pid);
    }

    public ViewObject(Set pids) {
        super("Open Objects");
        m_pids = pids;
    }

    public void actionPerformed(ActionEvent ae) {
        launch();
    }

    public void launch() {
        if (m_prompt) {
            String pid =
                    JOptionPane
                            .showInputDialog("Enter the PID of the object to open.");
            if (pid == null) {
                return;
            }
            m_pids = new HashSet();
            m_pids.add(pid);
        }
        Iterator pidIter = m_pids.iterator();
        while (pidIter.hasNext()) {
            String pid = (String) pidIter.next();
            try {
                ObjectEditorFrame editor = new ObjectEditorFrame(pid, 0);
                editor.setVisible(true);
                Administrator.getDesktop().add(editor);
                editor.setSelected(true);
            } catch (Exception e) {
                e.printStackTrace();
                Administrator.showErrorDialog(Administrator.getDesktop(),
                                              "Error Opening Object",
                                              e.getClass().getName() + ": "
                                                      + e.getMessage(),
                                              e);
            }
        }
    }

}
