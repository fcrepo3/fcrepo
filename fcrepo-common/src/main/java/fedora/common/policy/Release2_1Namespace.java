/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Vector;

import fedora.common.Constants;

public class Release2_1Namespace
        extends XacmlNamespace {

    private Release2_1Namespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
    }

    public static Release2_1Namespace onlyInstance =
            new Release2_1Namespace(FedoraAsProjectNamespace.getInstance(),
                                    "2.1");
    static {
        onlyInstance.addNamespace(SubjectNamespace.getInstance());
        onlyInstance.addNamespace(ActionNamespace.getInstance());
        onlyInstance.addNamespace(ResourceNamespace.getInstance());
        onlyInstance.addNamespace(EnvironmentNamespace.getInstance());
    }

    public static final Release2_1Namespace getInstance() {
        return onlyInstance;
    }

    public static final void main(String[] args) throws FileNotFoundException {
        PrintStream printer = System.out;
        if (args.length == 1)
        {
            try {
                printer = new PrintStream(new FileOutputStream(args[0]), true);
            } catch (FileNotFoundException e) {
                System.err.println("Error creating/opening file: " + args[0]
                        + ", " + e.getMessage());
                throw e;
            }
        }

        Release2_1Namespace instance = Release2_1Namespace.getInstance();
        Vector list = new Vector();
        instance.flatRep(list);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof XacmlName) {
                if (!((XacmlName) list.get(i)).toString()
                        .startsWith(Constants.ACTION.CONTEXT_ID.uri)) {
                    printer.println(list.get(i));
                }

            }
        }
    }

}
