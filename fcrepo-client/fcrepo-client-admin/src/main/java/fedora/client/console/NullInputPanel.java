/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

import javax.swing.JLabel;

/**
 * For use when unrecognized type.
 * 
 * @author Chris Wilper
 */
public class NullInputPanel
        extends InputPanel {

    private static final long serialVersionUID = 1L;

    private static NullInputPanel s_instance = new NullInputPanel();

    protected NullInputPanel() {
        add(new JLabel("Unrecognized type, using null"));
    }

    public static NullInputPanel getInstance() {
        return s_instance;
    }

    @Override
    public Object getValue() {
        return null;
    }

}
