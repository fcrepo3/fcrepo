/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

import java.lang.reflect.Method;

import javax.wsdl.PortType;

/**
 * @author Chris Wilper
 */
public class ServiceConsoleCommandFactory {

    public static ConsoleCommand[] getConsoleCommands(Class javaInterface,
                                                      PortType wsdlInterface) {
        if (!javaInterface.isInterface()) {
            return null;
        }
        Method[] methods = javaInterface.getDeclaredMethods();
        ConsoleCommand[] commands = new ConsoleCommand[methods.length];
        for (int i = 0; i < methods.length; i++) {
            commands[i] =
                    new ConsoleCommand(methods[i], null, null, null, null);
        }
        return commands;
    }

}
