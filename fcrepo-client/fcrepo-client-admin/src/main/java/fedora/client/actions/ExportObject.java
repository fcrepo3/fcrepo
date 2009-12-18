/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.actions;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;

import java.io.File;
import java.io.FileOutputStream;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import fedora.client.Administrator;
import fedora.client.export.ExportOptionsDialog;
import fedora.client.utility.export.AutoExporter;

/**
 * @author Chris Wilper
 */
public class ExportObject
        extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private Set m_pids;

    private boolean m_prompt;

    public ExportObject() {
        super("One Object...");
        m_prompt = true;
    }

    public ExportObject(String pid) {
        super("Export...");
        m_pids = new HashSet();
        m_pids.add(pid);
    }

    public ExportObject(Set pids) {
        super("Export Objects...");
        m_pids = pids;
    }

    public void actionPerformed(ActionEvent ae) {
        AutoExporter exporter = null;
        try {
            exporter = new AutoExporter(Administrator.APIA, Administrator.APIM);
        } catch (Exception e) {
            Administrator.showErrorDialog(Administrator.getDesktop(),
                                          "Export Failure",
                                          e.getClass().getName() + ": "
                                                  + e.getMessage(),
                                          e);
        }
        if (exporter != null) {
            if (m_prompt) {
                String pid = JOptionPane.showInputDialog("Enter the PID.");
                if (pid == null) {
                    return;
                }
                m_pids = new HashSet();
                m_pids.add(pid);
            }
            Iterator pidIter = m_pids.iterator();
            if (m_pids.size() == 1) {
                // If there's only one pid, get export filename
                String pid = (String) pidIter.next();
                try {
                    FileDialog dlg =
                            new FileDialog(Administrator.INSTANCE,
                                           "Export object to...",
                                           FileDialog.SAVE);
                    if (Administrator.getLastDir() != null) {
                        dlg.setDirectory(Administrator.getLastDir().getPath());
                    }
                    dlg.setVisible(true);
                    if (dlg.getFile() != null) {
                        File file =
                                new File(new File(dlg.getDirectory()), dlg
                                        .getFile());
                        Administrator.setLastDir(file.getParentFile()); // remember the dir for next time
                        ExportOptionsDialog optsDialog =
                                new ExportOptionsDialog("Select Options for Export");
                        if (optsDialog.getFormatSelection() != null) {
                            exporter.export(pid,
                                            optsDialog.getFormatSelection(),
                                            optsDialog.getContextSelection(),
                                            new FileOutputStream(file));
                            JOptionPane.showMessageDialog(Administrator
                                    .getDesktop(), "Exported " + pid);
                        }
                    }
                } catch (Exception e) {
                    Administrator.showErrorDialog(Administrator.getDesktop(),
                                                  "Export Failure",
                                                  e.getClass().getName() + ": "
                                                          + e.getMessage(),
                                                  e);
                }
            } else {
                // If there are multiple pids, select a directory first.
                try {
                    JFileChooser browse;
                    if (Administrator.getLastDir() == null) {
                        browse = new JFileChooser();
                    } else {
                        browse = new JFileChooser(Administrator.getLastDir());
                    }
                    browse.setApproveButtonText("Export");
                    browse.setApproveButtonMnemonic('E');
                    browse
                            .setApproveButtonToolTipText("Exports to the selected directory.");
                    browse.setDialogTitle("Export to...");
                    browse.setDialogTitle("Choose export directory...");
                    browse.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int returnVal =
                            browse.showOpenDialog(Administrator.getDesktop());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        Administrator.setLastDir(browse.getSelectedFile()); // remember the dir for next time
                        ExportOptionsDialog optsDialog =
                                new ExportOptionsDialog("Select Options for Export");
                        if (optsDialog.getFormatSelection() != null) {
                            while (pidIter.hasNext()) {
                                String pid = (String) pidIter.next();
                                StringBuffer buf = new StringBuffer();
                                for (int i = 0; i < pid.length(); i++) {
                                    char c = pid.charAt(i);
                                    if (c == ':') {
                                        buf.append('_');
                                    } else {
                                        buf.append(c);
                                    }
                                }
                                File outFile =
                                        new File(browse.getSelectedFile(), buf
                                                .toString()
                                                + ".xml");
                                exporter
                                        .export(pid,
                                                optsDialog.getFormatSelection(),
                                                optsDialog
                                                        .getContextSelection(),
                                                new FileOutputStream(outFile));
                            }
                            JOptionPane.showMessageDialog(Administrator
                                    .getDesktop(), "Exported " + m_pids.size()
                                    + " objects.");
                        }
                    }
                } catch (Exception e) {
                    Administrator.showErrorDialog(Administrator.getDesktop(),
                                                  "Export Failure",
                                                  e.getClass().getName() + ": "
                                                          + e.getMessage(),
                                                  e);
                }
            }
        }
    }

}
