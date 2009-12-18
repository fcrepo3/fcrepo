/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.stream.XMLEventWriter;

import org.apache.log4j.Logger;

import fedora.server.errors.ServerException;
import fedora.server.journal.JournalException;
import fedora.server.journal.JournalOperatingMode;
import fedora.server.journal.JournalWriter;
import fedora.server.journal.ServerInterface;
import fedora.server.journal.entry.CreatorJournalEntry;
import fedora.server.journal.helpers.JournalHelper;
import fedora.server.journal.helpers.ParameterHelper;
import fedora.server.journal.readerwriter.multicast.request.CloseFileRequest;
import fedora.server.journal.readerwriter.multicast.request.OpenFileRequest;
import fedora.server.journal.readerwriter.multicast.request.ShutdownRequest;
import fedora.server.journal.readerwriter.multicast.request.TransportRequest;
import fedora.server.journal.readerwriter.multicast.request.WriteEntryRequest;

import static fedora.server.journal.readerwriter.multicast.Transport.State.FILE_CLOSED;
import static fedora.server.journal.readerwriter.multicast.Transport.State.FILE_OPEN;
import static fedora.server.journal.readerwriter.multicast.Transport.State.SHUTDOWN;

/**
 * SYNCHRONIZATION NOTE: All public methods are synchronized against
 * {@link JournalWriter.SYNCHRONIZER}, as is the {@link #closeFile() closeFile}
 * method. This means that an asynchronous call by the timer task will not
 * interrupt a synchronous operation already in progress, or vice versa.
 *
 * @author jblake
 */
