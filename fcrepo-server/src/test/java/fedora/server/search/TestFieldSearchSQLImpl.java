/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.search;

import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Field;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;
import mock.sql.MockConnection;
import mock.sql.MockDriver;
import mock.sql.MockStatement;

import fedora.server.Context;
import fedora.server.config.DatastoreConfiguration;
import fedora.server.errors.InconsistentTableSpecException;
import fedora.server.errors.ServerException;
import fedora.server.storage.ConnectionPool;
import fedora.server.storage.MockDOReader;
import fedora.server.storage.MockRepositoryReader;
import fedora.server.storage.MockServiceDeploymentReader;
import fedora.server.storage.ServiceDeploymentReader;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DeploymentDSBindSpec;
import fedora.server.utilities.SQLUtility;
import fedora.server.utilities.TableCreatingConnection;
import fedora.server.utilities.TableSpec;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

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

    private static SQLUtility saveSqlUtility;

    @BeforeClass
    public static void saveSqlUtilityImpl() {
        saveSqlUtility = getSqlUtilityInstance();
    }

    @AfterClass
    public static void restoreSqlUtilityImpl() {
        setSqlUtilityInstance(saveSqlUtility);
    }

    private MockConnection mockConnection;

    private MockRepositoryReader mockRepositoryReader;

    private ConnectionPool connectionPool;

    private final MyMockDriver mockDriver = new MyMockDriver();

    private int expectedDateInserts;

    private int expectedDateDeletes;

    @Before
    public void registerMockDriver() {
        try {
            DriverManager.registerDriver(mockDriver);
        } catch (SQLException e) {
            fail("Failed to register mock JDBC driver: " + e);
        }
    }

    @After
    public void deregisterMockDriver() {
        try {
            DriverManager.deregisterDriver(mockDriver);
        } catch (SQLException e) {
            fail("Failed to deregister mock JDBC driver: " + e);
        }
    }

    @Before
    public void createConnectionPool() throws SQLException {
        // Create a connection pool that uses the Mock Driver and some other
        // plausible values.
        this.connectionPool = new ConnectionPool(MockDriver.class.getName(),
                "mock://bogus.url", "bogusUsername", "bogusPassword", 5, 5, 5,
                0, 0, 2, 300, null, false, false, false, (byte) 0);
    }

    @Before
    public void clearExpectedValues() {
        this.expectedDateInserts = 0;
        this.expectedDateDeletes = 0;
    }

    @Test
    public void noDC() throws ServerException {
        setSqlUtilityInstance(new UpdatingMockSqlUtility(SHORT_FIELDS,
                OBJECT_WITH_NO_DC.getShortFieldValueList()));
        this.mockConnection = new UnusedMockConnection();
        this.mockRepositoryReader = new UnusedMockRepositoryReader();

        updateRecord(OBJECT_WITH_NO_DC, false);
        checkExpectations();
    }

    @Test
    public void dcNoDatesShortFields() throws ServerException {
        setSqlUtilityInstance(new UpdatingMockSqlUtility(SHORT_FIELDS,
                OBJECT_WITH_DC.getShortFieldValueList()));
        this.mockConnection = new UnusedMockConnection();
        this.mockRepositoryReader = new UnusedMockRepositoryReader();

        updateRecord(OBJECT_WITH_DC, false);
        checkExpectations();
    }

    @Test
    public void dcNoDatesLongFields() throws ServerException {
        setSqlUtilityInstance(new UpdatingMockSqlUtility(LONG_FIELDS,
                OBJECT_WITH_DC.getLongFieldValueList()));
        this.mockConnection = new UpdatingMockConnection();
        this.expectedDateDeletes = 1;
        this.expectedDateInserts = 0;
        this.mockRepositoryReader = new UnusedMockRepositoryReader();

        updateRecord(OBJECT_WITH_DC, true);
        checkExpectations();
    }

    @Test
    public void dcDatesShortFields() throws ServerException {
        setSqlUtilityInstance(new UpdatingMockSqlUtility(SHORT_FIELDS,
                OBJECT_WITH_DC_AND_DATES.getShortFieldValueList()));
        this.mockConnection = new UnusedMockConnection();
        this.mockRepositoryReader = new UnusedMockRepositoryReader();

        updateRecord(OBJECT_WITH_DC_AND_DATES, false);
        checkExpectations();
    }

    @Test
    public void dcDatesLongFields() throws ServerException {
        setSqlUtilityInstance(new UpdatingMockSqlUtility(LONG_FIELDS,
                OBJECT_WITH_DC_AND_DATES.getLongFieldValueList()));
        this.mockConnection = new UpdatingMockConnection();
        this.expectedDateDeletes = 1;
        this.expectedDateInserts = 1;
        this.mockRepositoryReader = new UnusedMockRepositoryReader();

        updateRecord(OBJECT_WITH_DC_AND_DATES, true);
        checkExpectations();
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

    private void checkExpectations() {
        ((MockSqlUtility) getSqlUtilityInstance()).checkExpectations();

        if (mockConnection instanceof UpdatingMockConnection) {
            ((UpdatingMockConnection) mockConnection).checkExpectations(
                    expectedDateDeletes, expectedDateInserts);
        }

        if (mockRepositoryReader instanceof SDepMockRepositoryReader) {
            ((SDepMockRepositoryReader) mockRepositoryReader)
                    .checkExpectations();
        }
    }

    private void assertEqualArrays(String label, Object[] expected,
            Object[] actual) {
        if (!Arrays.equals(expected, actual)) {
            fail(label + ", expected: " + Arrays.deepToString(expected)
                    + ", actual: " + Arrays.deepToString(actual));

        }
    }

    private void assertEqualValues(String[] columns, Object[] expected,
            Object[] actual) {
        if (Arrays.equals(expected, actual)) {
            return;
        }

        String noValue = "_NO_VALUE_";
        String message = "";
        List<String> badColumns = new ArrayList<String>();
        for (int i = 0; i < columns.length; i++) {
            Object expectedValue = (i < expected.length) ? expected[i]
                    : noValue;
            Object actualValue = (i < actual.length) ? actual[i] : noValue;
            if (!equivalent(expectedValue, actualValue)) {
                badColumns.add(columns[i]);
            }
            String expectedString = (expectedValue == noValue) ? noValue
                    : (expectedValue == null) ? "null" : (expected[i]
                            .getClass().getName()
                            + "[" + expected[i] + "]");
            String actualString = (actualValue == noValue) ? noValue
                    : (actualValue == null) ? "null" : (actual[i].getClass()
                            .getName()
                            + "[" + actual[i] + "]");
            message += String.format("column '%s', expected=%s, actual=%s\n",
                    columns[i], expectedString, actualString);
        }
        if (!badColumns.isEmpty()) {
            message = "bad columns: " + badColumns + "\n" + message;
        }

        fail(message);
    }

    private boolean equivalent(Object o1, Object o2) {
        return (o1 == null) ? (o2 == null) : (o1.equals(o2));
    }

    /**
     * Reach into the {@link SQLUtility} class and get the instance that is
     * handling the JDBC-based methods.
     */
    private static SQLUtility getSqlUtilityInstance() {
        try {
            Field instanceField = SQLUtility.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            return (SQLUtility) instanceField.get(null);
        } catch (SecurityException e) {
            fail("Failed to set SqlUtility instance: " + e);
        } catch (NoSuchFieldException e) {
            fail("Failed to set SqlUtility instance: " + e);
        } catch (IllegalArgumentException e) {
            fail("Failed to set SqlUtility instance: " + e);
        } catch (IllegalAccessException e) {
            fail("Failed to set SqlUtility instance: " + e);
        }
        return null;
    }

    /**
     * Reach into the {@link SQLUtility} class and set an instance to handle the
     * JDBC-based methods.
     */
    private static void setSqlUtilityInstance(SQLUtility instance) {
        try {
            Field instanceField = SQLUtility.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, instance);
        } catch (SecurityException e) {
            fail("Failed to set SqlUtility instance: " + e);
        } catch (NoSuchFieldException e) {
            fail("Failed to set SqlUtility instance: " + e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail("Failed to set SqlUtility instance: " + e);
        } catch (IllegalAccessException e) {
            fail("Failed to set SqlUtility instance: " + e);
        }
    }

    /**
     * Base class for Mock {@link SQLUtility} implementations. Every method
     * causes the test to fail, unless overridden by the subclass.
     */
    public abstract static class MockSqlUtility extends SQLUtility {
        public abstract void checkExpectations();

        @Override
        protected void i_addRow(Connection conn, String table,
                String[] columns, String[] values, boolean[] numeric)
                throws SQLException {
            fail("Unexpected call to MockSqlUtility.i_addRow");
        }

        @Override
        protected void i_createNonExistingTables(ConnectionPool pool,
                InputStream dbSpec) throws IOException,
                InconsistentTableSpecException, SQLException {
            fail("Unexpected call to MockSqlUtility.i_addRow");
        }

        @Override
        protected void i_createTables(TableCreatingConnection tcConn,
                List<TableSpec> specs) throws SQLException {
            fail("Unexpected call to MockSqlUtility.i_addRow");
        }

        @Override
        protected ConnectionPool i_getConnectionPool(DatastoreConfiguration cpDC)
                throws SQLException {
            fail("Unexpected call to MockSqlUtility.i_addRow");
            return null;
        }

        @Override
        protected String i_getLongString(ResultSet rs, int pos)
                throws SQLException {
            fail("Unexpected call to MockSqlUtility.i_addRow");
            return null;
        }

        @Override
        protected List<TableSpec> i_getNonExistingTables(Connection conn,
                List<TableSpec> specs) throws SQLException {
            fail("Unexpected call to MockSqlUtility.i_addRow");
            return null;
        }

        @Override
        protected void i_replaceInto(Connection conn, String table,
                String[] columns, String[] values, String uniqueColumn,
                boolean[] numeric) throws SQLException {
            fail("Unexpected call to MockSqlUtility.i_addRow");
        }

        @Override
        protected boolean i_updateRow(Connection conn, String table,
                String[] columns, String[] values, String uniqueColumn,
                boolean[] numeric) throws SQLException {
            fail("Unexpected call to MockSqlUtility.i_addRow");
            return false;
        }
    }

    public static class UnusedMockSqlUtility extends MockSqlUtility {
        @Override
        public void checkExpectations() {
            // Nothing to check.
        }

    }

    private class UpdatingMockSqlUtility extends MockSqlUtility {
        private final String[] expectedColumns;

        private final String[] expectedValues;

        private String[] actualColumns;

        private String[] actualValues;

        /**
         * Write down some of what we expect to have happen.
         *
         * @param expectedColumns
         * @param expectedValues
         */
        public UpdatingMockSqlUtility(String[] expectedColumns,
                List<String> expectedValues) {
            this.expectedColumns = expectedColumns;
            this.expectedValues = expectedValues
                    .toArray(new String[expectedValues.size()]);
        }

        /**
         * If we get a replace call, store the columns and values for testing
         * later. (If we get more then one call, only the last will be
         * retained.)
         */
        @Override
        protected void i_replaceInto(Connection conn, String table,
                String[] columns, String[] values, String uniqueColumn,
                boolean[] numeric) throws SQLException {
            this.actualColumns = columns;
            this.actualValues = values;
        }

        @Override
        public void checkExpectations() {
            assertEqualArrays("column names", expectedColumns, actualColumns);
            assertEqualValues(expectedColumns, expectedValues, actualValues);
        }
    }

    private static class UnusedMockConnection extends MockConnection {
        @Override
        public Statement createStatement() throws SQLException {
            fail("Unexpected call to UnusedMockConnection.createStatement");
            return null;
        }
    }

    private static class UpdatingMockConnection extends MockConnection {
        private int deleteCalls = 0;

        private int insertCalls = 0;

        @Override
        public Statement createStatement() throws SQLException {
            return new MockStatement() {
                @Override
                public int executeUpdate(String sql) throws SQLException {
                    if (sql.trim().toLowerCase().startsWith("insert")) {
                        insertCalls++;
                    }
                    if (sql.trim().toLowerCase().startsWith("delete")) {
                        deleteCalls++;
                    }
                    return 1;
                }
            };
        }

        public void checkExpectations(int expectedDeletes, int expectedInserts) {
            assertEquals("delete calls", expectedDeletes, deleteCalls);
            assertEquals("insert calls", expectedInserts, insertCalls);
        }
    }

    private static class UnusedMockRepositoryReader extends
            MockRepositoryReader {
        @Override
        public synchronized ServiceDeploymentReader getServiceDeploymentReader(
                boolean cachedObjectRequired, Context context, String pid)
                throws ServerException {
            fail("Unexpected call to UnusedMockRepositoryReader.getServiceDeploymentReader");
            return null;
        }
    }

    private static class SDepMockRepositoryReader extends MockRepositoryReader {
        private int calls;

        public SDepMockRepositoryReader() {
        }

        public void checkExpectations() {
            assertEquals("sDep reader calls", 1, calls);
        }

        @Override
        public synchronized ServiceDeploymentReader getServiceDeploymentReader(
                boolean cachedObjectRequired, Context context, String pid)
                throws ServerException {
            calls++;
            return new MockServiceDeploymentReader(null) {
                @Override
                public DeploymentDSBindSpec getServiceDSInputSpec(Date versDateTime)
                        throws ServerException {
                    DeploymentDSBindSpec spec = new DeploymentDSBindSpec();
                    return spec;
                }
            };
        }
    }

    private class MyMockDriver extends MockDriver {
        @Override
        public Connection connect(String url, Properties info)
                throws SQLException {
            return mockConnection;
        }
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
