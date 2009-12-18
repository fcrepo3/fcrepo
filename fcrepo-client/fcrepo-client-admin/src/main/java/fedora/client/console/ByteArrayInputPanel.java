/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import fedora.common.Constants;

import fedora.server.utilities.StreamUtility;

/**
 * @author Chris Wilper
 */
public class ByteArrayInputPanel
        extends InputPanel
        implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final JTextField m_textField;

    private final JTextField m_fileField;

    private JFileChooser m_browse;

    private final JRadioButton m_fromTextRadioButton;

    private static File s_lastDir;

    public ByteArrayInputPanel(boolean primitive) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if (Constants.FEDORA_HOME != null) {
            File f = new File(Constants.FEDORA_HOME);
            if (f.exists() && f.isDirectory()) {
                s_lastDir = f;
            }
        }
        JPanel fromText = new JPanel();
        fromText.setLayout(new BoxLayout(fromText, BoxLayout.X_AXIS));
        m_fromTextRadioButton = new JRadioButton("Text: ");
        m_fromTextRadioButton.setSelected(true);
        fromText.add(m_fromTextRadioButton);
        m_textField = new JTextField(10);
        fromText.add(m_textField);
        add(fromText);

        JPanel fromFile = new JPanel();
        fromFile.setLayout(new BoxLayout(fromFile, BoxLayout.X_AXIS));
        JRadioButton fromFileRadioButton = new JRadioButton("File: ");
        fromFile.add(fromFileRadioButton);
        m_fileField = new JTextField(10);
        fromFile.add(m_fileField);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(this);
        fromFile.add(browseButton);

        ButtonGroup g = new ButtonGroup();
        g.add(m_fromTextRadioButton);
        g.add(fromFileRadioButton);
        add(fromFile);

        if (s_lastDir == null) {
            m_browse = new JFileChooser();
        } else {
            m_browse = new JFileChooser(s_lastDir);
        }
    }

    public void actionPerformed(ActionEvent e) {
        int returnVal = m_browse.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = m_browse.getSelectedFile();
            s_lastDir = file.getParentFile(); // remember the dir for next time
            m_fileField.setText(file.getAbsolutePath());
        }
    }

    @Override
    public Object getValue() {
        if (m_fromTextRadioButton.isSelected()) {
            return m_textField.getText().getBytes();
        } else {
            File f = new File(m_fileField.getText());
            if (!f.exists() || f.isDirectory()) {
                System.out.println("returning null..file doesnt exist");
                return null;
            }
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                FileInputStream in = new FileInputStream(f);
                StreamUtility.pipeStream(in, out, 4096);
                return out.toByteArray();
            } catch (IOException ioe) {
                System.out.println("ioexecption getting filestream: "
                        + ioe.getMessage());
                return null;
            }
        }
    }

}
