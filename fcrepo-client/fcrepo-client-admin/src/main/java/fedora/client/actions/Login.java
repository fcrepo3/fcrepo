/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import fedora.client.Administrator;
import fedora.client.LoginDialog;

/**
 * Action for launching the login window.
 * 
 * @author Chris Wilper
 */
public class Login
        extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public Login() {
        super("Change Repository...");
    }

    public void actionPerformed(ActionEvent ae) {
        try {
            LoginDialog ld = new LoginDialog();
        } catch (Exception e) {
            Administrator.showErrorDialog(Administrator.getDesktop(),
                                          "Login Error",
                                          e.getClass().getName() + ": "
                                                  + e.getMessage(),
                                          e);
        }
    }

}
