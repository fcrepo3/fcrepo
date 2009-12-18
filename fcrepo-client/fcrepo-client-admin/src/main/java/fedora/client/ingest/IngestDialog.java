/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.ingest;

import java.awt.Font;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

import fedora.client.Administrator;
import fedora.client.ObjectFormatDialog;
import fedora.client.utility.ingest.Ingest;
import fedora.client.utility.ingest.IngestCounter;
import fedora.client.utility.ingest.IngestLogger;

import fedora.server.types.gen.RepositoryInfo;

/**
 * Constructs an interactive ingest dialog for Ingesting.
 */
public class IngestDialog {

    public static int ONE_FROM_FILE = 0;

    public static int MULTI_FROM_DIR = 1;

    public static int ONE_FROM_REPOS = 2;

    public static int MULTI_FROM_REPOS = 3;

    private PrintStream log;

    private File logFile;

    private String logRootName;

    IngestCounter counter = new IngestCounter();

    // launch interactively via Administrator.java
    public IngestDialog(int kind) {
        counter.failures = 0;
        counter.successes = 0;
        log = null;
        logFile = null;
        logRootName = null;
        boolean wasMultiple = false;
        try {
            if (kind == ONE_FROM_FILE) {
                JFileChooser browse =
                        new JFileChooser(Administrator.getLastDir());
                int returnVal =
                        browse.showOpenDialog(Administrator.getDesktop());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = browse.getSelectedFile();
                    Administrator.setLastDir(file.getParentFile());
                    ObjectFormatDialog fmtDialog =
                            new ObjectFormatDialog("Select XML Format of Ingest File(s)");
                    if (fmtDialog.getSelection() != null) {
                        String ingestFormat = fmtDialog.getSelection();
                        String pid =
                                Ingest.oneFromFile(file,
                                                   ingestFormat,
                                                   Administrator.APIA,
                                                   Administrator.APIM,
                                                   null);
                        JOptionPane.showMessageDialog(Administrator
                                .getDesktop(), "Ingest succeeded.  PID='" + pid
                                + "'.");
                    }
                }
            } else if (kind == MULTI_FROM_DIR) {
                wasMultiple = true;
                JFileChooser browse =
                        new JFileChooser(Administrator.getLastDir());
                browse.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal =
                        browse.showOpenDialog(Administrator.getDesktop());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = browse.getSelectedFile();
                    Administrator.setLastDir(file);;
                    logRootName = "ingest-from-dir";
                    logFile = IngestLogger.newLogFile(logRootName);
                    log =
                            new PrintStream(new FileOutputStream(logFile),
                                            true,
                                            "UTF-8");
                    IngestLogger.openLog(log, logRootName);
                    long st = System.currentTimeMillis();
                    ObjectFormatDialog fmtDialog =
                            new ObjectFormatDialog("Select XML Format of Ingest File(s)");
                    if (fmtDialog.getSelection() != null) {
                        String ingestFormat = fmtDialog.getSelection();
                        Ingest.multiFromDirectory(file,
                                                  ingestFormat,
                                                  Administrator.APIA,
                                                  Administrator.APIM,
                                                  null,
                                                  log,
                                                  counter);
                        long et = System.currentTimeMillis();
                        JOptionPane.showMessageDialog(Administrator
                                .getDesktop(), counter.successes
                                + " objects successfully ingested.\n"
                                + counter.failures + " objects failed.\n"
                                + "Time elapsed: "
                                + Ingest.getDuration(et - st));
                    }
                }
            } else if (kind == ONE_FROM_REPOS) {
                SourceRepoDialog sdlg = new SourceRepoDialog();
                if (sdlg.getAPIA() != null) {
                    RepositoryInfo repoinfo =
                        sdlg.getAPIA().describeRepository();
                    String sourceExportFormat = Ingest.getExportFormat(repoinfo);
                    String pid = JOptionPane.
                        showInputDialog("Enter the PID of the object to ingest.");
                    if (pid != null && !pid.equals("")) {
                        pid =
                                Ingest.oneFromRepository(sdlg.getAPIA(),
                                                         sdlg.getAPIM(),
                                                         sourceExportFormat,
                                                         pid,
                                                         Administrator.APIA,
                                                         Administrator.APIM,
                                                         null);
                        JOptionPane.showMessageDialog(Administrator
                                .getDesktop(), "Ingest succeeded.  PID=" + pid);
                    }
                }
            } else if (kind == MULTI_FROM_REPOS) {
                wasMultiple = true;
                SourceRepoDialog sdlg = new SourceRepoDialog();
                if (sdlg.getAPIA() != null) {
                    RepositoryInfo repoinfo =
                        sdlg.getAPIA().describeRepository();
                    String sourceExportFormat = Ingest.getExportFormat(repoinfo);
                    // looks ok... do the request
                    long st = System.currentTimeMillis();
                    logRootName = "ingest-from-repos";
                    logFile = IngestLogger.newLogFile(logRootName);
                    log =
                            new PrintStream(new FileOutputStream(logFile),
                                            true,
                                            "UTF-8");
                    IngestLogger.openLog(log, logRootName);
                    Ingest.multiFromRepository(sdlg.getProtocol(),
                                               sdlg.getHost(),
                                               sdlg.getPort(),
                                               sdlg.getAPIA(),
                                               sdlg.getAPIM(),
                                               sourceExportFormat,
                                               Administrator.APIA,
                                               Administrator.APIM,
                                               null,
                                               log,
                                               counter);
                    long et = System.currentTimeMillis();
                    JOptionPane
                            .showMessageDialog(Administrator.getDesktop(),
                                               counter.successes
                                                       + " objects successfully ingested.\n"
                                                       + counter.failures
                                                       + " objects failed.\n"
                                                       + "Time elapsed: "
                                                       + Ingest.getDuration(et
                                                               - st));
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = e.getClass().getName();
            }
            e.printStackTrace();
            Administrator.showErrorDialog(Administrator.getDesktop(),
                                          "Ingest Failure",
                                          msg,
                                          e);
        } finally {
            try {
                if (log != null && wasMultiple) {
                    IngestLogger.closeLog(log, logRootName);
                    String logPath = logFile.getPath();
                    int n =
                            JOptionPane
                                    .showConfirmDialog(Administrator
                                                               .getDesktop(),
                                                       "A detailed log file was created at\n"
                                                               + logPath
                                                               + "\n\n"
                                                               + "View it now?",
                                                       "View Ingest Log?",
                                                       JOptionPane.YES_NO_OPTION);
                    if (n == JOptionPane.YES_OPTION) {
                        JTextComponent textEditor = new JTextArea();
                        textEditor.setFont(new Font("monospaced",
                                                    Font.PLAIN,
                                                    12));
                        textEditor.setText(fileAsString(logPath));
                        textEditor.setCaretPosition(0);
                        textEditor.setEditable(false);
                        JInternalFrame viewFrame =
                                new JInternalFrame("Viewing " + logPath,
                                                   true,
                                                   true,
                                                   true,
                                                   true);
                        ImageIcon editIcon =
                            new ImageIcon(ClassLoader.
                                          getSystemResource("images/client/standard/general/Edit16.gif"));
                        viewFrame.setFrameIcon(editIcon);
                        viewFrame.getContentPane()
                                .add(new JScrollPane(textEditor));
                        viewFrame.setSize(720, 520);
                        viewFrame.setVisible(true);
                        Administrator.getDesktop().add(viewFrame);
                        try {
                            viewFrame.setSelected(true);
                        } catch (java.beans.PropertyVetoException pve) {
                        }

                    }
                }
            } catch (Exception ex) {
                Administrator.showErrorDialog(Administrator.getDesktop(),
                                              "Error",
                                              ex.getMessage(),
                                              ex);
            }
        }
    }

    private static String fileAsString(String path) throws Exception {
        StringBuffer buffer = new StringBuffer();
        InputStream fis = new FileInputStream(path);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        Reader in = new BufferedReader(isr);
        int ch;
        while ((ch = in.read()) > -1) {
            buffer.append((char) ch);
        }
        in.close();
        return buffer.toString();
    }
}