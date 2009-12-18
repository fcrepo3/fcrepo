/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.objecteditor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import fedora.client.Administrator;
import fedora.client.objecteditor.types.MethodDefinition;
import fedora.client.objecteditor.types.ParameterDefinition;

/**
 * 
 */
public class ServiceDescriptionPanel
        extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Map<String, JPanel> m_loadedPanels;

    private final JComponent m_containerToValidate;

    /**
     * Initialize with information for the indicated service definition. If
     * sDefPID is given as null, don't display anything yet. If
     * containerToValidate is not given as null, that container will be
     * validate()ed each time this panel changes its structure.
     */
    public ServiceDescriptionPanel(String sDefPID,
                                    JComponent containerToValidate)
            throws IOException {
        m_containerToValidate = containerToValidate;
        setLayout(new BorderLayout());
        m_loadedPanels = new HashMap<String, JPanel>();
        if (sDefPID != null) {
            setSDef(sDefPID);
        }
    }

    /**
     * Switch what is displayed (if anything) with information for the indicated
     * service definition. If null is given, clear what is currently displayed.
     */
    public void setSDef(String sDefPID) throws IOException {
        removeAll();
        if (sDefPID != null) {
            JPanel lp = m_loadedPanels.get(sDefPID);
            if (lp == null) {
                lp = makePanel(sDefPID);
                m_loadedPanels.put(sDefPID, lp);
            }
            add(lp, BorderLayout.CENTER);
        }
        if (m_containerToValidate != null) {
            m_containerToValidate.revalidate();
            m_containerToValidate.repaint(new Rectangle(m_containerToValidate
                    .getSize()));
        }
    }

    /**
     * Create and return a new panel describing the service definition. Methods
     * dropdown "methodName - descriptionIfExists" Parameters "None." |
     * tabbedPane
     */
    private JPanel makePanel(String sDefPID) throws IOException {

        JTextArea supportsMethodsTextArea = new JTextArea("   defines method");
        supportsMethodsTextArea.setLineWrap(false);
        supportsMethodsTextArea.setEditable(false);
        supportsMethodsTextArea.setBackground(Administrator.BACKGROUND_COLOR);
        JTextArea methodParametersTextArea = new JTextArea("   with parm(s)");
        methodParametersTextArea.setLineWrap(false);
        methodParametersTextArea.setEditable(false);
        methodParametersTextArea.setBackground(Administrator.BACKGROUND_COLOR);

        JComponent[] left =
                new JComponent[] {supportsMethodsTextArea,
                        methodParametersTextArea};

        //
        // Methods
        //
        java.util.List methodDefs = Util.getMethodDefinitions(sDefPID);
        String[] methodSelections = new String[methodDefs.size()];
        for (int i = 0; i < methodDefs.size(); i++) {
            MethodDefinition def = (MethodDefinition) methodDefs.get(i);
            StringBuffer buf = new StringBuffer();
            buf.append(def.getName());
            if (def.getLabel() != null) {
                buf.append(" - ");
                buf.append(def.getLabel());
            }
            methodSelections[i] = buf.toString();
        }
        final JComboBox methodComboBox = new JComboBox(methodSelections);
        Administrator.constrainHeight(methodComboBox);

        //
        // Parameters... ParameterPanel handles the switching and displaying
        //
        final ParameterPanel parameterPanel = new ParameterPanel(methodDefs);

        methodComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                String[] parts =
                        ((String) methodComboBox.getSelectedItem())
                                .split(" - ");
                parameterPanel.show(parts[0]);
                parameterPanel.revalidate();
            }
        });

        JComponent[] right = new JComponent[] {methodComboBox, parameterPanel};

        GridBagLayout gb = new GridBagLayout();
        JPanel panel = new JPanel(gb);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        Util.addRows(left, right, gb, panel, true, false);

        return panel;
    }

    class ParameterPanel
            extends JPanel {

        private static final long serialVersionUID = 1L;

        private final CardLayout m_cardLayout;

        public ParameterPanel(java.util.List methodDefs) {
            m_cardLayout = new CardLayout();
            setLayout(m_cardLayout);
            for (int i = 0; i < methodDefs.size(); i++) {
                MethodDefinition def = (MethodDefinition) methodDefs.get(i);
                add(makePane(def), def.getName());
            }
        }

        public void show(String methodName) {
            m_cardLayout.show(this, methodName);
        }

        private JComponent makePane(MethodDefinition def) {
            if (def.parameterDefinitions().size() == 0) {
                JTextArea noParams = new JTextArea("no parameters.");
                noParams.setLineWrap(false);
                noParams.setEditable(false);
                noParams.setBackground(Administrator.BACKGROUND_COLOR);
                JPanel pane = new JPanel(new BorderLayout());
                JPanel leftPane = new JPanel(new BorderLayout());
                leftPane.add(noParams, BorderLayout.NORTH);
                pane.add(leftPane, BorderLayout.WEST);
                return pane;
            }
            JTabbedPane pane = new JTabbedPane();
            for (int i = 0; i < def.parameterDefinitions().size(); i++) {
                ParameterDefinition parmDef =
                        (ParameterDefinition) def.parameterDefinitions().get(i);
                pane.add(parmDef.getName(), makeDescPane(parmDef));
            }
            return pane;
        }

        private JPanel makeDescPane(ParameterDefinition parmDef) {
            StringBuffer buf = new StringBuffer();
            if (parmDef.isRequired()) {
                buf.append("Required.");
            } else {
                buf.append("Optional. ");
                if (parmDef.getDefaultValue() != null
                        && parmDef.getDefaultValue().length() > 0) {
                    buf
                            .append("Defaults to " + parmDef.getDefaultValue()
                                    + ".");
                }
            }
            if (parmDef.getLabel() != null) {
                buf.append(" " + parmDef.getLabel());
            }
            if (parmDef.validValues().size() > 0) {
                buf.append(" Valid values: ");
                for (int k = 0; k < parmDef.validValues().size(); k++) {
                    if (k > 0) {
                        buf.append(", ");
                    }
                    buf.append((String) parmDef.validValues().get(k));
                }
            }
            JTextArea desc = new JTextArea(buf.toString());
            desc.setLineWrap(true);
            desc.setEditable(false);
            desc.setWrapStyleWord(true);
            desc.setBackground(Administrator.BACKGROUND_COLOR);
            JPanel pane = new JPanel(new BorderLayout());
            pane.add(desc, BorderLayout.NORTH);
            return pane;
        }
    }

}