public class MulticastJournalWriter
        extends JournalWriter
        implements TransportParent {

    private static final Logger LOG =
            Logger.getLogger(MulticastJournalWriter.class);

    /**
     * prefix that indicates a transport parameter - must include the separator
     * character, if one is expected.
     */
    public static final String TRANSPORT_PARAMETER_PREFIX = "transport.";

    /**
     * Required parameter for each transport: the full name of the class that
     * implements the transport.
     */
    public static final String CLASSNAME_PARAMETER_KEY = "classname";

    /**
     * Required parameter for each transport, and must be set to "true" on at
     * least one transport.
     */
    public static final String CRUCIAL_PARAMETER_KEY = "crucial";

    /**
     * Every Transport needs these types of arguments for its constructor.
     */
    private static final Class<?>[] TRANSPORT_CONSTRUCTOR_ARGUMENT_TYPES =
            new Class<?>[] {Map.class, Boolean.TYPE, TransportParent.class};

    /** Journal file names will start with this string. */
    private final String filenamePrefix;

    /** Number of bytes before we start a new file - 0 means no limit */
    private final long sizeLimit;

    /** Number of milliseconds before we start a new file - 0 means no limit */
    private final long ageLimit;

    /** Nested map of parameters, keyed by transport name. */
    private final Map<String, Map<String, String>> transportParameters;

    /** Map of the transports, keyed by transport name. */
    private final Map<String, Transport> transports;

    /** Current state of the writer and the transports. */
    private Transport.State state = FILE_CLOSED;

    /** Approximately how many bytes have been written to the current file? */
    private long currentSize;

    /** A tool to estimate the output size of a JournalEntry. */
    private final JournalEntrySizeEstimator sizeEstimator;

    /** A timer to monitors the age of the current file. */
    private Timer timer;

    public MulticastJournalWriter(Map<String, String> parameters,
                                  String role,
                                  ServerInterface server)
            throws JournalException {
        super(parameters, role, server);

        filenamePrefix =
                ParameterHelper.parseParametersForFilenamePrefix(parameters);
        sizeLimit = ParameterHelper.parseParametersForSizeLimit(parameters);
        ageLimit = ParameterHelper.parseParametersForAgeLimit(parameters);

        transportParameters = parseTransportParameters(parameters);
        checkTransportParametersForValidity();
        transports = createTransports();

        sizeEstimator = new JournalEntrySizeEstimator(this);
    }

    /**
     * Create a Map of Maps, holding parameters for all of the transports.
     *
     * @throws JournalException
     */
    private Map<String, Map<String, String>> parseTransportParameters(Map<String, String> parameters)
            throws JournalException {
        Map<String, Map<String, String>> allTransports =
                new LinkedHashMap<String, Map<String, String>>();
        for (String key : parameters.keySet()) {
            if (isTransportParameter(key)) {
                Map<String, String> thisTransport =
                        getThisTransportMap(allTransports,
                                            getTransportName(key));
                thisTransport.put(getTransportParameterName(key), parameters
                        .get(key));
            }
        }
        return allTransports;
    }

    private boolean isTransportParameter(String key) throws JournalException {
        return key.startsWith(TRANSPORT_PARAMETER_PREFIX);
    }

    private int findParameterNameSeparator(String key) throws JournalException {
        int dotHere = key.indexOf('.', TRANSPORT_PARAMETER_PREFIX.length());
        if (dotHere < 0) {
            throw new JournalException("Invalid name for transport parameter '"
                    + key + "' - requires '.' after transport name.");
        }
        return dotHere;
    }

    private String getTransportParameterName(String key)
            throws JournalException {
        return key.substring(findParameterNameSeparator(key) + 1);
    }

    private String getTransportName(String key) throws JournalException {
        return key.substring(TRANSPORT_PARAMETER_PREFIX.length(),
                             findParameterNameSeparator(key));
    }

    /** If we don't yet have a map for this transport name, create one. */
    private Map<String, String> getThisTransportMap(Map<String, Map<String, String>> allTransports,
                                                    String transportName) {
        if (!allTransports.containsKey(transportName)) {
            allTransports.put(transportName, new HashMap<String, String>());
        }
        return allTransports.get(transportName);
    }

    /** "protected" so we can mock it out in unit tests. */
    protected void checkTransportParametersForValidity()
            throws JournalException {
        checkAtLeastOneTransport();
        checkAllTransportsHaveClassnames();
        checkAllTransportsHaveCrucialFlags();
        checkAtLeastOneCrucialTransport();
        LOG.info("Journal transport parameters validated.");
    }

    private void checkAtLeastOneTransport() throws JournalException {
        if (transportParameters.size() == 0) {
            throw new JournalException("MulticastJournalWriter must have "
                    + "at least one Transport.");
        }
    }

    private void checkAllTransportsHaveClassnames() throws JournalException {
        for (String transportName : transportParameters.keySet()) {
            Map<String, String> thisTransportMap =
                    transportParameters.get(transportName);
            if (!thisTransportMap.containsKey(CLASSNAME_PARAMETER_KEY)) {
                throw new JournalException("Transport '" + transportName
                        + "' does not have a '" + CLASSNAME_PARAMETER_KEY
                        + "' parameter");
            }
        }
    }

    private void checkAllTransportsHaveCrucialFlags() throws JournalException {
        for (String transportName : transportParameters.keySet()) {
            Map<String, String> thisTransportMap =
                    transportParameters.get(transportName);
            if (!thisTransportMap.containsKey(CRUCIAL_PARAMETER_KEY)) {
                throw new JournalException("Transport '" + transportName
                        + "' does not have a '" + CRUCIAL_PARAMETER_KEY
                        + "' parameter");
            }
        }
    }

    private void checkAtLeastOneCrucialTransport() throws JournalException {
        for (String transportName : transportParameters.keySet()) {
            Map<String, String> thisTransportMap =
                    transportParameters.get(transportName);
            String crucialString = thisTransportMap.get(CRUCIAL_PARAMETER_KEY);
            if (Boolean.parseBoolean(crucialString)) {
                return;
            }
        }
        throw new JournalException("There must be at least one crucial transport.");
    }

    private Map<String, Transport> createTransports() throws JournalException {
        Map<String, Transport> result = new HashMap<String, Transport>();
        for (String transportName : transportParameters.keySet()) {
            Map<String, String> thisTransportMap =
                    transportParameters.get(transportName);
            String className = thisTransportMap.get(CLASSNAME_PARAMETER_KEY);
            boolean crucialFlag =
                    Boolean.parseBoolean(thisTransportMap
                            .get(CRUCIAL_PARAMETER_KEY));

            Object transport =
                    JournalHelper
                            .createInstanceFromClassname(className,
                                                         TRANSPORT_CONSTRUCTOR_ARGUMENT_TYPES,
                                                         new Object[] {
                                                                 thisTransportMap,
                                                                 crucialFlag,
                                                                 this});
            LOG.info("Transport '" + transportName + "' is " + transport);
            result.put(transportName, (Transport) transport);
        }
        return result;
    }

    Map<String, Transport> getTransports() {
        return transports;
    }

    /**
     * <p>
     * Get ready to write a journal entry, insuring that we have an open file.
     * </p>
     * <p>
     * If we are shutdown, ignore this request. Otherwise, check if we need to
     * shut a file down based on size limit. Then check to see whether we need
     * to open another file. If so, we'll need a repository hash and a filename.
     * </p>
     *
     * @see fedora.server.journal.JournalWriter#prepareToWriteJournalEntry()
     */
    @Override
    public void prepareToWriteJournalEntry() throws JournalException {
        synchronized (JournalWriter.SYNCHRONIZER) {
            if (state == SHUTDOWN) {
                return;
            }

            LOG.debug("Preparing to write journal entry.");

            if (state == FILE_OPEN) {
                closeFileIfAppropriate();
            }

            if (state == FILE_CLOSED) {
                openNewFile();
            }
        }
    }

    /**
     * <p>
     * Write a journal entry.
     * </p>
     * <p>
     * If we are shutdown, ignore this request. Otherwise, get an output stream
     * from each Transport in turn, and write the entry. If this puts the file
     * size over the limit, close them.
     * </p>
     *
     * @see fedora.server.journal.JournalWriter#writeJournalEntry(fedora.server.journal.entry.CreatorJournalEntry)
     */
    @Override
    public void writeJournalEntry(CreatorJournalEntry journalEntry)
            throws JournalException {
        synchronized (JournalWriter.SYNCHRONIZER) {
            if (state == SHUTDOWN) {
                return;
            }
            LOG.debug("Writing journal entry.");
            sendRequestToAllTransports(new WriteEntryRequest(this, journalEntry));
            currentSize += sizeEstimator.estimateSize(journalEntry);

            if (state == FILE_OPEN) {
                closeFileIfAppropriate();
            }
        }
    }

    /**
     * <p>
     * Shut it down
     * </p>
     * <p>
     * If the Transports still have files open, close them. Then stop responding
     * to requests.
     * </p>
     *
     * @see fedora.server.journal.JournalWriter#shutdown()
     */
    @Override
    public void shutdown() throws JournalException {
        synchronized (JournalWriter.SYNCHRONIZER) {
            if (state == SHUTDOWN) {
                return;
            }
            if (state == FILE_OPEN) {
                closeFile();
            }

            LOG.debug("Shutting down.");
            sendRequestToAllTransports(new ShutdownRequest());
            state = SHUTDOWN;
        }
    }

    private void openNewFile() throws JournalException {
        try {
            String hash = server.getRepositoryHash();
            String filename =
                    JournalHelper.createTimestampedFilename(filenamePrefix,
                                                            getCurrentDate());
            timer = createTimer();
            sendRequestToAllTransports(new OpenFileRequest(hash,
                                                           filename,
                                                           getCurrentDate()));
            currentSize = 0;
            state = FILE_OPEN;
        } catch (ServerException e) {
            throw new JournalException(e);
        }
    }

    /** protected, so it can be mocked out for unit testing. */
    protected Date getCurrentDate() {
        return new Date();
    }

    /**
     * Create the timer, and schedule a task that will let us know when the file
     * is too old to continue. If the age limit is 0 or negative, we treat it as
     * "no limit".
     */
    private Timer createTimer() {
        Timer fileTimer = new Timer();

        // if the age limit is 0 or negative, treat it as "no limit".
        if (ageLimit >= 0) {
            fileTimer.schedule(new CloseFileTimerTask(), ageLimit);
        }

        return fileTimer;
    }

    /**
     * When the timer goes off, close the file.
     */
    private final class CloseFileTimerTask
            extends TimerTask {

        @Override
        public void run() {
            try {
                LOG.debug("Timer task requests file close.");
                closeFile();
            } catch (JournalException e) {
                /*
                 * What to do with this exception? If we print it, where is the
                 * console? If we throw it, who will catch it?
                 */
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Check to see whether the file size has passed the limit.
     */
    private void closeFileIfAppropriate() throws JournalException {
        if (sizeLimit != 0 && currentSize >= sizeLimit) {
            closeFile();
        }
    }

    /**
     * Close the file unconditionally. Called if
     * <ul>
     * <li>the file passes the size limit,</li>
     * <li>the timer expires,</li>
     * <li>the server commands a shutdown</li>
     * </ul>
     * Synchronized so a close request from the timer doesn't conflict with
     * other processing.
     */
    private void closeFile() throws JournalException {
        synchronized (JournalWriter.SYNCHRONIZER) {
            // check to be sure that another thread didn't close the file while
            // we were waiting for the lock.
            if (state == FILE_OPEN) {
                sendRequestToAllTransports(new CloseFileRequest());
                currentSize = 0;
                state = FILE_CLOSED;
            }

            // turn off the timer that is checking the age of this file.
            if (timer != null) {
                timer.cancel();
            }
        }
    }

    /** make this public, so the TransportRequest class can call it. */
    @Override
    public void writeJournalEntry(CreatorJournalEntry journalEntry,
                                  XMLEventWriter writer)
            throws JournalException {
        super.writeJournalEntry(journalEntry, writer);
    }

    /**
     * make this public so the Transport classes can call it via
     * TransportParent.
     */
    @Override
    public void writeDocumentHeader(XMLEventWriter writer,
                                    String repositoryHash,
                                    Date currentDate) throws JournalException {
        super.writeDocumentHeader(writer, repositoryHash, currentDate);
    }

    /**
     * make this public so the Transport classes can call it via
     * TransportParent.
     */
    @Override
    public void writeDocumentTrailer(XMLEventWriter writer)
            throws JournalException {
        super.writeDocumentTrailer(writer);
    }

    /**
     * Send a request for some operation to the Transports. Send it to all of
     * them, even if one or more throws an Exception. Report any exceptions when
     * all Transports have been attempted.
     *
     * @param request
     *        the request object
     * @param args
     *        the arguments to be passed to the request object
     * @throws JournalException
     *         if there were any crucial problems.
     */
    private void sendRequestToAllTransports(TransportRequest request)
            throws JournalException {
        Map<String, JournalException> crucialExceptions =
                new LinkedHashMap<String, JournalException>();
        Map<String, JournalException> nonCrucialExceptions =
                new LinkedHashMap<String, JournalException>();

        /*
         * Send the request to all transports, accumulating any Exceptions as we
         * go. That way, we increase the likeihood that at least one Transport
         * succeeded in the request.
         */
        for (String transportName : transports.keySet()) {
            Transport transport = transports.get(transportName);
            try {
                LOG.debug("Sending " + request.getClass().getSimpleName()
                        + " to transport '" + transportName + "'");
                request.performRequest(transport);
            } catch (JournalException e) {
                if (transport.isCrucial()) {
                    crucialExceptions.put(transportName, e);
                } else {
                    nonCrucialExceptions.put(transportName, e);
                }
            }
        }

        /*
         * Report the Exceptions. Report the non-crucial ones first, in case the
         * Server decides to take some definitive action on a crucial Exception.
         */
        reportNonCrucialExceptions(nonCrucialExceptions);
        reportCrucialExceptions(crucialExceptions);
    }

    private void reportNonCrucialExceptions(Map<String, JournalException> nonCrucialExceptions) {
        if (nonCrucialExceptions.isEmpty()) {
            return;
        }
        for (String transportName : nonCrucialExceptions.keySet()) {
            JournalException e = nonCrucialExceptions.get(transportName);
            LOG.error("Exception thrown from non-crucial Journal Transport: '"
                    + transportName + "'", e);
        }
    }

    private void reportCrucialExceptions(Map<String, JournalException> crucialExceptions)
            throws JournalException {
        if (!crucialExceptions.isEmpty()) {
            JournalOperatingMode.setMode(JournalOperatingMode.READ_ONLY);
        }
        for (String transportName : crucialExceptions.keySet()) {
            JournalException e = crucialExceptions.get(transportName);
            LOG.fatal("Exception thrown from crucial Journal Transport: '"
                    + transportName + "'", e);
        }
    }

}
