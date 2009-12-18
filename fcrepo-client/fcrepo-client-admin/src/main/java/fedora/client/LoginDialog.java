/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;

/**
 * Launch a dialog for logging into a Fedora repository.
 *
 * @author Chris Wilper
 */
public class LoginDialog
        extends JDialog {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(LoginDialog.class.getName());

    private static final long serialVersionUID = 1L;

    private final JComboBox m_serverComboBox;

    private final JComboBox m_protocolComboBox;

    private final JComboBox m_usernameComboBox;

    private final JPasswordField m_passwordField;

    private final JComboBox m_contextComboBox;

    private String m_lastUsername = "fedoraAdmin";

    private String m_lastServer = "localhost:8080";

    private String m_lastProtocol = "http";

    private String m_lastContext = Constants.FEDORA_DEFAULT_APP_CONTEXT;

    private final HashMap<String, String> m_usernames;

    private final HashMap<String, String> m_servers;

    private final HashMap<String, String> m_protocols;

    private final HashMap<String, String> m_contexts;

    public LoginDialog() {
        super(JOptionPane.getFrameForComponent(Administrator.getDesktop()),
              "Login",
              true);

        m_servers = new HashMap<String, String>();
        m_contexts = new HashMap<String, String>();
        m_protocols = new HashMap<String, String>();
        m_protocols.put("http", "");
        m_protocols.put("https", "");
        m_usernames = new HashMap<String, String>();

        JLabel serverLabel = new JLabel("Fedora Server");
        JLabel protocolLabel = new JLabel("Protocol");
        JLabel usernameLabel = new JLabel("Username");
        JLabel passwordLabel = new JLabel("Password");
        JLabel contextLabel = new JLabel("Context");

        m_serverComboBox = new JComboBox();
        m_serverComboBox.setEditable(true);
        m_protocolComboBox = new JComboBox();
        m_protocolComboBox.setEditable(true);
        m_contextComboBox = new JComboBox();
        m_contextComboBox.setEditable(true);
        m_usernameComboBox = new JComboBox();
        m_usernameComboBox.setEditable(true);
        m_passwordField = new JPasswordField();

        setComboBoxValues();

        LoginAction loginAction = new LoginAction(this);
        JButton loginButton = new JButton(loginAction);
        loginAction.setButton(loginButton);
        loginButton.setEnabled(false);
        m_passwordField
                .getDocument()
                .addDocumentListener(new PasswordChangeListener(loginButton,
                                                                m_passwordField));
        m_passwordField.setAction(loginAction);

        JPanel inputPane = new JPanel();
        inputPane.setBorder(BorderFactory
                .createCompoundBorder(BorderFactory
                        .createCompoundBorder(BorderFactory
                                .createEmptyBorder(6, 6, 6, 6), BorderFactory
                                .createEtchedBorder()), BorderFactory
                        .createEmptyBorder(6, 6, 6, 6)));
        GridBagLayout gridBag = new GridBagLayout();
        inputPane.setLayout(gridBag);
        addLabelValueRows(new JLabel[] {serverLabel, protocolLabel,
                contextLabel, usernameLabel, passwordLabel}, new JComponent[] {
                m_serverComboBox, m_protocolComboBox, m_contextComboBox,
                m_usernameComboBox, m_passwordField}, gridBag, inputPane);

        JButton cancelButton = new JButton(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });
        if (Administrator.APIA == null) {
            cancelButton.setText("Exit"); // if haven't logged in yet
        } else {
            cancelButton.setText("Cancel");
        }
        JPanel buttonPane = new JPanel();
        buttonPane.add(loginButton);
        buttonPane.add(cancelButton);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(inputPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent evt) {
                m_passwordField.requestFocus();
            }
        });
        pack();
        setLocation(Administrator.INSTANCE.getCenteredPos(getWidth(),
                                                          getHeight()));
        setVisible(true);
    }

    // re-writes fedora-admin.properties with latest values for servers
    // and usernames
    public void saveProperties() {
        try {
            Properties props = new Properties();
            props.setProperty("lastServer", m_lastServer);
            props.setProperty("lastProtocol", m_lastProtocol);
            props.setProperty("lastUsername", m_lastUsername);
            props.setProperty("lastContext", m_lastContext);

            Iterator<String> iter;
            int i;
            iter = m_servers.keySet().iterator();
            i = 0;
            while (iter.hasNext()) {
                String name = iter.next();
                props.setProperty("server" + i, name);
                i++;
            }
            iter = m_protocols.keySet().iterator();
            i = 0;
            while (iter.hasNext()) {
                String name = iter.next();
                props.setProperty("protocol" + i, name);
                i++;
            }
            iter = m_usernames.keySet().iterator();
            i = 0;
            while (iter.hasNext()) {
                String name = iter.next();
                props.setProperty("username" + i, name);
                i++;
            }
            iter = m_contexts.keySet().iterator();
            i = 0;
            while (iter.hasNext()) {
                String name = iter.next();
                props.setProperty("context" + i, name);
                i++;
            }

            props
                    .store(new FileOutputStream(new File(Administrator.BASE_DIR,
                                                         "fedora-admin.properties")),
                           "Fedora Administrator saved settings");
        } catch (Exception e) {
            System.err.println("Warning: Error writing properties: "
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void setComboBoxValues() {
        // get values from prop file, or use localhost:8080/fedoraAdmin if none
        try {
            Properties props = new Properties();
            props
                    .load(new FileInputStream(new File(Administrator.BASE_DIR,
                                                       "fedora-admin.properties")));
            Enumeration<?> names = props.propertyNames();
            while (names.hasMoreElements()) {
                String prop = (String) names.nextElement();
                if (prop.equals("lastServer")) {
                    m_lastServer = props.getProperty(prop);
                } else if (prop.equals("lastProtocol")) {
                    m_lastProtocol = props.getProperty(prop);
                } else if (prop.startsWith("lastContext")) {
                    m_lastContext = props.getProperty(prop);
                } else if (prop.equals("lastUsername")) {
                    m_lastUsername = props.getProperty(prop);
                } else if (prop.startsWith("server")) {
                    m_servers.put(props.getProperty(prop), "");
                } else if (prop.startsWith("protocol")) {
                    m_protocols.put(props.getProperty(prop), "");
                } else if (prop.startsWith("context")) {
                    m_contexts.put(props.getProperty(prop), "");
                } else if (prop.startsWith("username")) {
                    m_usernames.put(props.getProperty(prop), "");
                }
            }
        } catch (Exception e) {
            // if props file doesn't exist, this must be the first
            // time they've run the java client for this install,
            // so point them to the new one.
            JOptionPane.showMessageDialog(Administrator.getDesktop(),
                    "Check out the new, web-based Fedora Administrator.\n"
                  + "\n       http://localhost:8080/fedora/admin\n\n"
                  + "NOTE: The web-based Fedora Admin GUI will completely\n"
                  + "replace this one in future releases of Fedora.",
                    "Try the *New* Fedora Admin GUI!",
                    JOptionPane.INFORMATION_MESSAGE);  
        }
        // finally, populate them
        m_serverComboBox.addItem(m_lastServer);
        Iterator<String> sIter = m_servers.keySet().iterator();
        while (sIter.hasNext()) {
            String a = sIter.next();
            if (!a.equals(m_lastServer)) {
                m_serverComboBox.addItem(a);
            }
        }
        m_servers.put(m_lastServer, "");

        m_protocolComboBox.addItem(m_lastProtocol);
        Iterator<String> protocolIter = m_protocols.keySet().iterator();
        while (protocolIter.hasNext()) {
            String a = protocolIter.next();
            if (!a.equals(m_lastProtocol)) {
                m_protocolComboBox.addItem(a);
            }
        }
        m_protocols.put(m_lastProtocol, "");

        m_contextComboBox.addItem(m_lastContext);
        Iterator<String> contextIter = m_contexts.keySet().iterator();
        while (contextIter.hasNext()) {
            String a = contextIter.next();
            if (!a.equals(m_lastContext)) {
                m_contextComboBox.addItem(a);
            }
        }
        m_contexts.put(m_lastContext, "");


        m_usernameComboBox.addItem(m_lastUsername);
        Iterator<String> uIter = m_usernames.keySet().iterator();
        while (uIter.hasNext()) {
            String a = uIter.next();
            if (!a.equals(m_lastUsername)) {
                m_usernameComboBox.addItem(a);
            }
        }
        m_usernames.put(m_lastUsername, "");

        // make all entry widgets same size
        Dimension newSize =
                new Dimension(m_serverComboBox.getPreferredSize().width + 20,
                              m_serverComboBox.getPreferredSize().height);
        m_serverComboBox.setPreferredSize(newSize);
        m_protocolComboBox.setPreferredSize(newSize);
        m_usernameComboBox.setPreferredSize(newSize);
        m_passwordField.setPreferredSize(newSize);
    }

    public void addLabelValueRows(JLabel[] labels,
                                  JComponent[] values,
                                  GridBagLayout gridBag,
                                  Container container) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 6, 6, 6);
        for (int i = 0; i < labels.length; i++) {
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE; //reset to default
            c.weightx = 0.0; //reset to default
            gridBag.setConstraints(labels[i], c);
            container.add(labels[i]);

            c.gridwidth = GridBagConstraints.REMAINDER; //end row
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

    // sets Administrator.APIA/M if success, throws Exception if fails.
    public void tryLogin(String protocol,
                         String host,
                         int port,
                         String context,
                         String user,
                         String pass) throws Exception {

        try {
            LOG.info("Logging in...");
            // get a FedoraClient
            String baseURL = protocol + "://" + host + ":" + port + "/" + context;
            FedoraClient fc = new FedoraClient(baseURL, user, pass);

            // attempt to connect via REST
            String serverVersion = fc.getServerVersion();

            // ensure client is compatible with server, warn if not
            List<String> compatibleVersions =
                    FedoraClient.getCompatibleServerVersions();
            if (!compatibleVersions.contains(serverVersion)) {
                StringBuffer endText = new StringBuffer();
                if (compatibleVersions.size() == 1) {
                    // version A
                    endText.append("version " + compatibleVersions.get(0));
                } else {
                    // versions A and B
                    // versions A, B, and C
                    endText.append("versions ");
                    for (int i = 0; i < compatibleVersions.size(); i++) {
                        if (i > 0) {
                            if (i == compatibleVersions.size() - 1) {
                                if (i > 1) {
                                    endText.append(",");
                                }
                                endText.append(" and ");
                            } else {
                                endText.append(", ");
                            }
                        }
                        endText.append(compatibleVersions.get(i));
                    }
                }
                System.err.println("WARNING: Server version is "
                        + serverVersion + ".  This client is "
                        + "only designed to work with " + endText.toString());
            }

            // set SOAP stubs for Administrator
            Administrator.APIA = fc.getAPIA();
            Administrator.APIM = fc.getAPIM();

        } catch (Exception e) {
            if (e.getMessage().indexOf("Unauthorized") != -1
                    || e.getMessage().indexOf("Unrecognized") != -1) {
                throw new IOException("Bad username or password.");
            } else {
                if (e.getMessage() != null) {
                    throw new IOException(e.getClass().getName() + ": "
                            + e.getMessage());
                } else {
                    throw new IOException(e.getClass().getName());
                }
            }
        }
    }

    public class PasswordChangeListener
            implements DocumentListener {

        private final JButton m_loginButton;

        private final JPasswordField m_passField;

        public PasswordChangeListener(JButton loginButton, JPasswordField pf) {
            m_loginButton = loginButton;
            m_passField = pf;
        }

        public void changedUpdate(DocumentEvent e) {
            dataChanged();
        }

        public void insertUpdate(DocumentEvent e) {
            dataChanged();
        }

        public void removeUpdate(DocumentEvent e) {
            dataChanged();
        }

        public void dataChanged() {
            if (m_passField.getPassword().length == 0) {
                m_loginButton.setEnabled(false);
            } else {
                m_loginButton.setEnabled(true);
            }
        }

    }

    public class LoginAction
            extends AbstractAction {

        private static final long serialVersionUID = 1L;

        LoginDialog m_loginDialog;

        JButton m_button;

        public LoginAction(LoginDialog loginDialog) {
            super("Login");
            m_loginDialog = loginDialog;
        }

        public void setButton(JButton button) {
            m_button = button;
        }

        public void actionPerformed(ActionEvent evt) {
            if (m_button.isEnabled()) {
                FedoraAPIA oldAPIA = Administrator.APIA;
                FedoraAPIM oldAPIM = Administrator.APIM;
                try {
                    // pull out values and do a quick syntax check
                    String hostPort =
                            (String) m_serverComboBox.getSelectedItem();
                    int colonPos = hostPort.indexOf(":");
                    if (colonPos == -1) {
                        throw new IOException("Server must be specified as host:port");
                    }
                    String[] s = hostPort.split(":");
                    String host = s[0];
                    if (host.length() == 0) {
                        throw new IOException("No server name provided.");
                    }
                    int port = 0;
                    try {
                        port = Integer.parseInt(s[1]);
                    } catch (NumberFormatException nfe) {
                        throw new IOException("Server port must be an integer.");
                    }
                    String protocol =
                            (String) m_protocolComboBox.getSelectedItem();
                    if (protocol.equals("")) {
                        throw new IOException("No protocol provided.");
                    }
                    String context = (String) m_contextComboBox.getSelectedItem();
                    if (context.equals("")){
                        throw new IOException("No context provided");
                    }

                    String username =
                            (String) m_usernameComboBox.getSelectedItem();
                    if (username.equals("")) {
                        throw new IOException("No username provided.");
                    }
                    String pass = new String(m_passwordField.getPassword());

                    tryLogin(protocol, host, port, context, username, pass);
                    // all looks ok...just save stuff and exit now
                    m_lastServer = host + ":" + port;
                    m_lastProtocol = protocol;
                    m_lastUsername = username;
                    m_loginDialog.saveProperties();
                    Administrator.INSTANCE.setLoginInfo(protocol,
                                                        host,
                                                        port,
                                                        context,
                                                        username,
                                                        pass);
                    m_loginDialog.dispose();
                } catch (Exception e) {
                    String msg = e.getMessage();
                    Administrator.showErrorDialog(m_loginDialog,
                                                  "Login Error",
                                                  msg,
                                                  e);
                    Administrator.APIA = oldAPIA;
                    Administrator.APIM = oldAPIM;
                }
            }
        }
    }

}
