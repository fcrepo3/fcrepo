/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fedora.server.errors.ModuleInitializationException;
import fedora.server.journal.recoverylog.JournalRecoveryLog;

/**
 * <p>
 * Write recovery log messages to a {@link List} for examination in unit tests.
 * </p>
 * <p>
 * Since the instance is created dynamically by the {@link JournalConsumer},
 * the list of messsages must be static and accessible at the class level. The
 * list is set when the log is shut down. This means that the messages would be
 * lost if not read before the next instance is created and shut down, but that
 * should not pose a problem in unit tests.
 * </p>
 *
 * @author Jim Blake
 */
public class MockJournalRecoveryLog
        extends JournalRecoveryLog {

    // ----------------------------------------------------------------------
    // Mocking infrastructure
    // ----------------------------------------------------------------------

    private static List<String> messages = new ArrayList<String>();

    public static List<String> getMessages() {
        return new ArrayList<String>(messages);
    }

    public MockJournalRecoveryLog(Map<String, String> parameters,
                                  String role,
                                  ServerInterface server)
            throws ModuleInitializationException {
        super(parameters, role, server);
        messages.clear();
    }

    // ----------------------------------------------------------------------
    // Mocked methods
    // ----------------------------------------------------------------------

    @Override
    public void log(String message) {
        messages.add(message);
    }

    @Override
    public void shutdown() {
    }

    // ----------------------------------------------------------------------
    // Un-implemented methods
    // ----------------------------------------------------------------------

}
