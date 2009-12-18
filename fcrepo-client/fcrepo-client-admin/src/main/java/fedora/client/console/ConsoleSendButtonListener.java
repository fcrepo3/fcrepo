/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import fedora.client.Administrator;

/**
 * @author Chris Wilper
 */
public class ConsoleSendButtonListener
        implements ActionListener {

    private final Administrator m_mainFrame;

    private final ComboBoxModel m_model;

    private final Console m_console;

    public ConsoleSendButtonListener(ComboBoxModel model,
                                     Administrator mainFrame,
                                     Console console) {
        m_model = model;
        m_mainFrame = mainFrame;
        m_console = console;
    }

    public void actionPerformed(ActionEvent event) {
        ConsoleCommand command = (ConsoleCommand) m_model.getSelectedItem();
        JDialog jd = new JDialog(m_mainFrame, "Send Command", true);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        ConsoleCommandInvoker invoker =
                new ConsoleCommandInvoker(command, m_console);
        invoker.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        panel.add(invoker, BorderLayout.CENTER);
        JPanel okCancelPanel = new JPanel();
        okCancelPanel.setLayout(new BorderLayout());
        JButton okButton = new JButton("OK");
        InvokeDialogListener listener = new InvokeDialogListener(jd, invoker);
        okButton.addActionListener(listener);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(listener);
        okCancelPanel.add(cancelButton, BorderLayout.WEST);
        okCancelPanel.add(okButton, BorderLayout.EAST);
        panel.add(okCancelPanel, BorderLayout.SOUTH);
        jd.getContentPane().add(panel, BorderLayout.CENTER);
        jd.pack();
        jd.setLocation(m_mainFrame
                .getCenteredPos(jd.getWidth(), jd.getHeight()));
        jd.setVisible(true);
    }
}
