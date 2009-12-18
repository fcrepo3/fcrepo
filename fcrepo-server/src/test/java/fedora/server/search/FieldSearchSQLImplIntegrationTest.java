/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.search;

import java.sql.Connection;
import java.sql.Statement;

import java.util.Date;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import fedora.server.storage.ConnectionPool;
import fedora.server.storage.DOReader;
import fedora.server.storage.MockRepositoryReader;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DigitalObject;

import static org.junit.Assert.assertEquals;

import static fedora.server.storage.types.ObjectBuilder.addXDatastream;
import static fedora.server.storage.types.ObjectBuilder.getDC;
import static fedora.server.storage.types.ObjectBuilder.getTestObject;
import static fedora.server.storage.types.ObjectBuilder.setDates;

public class FieldSearchSQLImplIntegrationTest {

    // test database constants
    private static final String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String url = "jdbc:derby:test;create=true";
    private static final String username = "test";
    private static final String password = "test";
    private static final int maxActive = 4;
    private static final int maxIdle = 4;
    private static final int maxWait = -1;
    private static final int minIdle = 0;
    private static final long minEvictableIdleTimeMillis = 1800000;
    private static final int numTestsPerEvictionRun = 3;
    private static final long timeBetweenEvictionRuns = -1;
    private static final String validationQuery = "values(1)";
    private static final boolean testOnBorrow = true;
    private static final boolean testOnReturn = true;
    private static final boolean testWhileIdle = true;
    private static final byte whenExhaustedAction = 1;

    // test fieldsearch impl constants
    private static final int maxResultsDefault = 100;
    private static final int maxSecondsPerSessionDefault = 600;

    private static ConnectionPool cPool;

    private MockRepositoryReader m_repo;
    private FieldSearchSQLImpl m_impl;

    // needs to be set in order for object serializers/deserializers to work
    static {
        Datastream.defaultChecksumType = "DISABLED";
    }

    // Test setUp:
    //   - create cPool if necessary
    //   - drop doRegistry table (silently fail if it doesn't exist)
    //   - re-create doRegistry table
    @Before
    public void setUpTest() throws Exception {
        if (cPool == null) {
            cPool = new ConnectionPool(driver,
                                       url,
                                       username,
                                       password,
                                       maxActive,
                                       maxIdle,
                                       maxWait,
                                       minIdle,
                                       minEvictableIdleTimeMillis,
                                       numTestsPerEvictionRun,
                                       timeBetweenEvictionRuns,
                                       validationQuery,
                                       testOnBorrow,
                                       testOnReturn,
                                       testWhileIdle,
                                       whenExhaustedAction);
        }
        Connection conn = cPool.getConnection();
        executeUpdate(conn, "DROP TABLE doFields", true);
        executeUpdate(conn, "DROP TABLE dcDates", true);
        executeUpdate(conn, "CREATE TABLE doFields (\n"
            + "pid VARCHAR(64) NOT NULL,\n"
            + "label VARCHAR(255) NOT NULL,\n"
            + "state VARCHAR(1) NOT NULL,\n"
            + "ownerId VARCHAR(64),\n"
            + "cDate BIGINT NOT NULL,\n"
            + "mDate BIGINT NOT NULL,\n"
            + "dcmDate BIGINT,\n"
            + "dcTitle CLOB,\n"
            + "dcCreator CLOB,\n"
            + "dcSubject CLOB,\n"
            + "dcDescription CLOB,\n"
            + "dcPublisher CLOB,\n"
            + "dcContributor CLOB,\n"
            + "dcDate CLOB,\n"
            + "dcType CLOB,\n"
            + "dcFormat CLOB,\n"
            + "dcIdentifier CLOB,\n"
            + "dcSource CLOB,\n"
            + "dcLanguage CLOB,\n"
            + "dcRelation CLOB,\n"
            + "dcCoverage CLOB,\n"
            + "dcRights CLOB)", false);
        executeUpdate(conn,
               "CREATE INDEX doFields_pid ON doFields (pid)", false);
        executeUpdate(conn, "CREATE TABLE dcDates (\n"
            + "pid VARCHAR(64) NOT NULL,"
            + "dcDate BIGINT NOT NULL)", false);
        executeUpdate(conn,
               "CREATE INDEX dcDates_pid ON dcDates (pid)", false);
        cPool.free(conn);
    }

