/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast;

import java.util.Date;
import java.util.Map;

import fedora.server.journal.JournalException;
import fedora.server.journal.ServerInterface;

public class MockMulticastJournalWriter
        extends MulticastJournalWriter {

    private boolean checkParametersForValidity = true;

    private Date currentDate;

    public MockMulticastJournalWriter(Map<String, String> parameters,
                                      String role,
                                      ServerInterface server)
            throws JournalException {
        super(parameters, role, server);
    }

    public void setCheckParametersForValidity(boolean checkParametersForValidity) {
        this.checkParametersForValidity = checkParametersForValidity;
    }

    public void setCurrentDate(Date currentDate) {
        this.currentDate = currentDate;
    }

    /**
     * In unit tests, we'll want to know what the "current date" is, so we know
     * what filename to expect.
     */
    @Override
    protected Date getCurrentDate() {
        if (currentDate == null) {
            return super.getCurrentDate();
        } else {
            return currentDate;
        }
    }

    /**
     * We can choose not to check for valid parameters, if we are just running
     * tests on the Size Estimator or on a Transport.
     */
    @Override
    protected void checkTransportParametersForValidity()
            throws JournalException {
        if (checkParametersForValidity) {
            super.checkTransportParametersForValidity();
        }
    }

}
