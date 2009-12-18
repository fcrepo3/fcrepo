/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;

import fedora.server.utilities.MethodInvokerThread;

/**
 * @author Chris Wilper
 */
public class InvokeDialogListener
        implements ActionListener {

    private final JDialog m_dialog;

    private final ConsoleCommandInvoker m_invoker;

    public InvokeDialogListener(JDialog dialog, ConsoleCommandInvoker invoker) {
        m_dialog = dialog;
        m_invoker = invoker;
    }

    public void actionPerformed(ActionEvent event) {
        m_dialog.setVisible(false);
        if (event.getActionCommand().equals("OK")) {
            try {
                MethodInvokerThread th =
                        new MethodInvokerThread(m_invoker,
                                                m_invoker
                                                        .getClass()
                                                        .getMethod("invoke",
                                                                   new Class[0]),
                                                new Object[0]);
                th.start();
            } catch (NoSuchMethodException nsme) {
                System.out
                        .println("No such method as invoke()? This Shouldnt happen!");
            }
        }
    }
}
