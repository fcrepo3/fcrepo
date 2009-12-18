/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

/**
 * @author Chris Wilper
 */
public abstract class InputPanelFactory {

    public static InputPanel getPanel(Class cl) {
        if (cl.getName().equals("java.lang.String")) {
            return new StringInputPanel();
        }
        if (cl.getName().equals("[B")) {
            return new ByteArrayInputPanel(true);
        }
        if (cl.getName().equals("boolean")) {
            return new BooleanInputPanel(true);
        }
        if (cl.getName().equals("java.lang.Boolean")) {
            return new BooleanInputPanel(false);
        }
        if (cl.getName().equals("java.util.Date")) {
            return new DateTimeInputPanel();
        }
        if (cl.getName().equals("org.apache.axis.types.NonNegativeInteger")) {
            return new NonNegativeIntegerInputPanel();
        }
        if (cl.getName().startsWith("[L")) {
            try {
                return new ArrayInputPanel(Class.forName(cl.getName()
                        .substring(2, cl.getName().length() - 1)));
            } catch (ClassNotFoundException cnfe) {
                // will fall through as unrecognized
            }
        }
        System.out.println("Unrecognized type: " + cl.getName());
        return NullInputPanel.getInstance();
    }

}
