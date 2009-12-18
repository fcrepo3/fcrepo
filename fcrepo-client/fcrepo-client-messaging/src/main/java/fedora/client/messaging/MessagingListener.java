/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.messaging;

import javax.jms.Message;

/**
 * A MessagingListener is used to receive asynchronously 
 * delivered notifications.
 *
 * @author Bill Branan
 */
public interface MessagingListener {
    
    /**
     * Processes a message.
     * 
     * @param clientId - the identifier of the messaging client from 
     *                   which the message came
     * @param message - the message to be processed
     */
    public void onMessage(String clientId, Message message);
    
}
