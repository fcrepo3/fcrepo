/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.messaging;

import fedora.server.errors.MessagingException;

/**
 * A client used to receive messages. 
 * 
 * @author Bill Branan
 */
public interface MessagingClient {
    
    /**
     * Starts the client, begins listening for messages
     */
    public void start() throws MessagingException;
    
    /**
     * Stops the client, stops listening for messages, and closes 
     * down connections
     * 
     * @param unsubscribe - set to true in order to remove all durable
     *                      subscriptions
     */
    public void stop(boolean unsubscribe)throws MessagingException;
}