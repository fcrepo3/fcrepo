/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast.request;

import fedora.server.journal.JournalException;
import fedora.server.journal.readerwriter.multicast.Transport;

/**
 * TransportRequest that asks the Transports to close the current file.
 */
public class CloseFileRequest
        extends TransportRequest {

    @Override
    public void performRequest(Transport transport) throws JournalException {
        transport.closeFile();
    }
}