/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast.rmi;

import java.io.BufferedWriter;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import java.util.Date;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import javanet.staxutils.IndentingXMLEventWriter;

import fedora.server.journal.JournalException;
import fedora.server.journal.readerwriter.multicast.Transport;
import fedora.server.journal.readerwriter.multicast.TransportParent;

/**
 * <p>
 * RmiTransport.java
 * </p>
 * <p>
 * Writes Journal files to an {@link RmiJournalReceiver}, either locally or on
 * another machine. Requires parameters for the names of the receiving host and
 * the receiving RMI service. Also accepts optional parameters for the RMI port
 * number and the internal message buffer size.
 * </p>
 *
 * @author jblake
 * @version $Id: RmiTransport.java,v 1.3 2007/06/01 17:21:32 jblake Exp $
 */
public class RmiTransport
        extends Transport {

    public static final String PARAMETER_HOST_NAME = "hostName";

    public static final String PARAMETER_PORT_NUMBER = "port";

    public static final String PARAMETER_SERVICE_NAME = "service";

    public static final String PARAMETER_BUFFER_SIZE = "bufferSize";

    public static final int DEFAULT_PORT_NUMBER = 1099;

    public static final int DEFAULT_BUFFER_SIZE = 100000;

    private final int bufferSize;

    private final RmiJournalReceiverInterface receiver;

    private RmiTransportWriter writer;

    private XMLEventWriter xmlWriter;

    public RmiTransport(Map<String, String> parameters,
                        boolean crucial,
                        TransportParent parent)
            throws JournalException {
        super(parameters, crucial, parent);
        String host = parseHost(parameters);
        String port = parsePort(parameters);
        String serviceName = parseServiceName(parameters);
        bufferSize = parseBufferSize(parameters);

        String serverName = "//" + host + ":" + port;
        String nameString = serverName + "/" + serviceName;

        try {
            receiver = (RmiJournalReceiverInterface) Naming.lookup(nameString);
        } catch (MalformedURLException e) {
            throw new JournalException("Problem finding RMI registry", e);
        } catch (RemoteException e) {
            throw new JournalException("Problem contacting RMI registry", e);
        } catch (NotBoundException e) {
            throw new JournalException("'" + serviceName
                    + "' not registered at '" + serverName + "'", e);
        }
    }

    private String parseHost(Map<String, String> parameters)
            throws JournalException {
        String host = getRequiredParameter(parameters, PARAMETER_HOST_NAME);
        try {
            InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new JournalException("Invalid '" + PARAMETER_HOST_NAME
                    + "' parameter: " + host, e);
        }
        return host;
    }

    private String parsePort(Map<String, String> parameters)
            throws JournalException {
        if (parameters.containsKey(PARAMETER_PORT_NUMBER)) {
            String port = parameters.get(PARAMETER_PORT_NUMBER);
            try {
                Integer.parseInt(port);
                return port;
            } catch (NumberFormatException e) {
                throw new JournalException("Invalid '" + PARAMETER_PORT_NUMBER
                        + "' parameter: " + port, e);
            }
        } else {
            return String.valueOf(DEFAULT_PORT_NUMBER);
        }
    }

    private String parseServiceName(Map<String, String> parameters)
            throws JournalException {
        return getRequiredParameter(parameters, PARAMETER_SERVICE_NAME);
    }

    private int parseBufferSize(Map<String, String> parameters)
            throws JournalException {
        if (parameters.containsKey(PARAMETER_BUFFER_SIZE)) {
            String size = parameters.get(PARAMETER_BUFFER_SIZE);
            try {
                return Integer.parseInt(size);
            } catch (NumberFormatException e) {
                throw new JournalException("Invalid '" + PARAMETER_BUFFER_SIZE
                        + "' parameter: " + size, e);
            }
        } else {
            return DEFAULT_BUFFER_SIZE;
        }
    }

    private String getRequiredParameter(Map<String, String> parameters,
                                        String parameter)
            throws JournalException {
        if (!parameters.containsKey(parameter)) {
            throw new JournalException("RmiTransport requires '" + parameter
                    + "' parameter.");
        }
        return parameters.get(parameter);
    }

    /**
     * check state, send the open request, open a very special writer, write the
     * file opening, set state
     */
    @Override
    public void openFile(String repositoryHash,
                         String filename,
                         Date currentDate) throws JournalException {
        try {
            super.testStateChange(State.FILE_OPEN);

            writer = new RmiTransportWriter(receiver, repositoryHash, filename);
            xmlWriter =
                    new IndentingXMLEventWriter(XMLOutputFactory
                            .newInstance()
                            .createXMLEventWriter(new BufferedWriter(writer,
                                                                     bufferSize)));

            parent.writeDocumentHeader(xmlWriter, repositoryHash, currentDate);

            super.setState(State.FILE_OPEN);
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        } catch (FactoryConfigurationError e) {
            throw new JournalException(e);
        }
    }

    /**
     * check state, hand over to the writer
     */
    @Override
    public XMLEventWriter getWriter() throws JournalException {
        super.testWriterState();
        return xmlWriter;
    }

    /**
     * check state, write the file closing, close/flush the writer, send the
     * close request, set state.
     */
    @Override
    public void closeFile() throws JournalException {
        try {
            super.testStateChange(State.FILE_CLOSED);
            parent.writeDocumentTrailer(xmlWriter);
            xmlWriter.close();
            super.setState(State.FILE_CLOSED);
        } catch (XMLStreamException e) {
            throw new JournalException(e);
        }
    }

    /**
     * check state, set state. a redundant call is not an error, but requires no
     * action.
     */
    @Override
    public void shutdown() throws JournalException {
        super.testStateChange(State.SHUTDOWN);
        if (super.getState() != State.SHUTDOWN) {
            super.setState(State.SHUTDOWN);
        }
    }

}
