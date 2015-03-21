/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.search;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.JUnit4TestAdapter;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.ConnectionPool;
import org.fcrepo.server.storage.MockDOReader;
import org.fcrepo.server.storage.MockRepositoryReader;
import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.server.utilities.SQLUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "org.apache.xerces.*", "javax.xml.*",
    "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({SQLUtility.class})
public class TestFieldSearchSQLImpl {
    private static final String[] SHORT_FIELDS = FieldSearchSQLImpl.DB_COLUMN_NAMES_NODC;

    private static final String[] LONG_FIELDS = FieldSearchSQLImpl.DB_COLUMN_NAMES;

    private static final String DC_PAYLOAD_NO_DATES = "<oai_dc:dc "
            + "    xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" "
            + "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
            + "  <dc:title>Sandy's Reference Object</dc:title>\n"
            + "  <dc:creator>Sandy Payette</dc:creator>\n"
            + "  <dc:subject>FOXML Testing</dc:subject>\n"
            + "  <dc:description>Object depicts all types of datastreams</dc:description>\n"
            + "  <dc:publisher>Cornell CIS</dc:publisher>\n"
            + "  <dc:identifier>test:100</dc:identifier>\n" + "</oai_dc:dc>\n";

    private static final String DC_PAYLOAD_WITH_DATES = "<oai_dc:dc "
            + "    xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" "
            + "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
            + "  <dc:title>Sandy's Reference Object</dc:title>\n"
            + "  <dc:creator>Sandy Payette</dc:creator>\n"
            + "  <dc:subject>FOXML Testing</dc:subject>\n"
            + "  <dc:description>Object depicts all types of datastreams</dc:description>\n"
            + "  <dc:publisher>Cornell CIS</dc:publisher>\n"
            + "  <dc:identifier>test:100</dc:identifier>\n"
            + "  <dc:date>2006-10-15</dc:date>\n" + "</oai_dc:dc>\n";

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestFieldSearchSQLImpl.class);
    }

    private static final ObjectData OBJECT_WITH_NO_DC = new ObjectData(
            "somePid", "myLabel", "A", "theOwner", new Date(
                    12345), new Date(67890), new Date(0), null);

    private static final ObjectData OBJECT_WITH_DC = new ObjectData("somePid",
            "myLabel", "A", "theOwner", new Date(12345),
            new Date(67890), new Date(10000), DC_PAYLOAD_NO_DATES);

    private static final ObjectData OBJECT_WITH_DC_AND_DATES = new ObjectData(
            "somePid", "myLabel",  "A", "theOwner", new Date(
                    12345), new Date(67890), new Date(10000),
            DC_PAYLOAD_WITH_DATES);

    @Mock
    private MockRepositoryReader mockRepositoryReader;

    @Mock
    private ConnectionPool connectionPool;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockStmt;

    @Before
    public void setUp() throws Exception {
        mockStatic(SQLUtility.class);
        when(connectionPool.getReadWriteConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any(String.class))).thenReturn(mockStmt);
        //PowerMockito.doNothing().when(SQLUtility.class, "createNonExistingTables",any(ConnectionPool.class), any(InputStream.class));
    }

    @Test
    public void noDC() throws ServerException, SQLException {
        updateRecord(OBJECT_WITH_NO_DC, false);
        verify(mockConnection,times(0)).prepareStatement("DELETE FROM dcDates WHERE pid=?");
        verify(mockConnection, times(0)).prepareStatement("INSERT INTO dcDates (pid, dcDate) values (?, ?)");
        verifyStatic(times(1));
        SQLUtility.replaceInto(
                any(Connection.class), eq("doFields"), aryEq(SHORT_FIELDS), aryEq(OBJECT_WITH_NO_DC.getShortFieldValueList().toArray(new String[]{})), eq("pid"), any(boolean[].class));
    }

    @Test
    public void dcNoDatesShortFields() throws ServerException, SQLException {
        updateRecord(OBJECT_WITH_DC, false);
        verify(mockConnection,times(0)).prepareStatement("DELETE FROM dcDates WHERE pid=?");
        verify(mockConnection, times(0)).prepareStatement("INSERT INTO dcDates (pid, dcDate) values (?, ?)");
        verifyStatic(times(1));
        SQLUtility.replaceInto(
                any(Connection.class), eq("doFields"), aryEq(SHORT_FIELDS), aryEq(OBJECT_WITH_DC.getShortFieldValueList().toArray(new String[]{})), eq("pid"), any(boolean[].class));
    }

    @Test
    public void dcNoDatesLongFields() throws ServerException, SQLException {
        updateRecord(OBJECT_WITH_DC, true);
        verify(mockConnection,times(1)).prepareStatement("DELETE FROM dcDates WHERE pid=?");
        verify(mockConnection, times(0)).prepareStatement("INSERT INTO dcDates (pid, dcDate) values (?, ?)");
        verifyStatic(times(1));
        SQLUtility.replaceInto(
                any(Connection.class), eq("doFields"), aryEq(LONG_FIELDS), aryEq(OBJECT_WITH_DC.getLongFieldValueList().toArray(new String[]{})), eq("pid"), any(boolean[].class));
    }

    @Test
    public void dcDatesShortFields() throws ServerException, SQLException {
        updateRecord(OBJECT_WITH_DC_AND_DATES, false);
        verify(mockConnection,times(0)).prepareStatement("DELETE FROM dcDates WHERE pid=?");
        verify(mockConnection, times(0)).prepareStatement("INSERT INTO dcDates (pid, dcDate) values (?, ?)");
        verifyStatic(times(1));
        SQLUtility.replaceInto(
                any(Connection.class), eq("doFields"), aryEq(SHORT_FIELDS), aryEq(OBJECT_WITH_DC.getShortFieldValueList().toArray(new String[]{})), eq("pid"), any(boolean[].class));
    }

    @Test
    public void dcDatesLongFields() throws ServerException, SQLException {
        updateRecord(OBJECT_WITH_DC_AND_DATES, true);
        verify(mockConnection,times(1)).prepareStatement("DELETE FROM dcDates WHERE pid=?");
        verify(mockConnection, times(1)).prepareStatement("INSERT INTO dcDates (pid, dcDate) values (?, ?)");
        verifyStatic(times(1));
        SQLUtility.replaceInto(
                any(Connection.class), eq("doFields"), aryEq(LONG_FIELDS), aryEq(OBJECT_WITH_DC_AND_DATES.getLongFieldValueList().toArray(new String[]{})), eq("pid"), any(boolean[].class));
    }

    private void updateRecord(ObjectData objectData, boolean longFields)
            throws ServerException {
        // Create a DC datastream if appropriate.
        DatastreamXMLMetadata dcmd = null;
        if (objectData.getDcPayload() != null) {
            dcmd = new DatastreamXMLMetadata();
            dcmd.DatastreamID = "DC";
            dcmd.DSCreateDT = objectData.getDcModifiedDate();
            dcmd.xmlContent = objectData.getDcPayload().getBytes();
        }

        // Create the object and populate it.
        BasicDigitalObject theObject = new BasicDigitalObject();
        theObject.setPid(objectData.getPid());
        theObject.setLabel(objectData.getLabel());

        theObject.setState(objectData.getState());
        theObject.setOwnerId(objectData.getOwnerId());
        theObject.setCreateDate(objectData.getCreateDate());
        theObject.setLastModDate(objectData.getLastModDate());
        if (dcmd != null) {
            theObject.addDatastreamVersion(dcmd, false);
        }

        // Create the test instance.
        FieldSearchSQLImpl fssi = new FieldSearchSQLImpl(this.connectionPool,
                this.mockRepositoryReader, 50, 50, longFields);

        // And do the update.
        fssi.update(new MockDOReader(theObject));
    }


    private static class ObjectData {
        private final String pid;

        private final String label;

        private final String state;

        private final String ownerId;

        private final Date createDate;

        private final Date lastModDate;

        private final Date dcModifiedDate;

        private final String dcPayload;

        public ObjectData(String pid, String label,
                String state, String ownerId, Date createDate,
                Date lastModDate, Date dcModifiedDate, String dcPayload) {
            this.pid = pid;
            this.label = label;
            this.state = state;
            this.ownerId = ownerId;
            this.createDate = createDate;
            this.lastModDate = lastModDate;
            this.dcModifiedDate = dcModifiedDate;
            this.dcPayload = dcPayload;
        }

        public List<String> getShortFieldValueList() {
            List<String> result = new ArrayList<String>();
            result.add(pid);
            result.add(lowerCase(label));
            result.add(lowerCase(state));
            result.add(lowerCase(ownerId));
            result.add(dateStamp(createDate));
            result.add(dateStamp(lastModDate));
            result.add(dateStamp(dcModifiedDate));
            return result;
        }

        public List<String> getLongFieldValueList() {
            List<String> result = new ArrayList<String>();
            result.addAll(getShortFieldValueList());
            result.add(lowerCase(getDcFields("dc:title")));
            result.add(lowerCase(getDcFields("dc:creator")));
            result.add(lowerCase(getDcFields("dc:subject")));
            result.add(lowerCase(getDcFields("dc:description")));
            result.add(lowerCase(getDcFields("dc:publisher")));
            result.add(lowerCase(getDcFields("dc:contributor")));
            result.add(lowerCase(getDcFields("dc:date")));
            result.add(lowerCase(getDcFields("dc:type")));
            result.add(lowerCase(getDcFields("dc:format")));
            result.add(lowerCase(getDcFields("dc:identifier")));
            result.add(lowerCase(getDcFields("dc:source")));
            result.add(lowerCase(getDcFields("dc:language")));
            result.add(lowerCase(getDcFields("dc:relation")));
            result.add(lowerCase(getDcFields("dc:coverage")));
            result.add(lowerCase(getDcFields("dc:rights")));
            return result;
        }

        public String getPid() {
            return pid;
        }

        public String getLabel() {
            return label;
        }

        public String getState() {
            return state;
        }

        public String getOwnerId() {
            return ownerId;
        }

        public Date getCreateDate() {
            return createDate;
        }

        public Date getLastModDate() {
            return lastModDate;
        }

        public Date getDcModifiedDate() {
            return dcModifiedDate;
        }

        public String getDcPayload() {
            return dcPayload;
        }

        private String lowerCase(String raw) {
            return (raw == null) ? null : raw.toLowerCase();
        }

        private String dateStamp(Date date) {
            return (date == null) ? null : String.valueOf(date.getTime());
        }

        private String getDcFields(String fieldName) {
            String pString = String.format("<%1$s>\\s*([^<]*)\\s*</%1$s>",
                    fieldName);
            Pattern p = Pattern.compile(pString);
            Matcher m = p.matcher(dcPayload);

            List<String> values = new ArrayList<String>();
            int start = 0;
            while (m.find(start)) {
                values.add(m.group(1));
                start = m.end();
            }

            return joinStrings(values);
        }

        private String joinStrings(Collection<String> strings) {
            if ((strings == null) || (strings.isEmpty())) {
                return null;
            }
            StringBuffer result = new StringBuffer();
            for (String string : strings) {
                result.append(" ").append(string).append(" .");

            }
            return result.toString();
        }
    }
}
