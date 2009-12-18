/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

import javax.swing.JCheckBox;

/**
 * @author Chris Wilper
 */
public class BooleanInputPanel
        extends InputPanel {

    private static final long serialVersionUID = 1L;

    private final JCheckBox m_checkBox;

    public BooleanInputPanel(boolean primitive) {
        m_checkBox = new JCheckBox();
        m_checkBox.setSelected(false);
        add(m_checkBox);
    }

    @Override
    public Object getValue() {
        return new Boolean(m_checkBox.isSelected());
    }

}
