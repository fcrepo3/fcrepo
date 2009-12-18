/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Chris Wilper
 */
public interface Console {

    /** Gets an object that fulfills the command. */
    public Object getInvocationTarget(ConsoleCommand cmd)
            throws InvocationTargetException;

    /** Sends the given text to the console. */
    public void print(String output);

    /** Clears the console. */
    public void clear();

    /** Tells the console whether it should look busy or not. */
    public void setBusy(boolean busy);

    /** Checks whether the console is busy. */
    public boolean isBusy();

}
