/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast;

import java.util.Date;

import javax.xml.stream.XMLEventWriter;

import fedora.server.journal.JournalException;

/**
 * <p>
 * <b>Title:</b> Transport.java
 * </p>
 * <p>
 * <b>Description:</b> Allows a Transport to request formatting operations from
 * its parent object.
 * </p>
 *
 * @author jblake
 * @version $Id: TransportParent.java,v 1.3 2007/06/01 17:21:31 jblake Exp $
 */
public interface TransportParent {

    void writeDocumentHeader(XMLEventWriter writer,
                             String repositoryHash,
                             Date currentDate) throws JournalException;

    void writeDocumentTrailer(XMLEventWriter xmlWriter) throws JournalException;
}