    private static void executeUpdate(Connection conn,
                                      String sql,
                                      boolean ignoreError) throws Exception {
        Statement st = conn.createStatement();
        try {
            st.executeUpdate(sql);
        } catch (Exception e) {
            if (!ignoreError) {
                throw e;
            }
        } finally {
            try { st.close(); } catch (Exception e) { }
        }
    }

    // Test tearDown
    @AfterClass
    public static void tearDownTest() throws Exception {
        // FIXME: Although cPool.close() should be called, it causes the removal
        //       of the Embedded-Derby driver needed by 'ResourceIndexIntegrationTest.java'
        //
        // if (cPool != null) {
        //     cPool.close();
        // }
    }

    private void init(int maxResults,
                      int maxSecondsPerSession,
                      boolean indexDCFields) throws Exception {
        m_repo = new MockRepositoryReader();
        m_impl = new FieldSearchSQLImpl(cPool,
                                        m_repo,
                                        maxResults,
                                        maxSecondsPerSession,
                                        indexDCFields);
    }

    @Test
    public void testFindOneSeveralTimesNoDC() throws Exception {
        init(maxResultsDefault, maxSecondsPerSessionDefault, true);

        // add one object to index
        String pid1 = "test:1";
        DigitalObject obj1 = getTestObject(pid1, pid1);
        setDates(obj1, new Date());
        m_repo.putObject(obj1);
        DOReader reader1 = m_repo.getReader(false, null, pid1);
        m_impl.update(reader1);

        // query for everything several times
        // should get 1 page w/1 result each time
        // and cPool shouldn't be exhausted because the impl should
        // release each connection immediately after the query completes
        FieldSearchQuery query = new FieldSearchQuery("*");
        int[] expected = new int[] { 1, 1 };
        for (int i = 0; i < 20; i++) {
            checkResults(expected, countResults(query, 10));
        }
    }

    @Test
    public void testIndexAndFindByDCIdentifier() throws Exception {
        init(maxResultsDefault, maxSecondsPerSessionDefault, true);

        // add one object with DC to index
        String pid1 = "test:1";
        DigitalObject obj1 = getTestObject(pid1, pid1);
        String dcContent = "<dc:identifier>" + pid1 + "</dc:identifier>";
        addXDatastream(obj1, "DC", getDC(dcContent));

        setDates(obj1, new Date());
        m_repo.putObject(obj1);
        DOReader reader1 = m_repo.getReader(false, null, pid1);
        m_impl.update(reader1);

        // query for it via dc:identifier
        FieldSearchQuery query = new FieldSearchQuery(Condition.getConditions("identifier~" + pid1));
        int[] expected = new int[] { 1, 1 };

        // first try with client requesting max TEN results per page
        // should get 1 page w/1 result
        checkResults(expected, countResults(query, 10));

        // then try same, but with client requesting max ONE result per page
        // should still get 1 page w/1 result
        checkResults(expected, countResults(query, 1));
    }

    // runs the query (all pages) and returns { pageCount, resultCount }
    private int[] countResults(FieldSearchQuery query,
                               int maxResultsPerPage) throws Exception {
        int pageCount = 0;
        int resultCount = 0;
        FieldSearchResult page = m_impl.findObjects(new String[] { "pid" },
                                                    maxResultsPerPage,
                                                    query);
        while (page != null) {
            pageCount++;
            resultCount += page.objectFieldsList().size();
            if (page.getToken() != null) {
                m_impl.resumeFindObjects(page.getToken());
            } else {
                page = null;
            }
        }
        return new int[] { pageCount, resultCount };
    }

    private static void checkResults(int[] expected, int[] got) throws Exception {
        assertEquals("Unexpected page count", expected[0], got[0]);
        assertEquals("Unexpected result count", expected[1], got[1]);
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(FieldSearchSQLImplIntegrationTest.class);
    }
}
