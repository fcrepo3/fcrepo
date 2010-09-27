/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.KeyStroke;

import org.apache.axis.AxisFault;

import org.apache.log4j.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fedora.client.actions.Login;
import fedora.client.actions.PurgeObject;
import fedora.client.actions.ViewObject;
import fedora.client.actions.ViewObjectXML;
import fedora.client.batch.BatchModify;
import fedora.client.batch.BatchModifyValidate;
import fedora.client.console.access.AccessConsole;
import fedora.client.console.management.ManagementConsole;
import fedora.client.export.ExportDialog;
import fedora.client.ingest.IngestDialog;
import fedora.client.search.Search;
import fedora.client.utility.ingest.XMLBuilder.OBJECT_TYPE;

import fedora.common.Constants;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;

import fedora.swing.mdi.MDIDesktopPane;
import fedora.swing.mdi.WindowMenu;

import fedora.utilities.Log4J;

/**
 * Fedora Administrator GUI.
 *
 * @author Chris Wilper
 */
public class Administrator
        extends JFrame {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(Administrator.class.getName());

    private static final long serialVersionUID = 1L;

    private static MDIDesktopPane s_desktop;

    private static int s_maxButtonHeight;

    private JDialog m_aboutDialog;

    private static File s_lastDir;

    protected static File batchtoolLastDir;

    //public static ClassLoader cl;

    public static JTextArea WATCH_AREA;

    private static Administrator s_instance;

    private final JLabel m_aboutPic;

    private final JLabel m_aboutText;

    private static String s_protocol;

    private static String s_context;

    private static String s_host;

    private static int s_port;

    private static String s_user;

    private static String s_pass;

    public static JProgressBar PROGRESS;

    public static Downloader DOWNLOADER;

    public static Uploader UPLOADER;

    public static Color ACTIVE_COLOR = new Color(180, 210, 180);

    public static Color INACTIVE_COLOR = new Color(210, 210, 180);

    public static Color DELETED_COLOR = new Color(210, 180, 180);

    public static Color DEFAULT_COLOR = new Color(185, 185, 185);

    public static Color DESKTOP_COLOR = new Color(46, 97, 116); // from website

    public static Color BACKGROUND_COLOR;

    public static FedoraAPIA APIA = null;

    public static FedoraAPIM APIM = null;

    public static File BASE_DIR;

    public static Administrator INSTANCE = null;

    private static ResourceBundle s_const =
            ResourceBundle.getBundle("fedora.client.resources.Client");

    public static String VERSION = s_const.getString("version");

    public static String BUILD_DATE = s_const.getString("buildDate");

    public static String BUILD_NUMBER = s_const.getString("buildNumber");

    public static final void showErrorDialog(Component parent,
                                             String title,
                                             String explanation,
                                             Exception e) {
        if (e instanceof AxisFault) {
            StringBuffer authzDetail = new StringBuffer("");
            org.w3c.dom.Element[] getFaultDetails =
                    ((AxisFault) e).getFaultDetails();
            if (getFaultDetails != null) {
                for (Element detail : getFaultDetails) {
                    if ("Authz".equals(detail.getLocalName())
                            && detail.hasChildNodes()) {
                        NodeList nodeList = detail.getChildNodes();
                        for (int j = 0; j < nodeList.getLength(); j++) {
                            authzDetail.append(nodeList.item(j).getNodeValue());
                        }
                    }
                }
            }
            if (authzDetail.length() > 0) {
                explanation = authzDetail.toString();
            }
        }
        JOptionPane.showMessageDialog(parent,
                                      explanation,
                                      title,
                                      JOptionPane.ERROR_MESSAGE);
    }

    public Administrator(String protocol,
                         String host,
                         int port,
                         String context,
                         String user,
                         String pass) {
        super("Fedora Administrator");
        INSTANCE = this;
        WATCH_AREA = new JTextArea();
        WATCH_AREA.setFont(new Font("monospaced", Font.PLAIN, 12));
        WATCH_AREA.setCaretPosition(0);

        s_maxButtonHeight = new JTextField("test").getPreferredSize().height;
        BACKGROUND_COLOR = new JPanel().getBackground();

        if (host != null) {
            // already must have passed through non-interactive login
            try {
                //APIA=APIAStubFactory.getStub(protocol, host, port, user, pass);
                //APIM=APIMStubFactory.getStub(protocol, host, port, user, pass);

                // ******************************************
                // NEW: use new client utility class
                // FIXME:  Get around hardcoding the path in the baseURL
                String baseURL =
                        protocol + "://" + host + ":" + port + "/" + context;
                FedoraClient fc = new FedoraClient(baseURL, user, pass);
                APIA = fc.getAPIA();
                APIM = fc.getAPIM();
                //*******************************************
                setLoginInfo(protocol, host, port, context, user, pass);
            } catch (Exception e) {
                APIA = null;
                APIM = null;
            }
        }

        if (Constants.FEDORA_HOME != null) {
            File f = new File(Constants.FEDORA_HOME);
            if (f.exists() && f.isDirectory()) {
                BASE_DIR = new File(f, "client");
                s_lastDir = BASE_DIR;
            }
        }

        ImageIcon aboutIcon =
            new ImageIcon(ClassLoader.getSystemResource("images/client/fedora/aboutadmin.gif"));
        m_aboutPic = new JLabel(aboutIcon);
        m_aboutText =
                new JLabel("<html>"
                        + "<p>Copyright 2009, DuraSpace</p>"
                        + "<p>Copyright 2008-2009, Fedora Commons, Inc.</p>"
                        + "<p>Copyright 2002-2007, The Rector and Visitors of the</p>"
                        + "<p>University of Virginia and Cornell University.</p><p></p>"
                        + "<p><b>License: </b>This software is subject to the terms of the</p>"
                        + "<p>Apache License, Version 2.0 (the \"License\"); you may not use</p>"
                        + "<p>this software except in compliance with the License. You may</p>"
                        + "<p>obtain a copy of the License at:</p>"
                        + "<blockquote>http://www.apache.org/licenses/</blockquote><p></p>"
                        + "<p>Software distributed under the License is distributed on an \"AS IS\"</p>"
                        + "<p>basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.</p>"
                        + "<p>See the License for the specific language governing rights and</p>"
                        + "<p>limitations under the License.</p><p></p>"
                        + "<p><b>Version: </b>" + VERSION + "</p>"
                        + "<p><b>Build Date: </b>" + BUILD_DATE + "</p>"
                        + "<p><b>Build Number: </b>" + BUILD_NUMBER + "</p>"
                        + "<p></p>"
                        + "<p>See http://fedora-commons.org/ for more information.</p></html>");

        m_aboutText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel splashPicAndText = new JPanel();
        splashPicAndText.setLayout(new BorderLayout());
        splashPicAndText.setBorder(BorderFactory.createLineBorder(Color.black,
                                                                  5));
        splashPicAndText.add(m_aboutPic, BorderLayout.CENTER);
        splashPicAndText.add(m_aboutText, BorderLayout.SOUTH);
        JWindow splashScreen = new JWindow();
        splashScreen.getContentPane().add(splashPicAndText);
        splashScreen.pack();
        int xSize = splashScreen.getWidth();
        int ySize = splashScreen.getHeight();
        Dimension screenSize = getToolkit().getScreenSize();
        int xLoc = screenSize.width / 2 - xSize / 2;
        int yLoc = screenSize.height / 2 - ySize / 2;
        splashScreen.setBounds(xLoc, yLoc, xSize, ySize);
        splashScreen.setVisible(true);

        ImageIcon fedoraIcon =
            new ImageIcon(ClassLoader.getSystemResource("images/client/fedora/fedora-icon16.gif"));
        setIconImage(fedoraIcon.getImage());
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        s_desktop = new MDIDesktopPane();
        s_desktop.setBackground(DESKTOP_COLOR);
        s_desktop.setVisible(true);
        mainPanel.add(new JScrollPane(s_desktop), BorderLayout.CENTER);
        PROGRESS = new JProgressBar(0, 2000);
        PROGRESS.setValue(0);
        PROGRESS.setStringPainted(true);
        PROGRESS.setString("");

        mainPanel.add(PROGRESS, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);
        setJMenuBar(createMenuBar());

        //Make dragging faster:
        //s_desktop.putClientProperty("JDesktopPane.dragMode", "outline");

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        splashScreen.setVisible(false);
        s_instance = this;

        int xs = 1000;
        int ys = 744;
        Dimension sz = getToolkit().getScreenSize();
        int xl = sz.width / 2 - xs / 2;
        int yl = sz.height / 2 - ys / 2;
        setBounds(xl, yl, xs, ys);
        setVisible(true);

        if (APIA == null || APIM == null) {
            new LoginDialog(); // first thing to do... launch login dialog
        }
        if (APIA == null || APIM == null) {
            dispose();
            System.exit(0);
        }
    }

    public static JDesktopPane getDesktop() {
        return s_desktop;
    }

    public void setLoginInfo(String protocol,
                             String host,
                             int port,
                             String context,
                             String user,
                             String pass) {
        s_protocol = protocol;
        s_host = host;
        s_port = port;
        s_context = context;
        s_user = user;
        s_pass = pass;
        try {
            DOWNLOADER = new Downloader(host, port, context, user, pass);
            UPLOADER = new Uploader(host, port, context, user, pass);
        } catch (IOException ioe) {
        }
        doTitle();
    }

    public void doTitle() {
        setTitle("Fedora Administrator - " + s_user + "@" + s_host + ":"
                + s_port);
    }

    public static Administrator getInstance() {
        return s_instance;
    }

    public static JComponent constrainHeight(JComponent component) {
        int preferredWidth = component.getPreferredSize().width;
        component.setPreferredSize(new Dimension(preferredWidth,
                                                 s_maxButtonHeight));
        component.setMaximumSize(new Dimension(2048, s_maxButtonHeight));
        component.setMinimumSize(new Dimension(preferredWidth,
                                               s_maxButtonHeight));
        return component;
    }

    protected JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        //   [N]ew
        JMenu fileNew = new JMenu("New");
        fileNew.setMnemonic(KeyEvent.VK_N);

        JMenuItem fileNewObject = new JMenuItem("Data Object", KeyEvent.VK_O);
        fileNewObject.setAccelerator(KeyStroke
                .getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        fileNewObject.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new NewObjectDialog(OBJECT_TYPE.dataObject, "New Object");
            }
        });

        JMenuItem fileNewCModel = new JMenuItem("Content Model", KeyEvent.VK_C);
        fileNewCModel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new NewObjectDialog(OBJECT_TYPE.contentModel,
                                    "New Content Model");
            }
        });

        JMenuItem fileNewSDef =
                new JMenuItem("Service Definition", KeyEvent.VK_D);
        fileNewSDef.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new NewObjectDialog(OBJECT_TYPE.serviceDefinition,
                                    "New Service Definition");
            }
        });

        JMenuItem fileNewSDep =
                new JMenuItem("Service Deployment", KeyEvent.VK_M);
        fileNewSDep.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new NewObjectDialog(OBJECT_TYPE.serviceDeployment,
                                    "New Service Deployment");
            }
        });

        fileNew.add(fileNewObject);
        fileNew.add(fileNewCModel);
        fileNew.add(fileNewSDef);
        fileNew.add(fileNewSDep);

        //   [O]pen
        JMenuItem fileOpen = new JMenuItem(new ViewObject());
        fileOpen.setMnemonic(KeyEvent.VK_O);
        fileOpen
                .setToolTipText("Launches a viewer/editor for an object and it's components.");
        fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                                                       ActionEvent.CTRL_MASK));

        //   [I]ngest
        JMenu fileIngest = new JMenu("Ingest");
        fileIngest.setMnemonic(KeyEvent.VK_I);
        JMenu fileIngestOne = new JMenu("One Object");
        fileIngestOne.setMnemonic(KeyEvent.VK_O);
        JMenuItem fileIngestOneFromFile =
                new JMenuItem("From File...", KeyEvent.VK_F);
        fileIngestOneFromFile.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new IngestDialog(IngestDialog.ONE_FROM_FILE);
            }
        });
        JMenuItem fileIngestOneFromRepository =
                new JMenuItem("From Repository...", KeyEvent.VK_R);
        fileIngestOneFromRepository.setAccelerator(KeyStroke
                .getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
        fileIngestOneFromRepository.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new IngestDialog(IngestDialog.ONE_FROM_REPOS);
            }
        });
        fileIngestOne.add(fileIngestOneFromFile);
        fileIngestOne.add(fileIngestOneFromRepository);
        JMenu fileIngestMultiple = new JMenu("Multiple Objects");
        fileIngestMultiple.setMnemonic(KeyEvent.VK_M);
        JMenuItem fileIngestMultipleFromFile =
                new JMenuItem("From Directory...", KeyEvent.VK_D);
        fileIngestMultipleFromFile.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new IngestDialog(IngestDialog.MULTI_FROM_DIR);
            }
        });
        JMenuItem fileIngestMultipleFromRepository =
                new JMenuItem("From Repository...", KeyEvent.VK_R);
        fileIngestMultipleFromRepository
                .addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new IngestDialog(IngestDialog.MULTI_FROM_REPOS);
                    }
                });

        fileIngestMultiple.add(fileIngestMultipleFromFile);
        fileIngestMultiple.add(fileIngestMultipleFromRepository);
        fileIngest.add(fileIngestOne);
        fileIngest.add(fileIngestMultiple);

        //   [E]xport
        JMenu fileExport = new JMenu("Export");
        fileExport.setMnemonic(KeyEvent.VK_E);

        JMenuItem fileExportObject = new JMenuItem("One Object...");
        fileExportObject.setMnemonic(KeyEvent.VK_O);
        fileExportObject.setAccelerator(KeyStroke
                .getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
        fileExportObject
                .setToolTipText("Exports a serialized Digitial Object to disk.");
        fileExportObject.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new ExportDialog(ExportDialog.ONE);
            }
        });

        JMenuItem fileExportMultiple = new JMenuItem("Multiple Objects...");
        fileExportMultiple.setMnemonic(KeyEvent.VK_M);
        fileExportMultiple
                .setToolTipText("Exports multiple serialized Digitial Objects to disk.");
        fileExportMultiple.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new ExportDialog(ExportDialog.MULTI);
            }
        });
        fileExport.add(fileExportObject);
        fileExport.add(fileExportMultiple);

        //     [V]iew Object XML
        JMenuItem fileViewXML = new JMenuItem(new ViewObjectXML());
        fileViewXML.setMnemonic(KeyEvent.VK_V);
        fileViewXML.setAccelerator(KeyStroke
                .getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        fileViewXML
                .setToolTipText("Launches a viewer for the internal XML of an object in the repository.");
        //     [P]urge Object
        JMenuItem filePurge = new JMenuItem(new PurgeObject());
        filePurge.setMnemonic(KeyEvent.VK_P);
        filePurge.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                                                        ActionEvent.CTRL_MASK));
        filePurge
                .setToolTipText("Permanently removes a Digitial Object from the repository.");

        //   [L]ogin
        JMenuItem fileLogin = new JMenuItem(new Login());
        fileLogin.setMnemonic(KeyEvent.VK_R);
        fileLogin.setToolTipText("Changes the working repository.");
        fileLogin.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                                                        ActionEvent.CTRL_MASK));

        //   E[x]it
        JMenuItem fileExit = new JMenuItem("Exit", KeyEvent.VK_X);
        fileExit.setToolTipText("Exits the application");
        fileExit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();
                System.exit(0);
            }
        });

        fileMenu.add(fileNew);
        fileMenu.add(fileOpen);
        fileMenu.addSeparator();
        fileMenu.add(fileIngest);
        fileMenu.add(fileExport);
        fileMenu.addSeparator();
        fileMenu.add(filePurge);
        fileMenu.add(fileViewXML);
        fileMenu.addSeparator();
        fileMenu.add(fileLogin);
        fileMenu.add(fileExit);

        menuBar.add(fileMenu);

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);

        JMenuItem toolsSearch =
                new JMenuItem("Search/Browse Repository", KeyEvent.VK_S);
        toolsSearch.setAccelerator(KeyStroke
                .getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        toolsSearch.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createSearchRepository();
            }
        });
        toolsMenu.add(toolsSearch);

        JMenu toolsBatchSubMenu = new JMenu("Batch");

        JMenuItem toolsBatchBuild = new JMenuItem("Build Batch"/*
                                                                * ,
                                                                * KeyEvent.VK_A
                                                                */);
        toolsBatchBuild.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createBatchBuildConsole();
            }
        });
        toolsBatchSubMenu.add(toolsBatchBuild);

        JMenuItem toolsBatchBuildIngest =
                new JMenuItem("Build and Ingest Batch"/*
                                                       * , KeyEvent.VK_A
                                                       */);
        toolsBatchBuildIngest.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createBatchBuildIngestConsole();
            }
        });
        toolsBatchSubMenu.add(toolsBatchBuildIngest);

        JMenuItem toolsBatchIngest = new JMenuItem("Ingest Batch"/*
                                                                  * ,
                                                                  * KeyEvent.VK_A
                                                                  */);
        toolsBatchIngest.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createBatchIngestConsole();
            }
        });
        toolsBatchSubMenu.add(toolsBatchIngest);

        JMenu toolsBatchModify = new JMenu("Modify Batch");
        toolsBatchModify.setMnemonic(KeyEvent.VK_M);
        JMenuItem executeBatchModify =
                new JMenuItem("Process Directives", KeyEvent.VK_P);
        toolsBatchModify.setToolTipText("Modifies a batch of objects based on "
                + "modify directives specified in a file on disk.");
        executeBatchModify.setToolTipText("Run the Batch Modify Utility.");
        executeBatchModify.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new BatchModify();
            }
        });
        toolsBatchModify.add(executeBatchModify);

        JMenuItem validateBatchModify =
                new JMenuItem("Validate Directives File", KeyEvent.VK_V);
        validateBatchModify
                .setToolTipText("Validate the modify directives file against the batchModify XML Schema.");
        validateBatchModify.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new BatchModifyValidate();
            }
        });

        toolsBatchModify.add(validateBatchModify);

        toolsBatchSubMenu.add(toolsBatchModify);
        toolsMenu.addSeparator();
        toolsMenu.add(toolsBatchSubMenu);

        //     [A]ccess Console
        JMenuItem toolsAccess = new JMenuItem("Access API", KeyEvent.VK_A);
        toolsAccess.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createAccessConsole();
            }
        });
        //     [M]anagement Console
        JMenuItem toolsManagement =
                new JMenuItem("Management API", KeyEvent.VK_M);
        toolsManagement.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createManagementConsole();
            }
        });

        JMenu toolsConsole = new JMenu("Console");
        toolsConsole.setMnemonic(KeyEvent.VK_C);
        toolsConsole.add(toolsAccess);
        toolsConsole.add(toolsManagement);
        toolsMenu.add(toolsConsole);

        menuBar.add(toolsMenu);

        WindowMenu windowMenu = new WindowMenu(s_desktop, "Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        menuBar.add(windowMenu);

        // [H]elp
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        JMenuItem helpContents = new JMenuItem("Documentation", KeyEvent.VK_D);
        String portPart = "";
        if (getPort() != 80) {
            portPart = ":" + getPort();
        }

        helpContents.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String documentationURL = "http://fedora-commons.org/go/fcr30";
                JOptionPane.showMessageDialog(getDesktop(),
                                              "For Fedora documentation, see "
                                                      + documentationURL,
                                              "Fedora Documentation",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        });

        m_aboutDialog = new JDialog(this, "About Fedora Administrator", true);

        m_aboutDialog.getContentPane().add(m_aboutPic, BorderLayout.CENTER);
        JButton aboutClose = new JButton("Close");

        JPanel infoAndButton = new JPanel();
        infoAndButton.setLayout(new BorderLayout());
        infoAndButton.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        infoAndButton.add(m_aboutText);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(aboutClose);
        infoAndButton.add(buttonPane, BorderLayout.SOUTH);

        m_aboutDialog.getContentPane().add(infoAndButton, BorderLayout.SOUTH);
        aboutClose.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                m_aboutDialog.setVisible(false);
            }
        });
        m_aboutDialog.pack();

        JMenuItem helpAbout =
                new JMenuItem("About Fedora Administrator", KeyEvent.VK_A);
        helpAbout.setToolTipText("Gives brief information this application");
        helpAbout.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                m_aboutDialog.setLocation(getCenteredPos(m_aboutDialog
                        .getWidth(), m_aboutDialog.getHeight()));
                m_aboutDialog.setVisible(true);
            }
        });

        helpMenu.add(helpContents);
        helpMenu.addSeparator();
        helpMenu.add(helpAbout);

        menuBar.add(helpMenu);

        return menuBar;
    }

    public static File getLastDir() {
        return s_lastDir;
    }

    public static void setLastDir(File f) {
        s_lastDir = f;
    }

    protected void createSearchRepository() {
        Search frame = new Search();
        frame.setVisible(true);
        s_desktop.add(frame);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
    }

    protected void createManagementConsole() {
        ManagementConsole frame = new ManagementConsole(this);
        frame.setVisible(true);
        s_desktop.add(frame);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
    }

    protected void createAccessConsole() {
        AccessConsole frame = new AccessConsole(this);
        frame.setVisible(true);
        s_desktop.add(frame);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
    }

    protected void createBatchBuildConsole() {
        BatchBuildGUI frame = new BatchBuildGUI(this, s_desktop);
        frame.setVisible(true);
        s_desktop.add(frame);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
    }

    protected void createBatchBuildIngestConsole() {
        BatchBuildIngestGUI frame =
                new BatchBuildIngestGUI(this,
                                        s_desktop,
                                        s_host,
                                        s_port,
                                        s_context,
                                        s_user,
                                        s_pass);
        frame.setVisible(true);
        s_desktop.add(frame);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
    }

    protected void createBatchIngestConsole() {
        BatchIngestGUI frame =
                new BatchIngestGUI(this,
                                   s_desktop,
                                   s_host,
                                   s_port,
                                   s_context,
                                   s_user,
                                   s_pass);
        frame.setVisible(true);
        s_desktop.add(frame);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
    }

    public Point getCenteredPos(int xSize, int ySize) {
        Dimension screenSize = getToolkit().getScreenSize();
        int maxXPos = screenSize.width - xSize;
        int maxYPos = screenSize.height - ySize;
        int centerX = getX() + getWidth() / 2;
        int centerY = getY() + getHeight() / 2;
        int prefXPos = centerX - xSize / 2;
        int prefYPos = centerY - ySize / 2;
        if (prefXPos < 0) {
            prefXPos = 0;
        }
        if (prefXPos > maxXPos) {
            prefXPos = maxXPos;
        }
        if (prefYPos < 0) {
            prefYPos = 0;
        }
        if (prefYPos > maxYPos) {
            prefYPos = maxYPos;
        }
        return new Point(prefXPos, prefYPos);
    }

    public static String getProtocol() {
        return s_protocol;
    }

    public static String getHost() {
        return s_host;
    }

    public static int getPort() {
        return s_port;
    }

    public static String getUser() {
        return s_user;
    }

    public static String getPass() {
        return s_pass;
    }

    public static String getAppServContext() {
        return s_context;
    }

    public static void main(String[] args) {
        Log4J.force();
        if (args.length == 1) {
            int socketTimeoutSeconds = 120;
            try {
                socketTimeoutSeconds = Integer.parseInt(args[0]);
            } catch (Exception e) {
            }
            APIAStubFactory.SOCKET_TIMEOUT_SECONDS = socketTimeoutSeconds;
            LOG.info("Socket timeout set to " + socketTimeoutSeconds
                    + " seconds");
        }
        String protocol = null;
        String host = null;
        int port = 0;
        String user = null;
        String pass = null;
        String context = null;
        Administrator administrator =
                new Administrator(protocol, host, port, context, user, pass);
    }
}
