/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

import javax.swing.JPanel;

/**
 * @author Chris Wilper
 */
public abstract class InputPanel
        extends JPanel {

    public abstract Object getValue();

}
