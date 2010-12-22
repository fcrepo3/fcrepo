/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.journal.readerwriter.multicast;

import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import javanet.staxutils.IndentingXMLEventWriter;

import org.junit.Before;
import org.junit.Test;

import org.fcrepo.server.journal.JournalException;
import org.fcrepo.server.journal.MockServerForJournalTesting;
import org.fcrepo.server.journal.ServerInterface;
import org.fcrepo.server.journal.entry.CreatorJournalEntry;
import org.fcrepo.server.journal.readerwriter.multicast.JournalEntrySizeEstimator;
import org.fcrepo.server.management.MockManagementDelegate;


import static org.junit.Assert.fail;

/**
 * <p>
 * <b>Title:</b> TestJournalEntrySizeEstimator.java
 * </p>
 * <p>
 * <b>Description:</b> Unit tests for JournalEntrySizeEstimator
 * </p>
 *
 * @author jblake
 * @version $Id: TestJournalEntrySizeEstimator.java,v 1.1 2007/02/27 20:23:48
 *          jblake Exp $
 */
public class TestJournalEntrySizeEstimator {

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestJournalEntrySizeEstimator.class);
    }

    private final Map<String, String> parameters =
            new HashMap<String, String>();

    private final String role = "dummyRole";

    private final ServerInterface server =
            new MockServerForJournalTesting(new MockManagementDelegate(),
                                            "repositoryHash");

    private List<CreatorJournalEntry> journalEntries;

    @Before
    public void createListOfJournalEntries() throws JournalException {
        journalEntries =
                new ArrayList<CreatorJournalEntry>(SampleJournalEntries.ALL_ENTRIES);
    }

    @Test
    public void testSizeEstimation() throws JournalException,
            XMLStreamException, FactoryConfigurationError {
        MockMulticastJournalWriter mjw =
                new MockMulticastJournalWriter(parameters, role, server);
        mjw.setCheckParametersForValidity(false);

        JournalEntrySizeEstimator estimator =
                new JournalEntrySizeEstimator(mjw);

        for (CreatorJournalEntry journalEntry : journalEntries) {
            StringWriter stringWriter = new StringWriter();
            XMLEventWriter xmlEventWriter =
                    new IndentingXMLEventWriter(XMLOutputFactory.newInstance()
                            .createXMLEventWriter(stringWriter));
            mjw.writeJournalEntry(journalEntry, xmlEventWriter);
            long estimatedSize = estimator.estimateSize(journalEntry);
            assertSizesAreReallyClose(stringWriter.getBuffer().length(),
                                      estimatedSize);
        }
    }

    private void assertSizesAreReallyClose(int actualSize, long estimatedSize) {
        long difference = Math.abs(actualSize - estimatedSize);
        if (difference / actualSize > 0.001) {
            fail("sizes aren't even close: actual=" + actualSize
                    + ", estimated=" + estimatedSize);
        }
    }

}
