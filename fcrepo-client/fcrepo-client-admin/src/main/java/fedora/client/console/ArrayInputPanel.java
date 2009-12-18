/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

import java.util.ArrayList;

import javax.swing.JLabel;

/**
 * @author Chris Wilper
 */
public class ArrayInputPanel
        extends InputPanel {

    private static final long serialVersionUID = 1L;

    private final ArrayList m_inputPanels;

    public ArrayInputPanel(Class cl) {
        m_inputPanels = new ArrayList();
        add(new JLabel("Array handler not implemented, will be null."));
    }

    @Override
    public Object getValue() {
        Object[] out = null;
        if (m_inputPanels.size() > 0) {
            out = new Object[m_inputPanels.size()];
        }
        return out;
    }

}
