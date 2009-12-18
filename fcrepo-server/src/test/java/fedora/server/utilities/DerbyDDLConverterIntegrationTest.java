/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.io.IOException;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import mock.fedora.server.utilities.MockTableSpec;

import fedora.server.errors.InconsistentTableSpecException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrew Woods
 */
public class DerbyDDLConverterIntegrationTest {

    private DerbyDDLConverter converter;

    private List<String> expectedStmts;

    private final String OBJECT_PATHS_TABLE_SPEC =
            "CREATE TABLE objectPaths (\n  "
                    + "tokenDbID INT NOT NULL GENERATED ALWAYS AS IDENTITY,\n  "
                    + "token VARCHAR(64) UNIQUE NOT NULL DEFAULT '',\n  "
                    + "path VARCHAR(255) NOT NULL DEFAULT '',\n  "
                    + "PRIMARY KEY (tokenDbID))";

    private final String DATASTREAM_PATHS_TABLE_SPEC =
            "CREATE TABLE datastreamPaths (\n  "
                    + "tokenDbID INT NOT NULL GENERATED ALWAYS AS IDENTITY,\n  "
                    + "token VARCHAR(199) UNIQUE NOT NULL DEFAULT '',\n  "
                    + "path VARCHAR(255) NOT NULL DEFAULT '',\n  "
                    + "PRIMARY KEY (tokenDbID))";

    private final String PID_GEN_TABLE_SPEC =
            "CREATE TABLE pidGen (\n  "
                    + "namespace VARCHAR(255) NOT NULL,\n  "
                    + "highestID INT NOT NULL)";

    private final String DO_REGISTRY_TABLE_SPEC =
            "CREATE TABLE doRegistry (\n  " + "doPID VARCHAR(64) NOT NULL,\n  "
                    + "systemVersion SMALLINT NOT NULL DEFAULT 0,\n  "
                    + "ownerId VARCHAR(64),\n  "
                    + "objectState VARCHAR(1) NOT NULL DEFAULT 'A',\n  "
                    + "label VARCHAR(255) DEFAULT '',\n  "
                    + "PRIMARY KEY (doPID))";

    private final String MODEL_DEPLOYMENT_MAP_TABLE_SPEC =
            "CREATE TABLE modelDeploymentMap (\n  "
                    + "cModel VARCHAR(64) NOT NULL,\n  "
                    + "sDef VARCHAR(64) NOT NULL,\n  "
                    + "sDep VARCHAR(64) NOT NULL)";

    private final String DO_FIELDS_TABLE_SPEC =
            "CREATE TABLE doFields (\n  " + "pid VARCHAR(64) NOT NULL,\n  "
                    + "label VARCHAR(255),\n  "
                    + "state VARCHAR(1) NOT NULL DEFAULT 'A',\n  "
                    + "ownerId VARCHAR(64),\n  " + "cDate BIGINT NOT NULL,\n  "
                    + "mDate BIGINT NOT NULL,\n  " + "dcmDate BIGINT,\n  "
                    + "dcTitle CLOB,\n  " + "dcCreator CLOB,\n  "
                    + "dcSubject CLOB,\n  " + "dcDescription CLOB,\n  "
                    + "dcPublisher CLOB,\n  " + "dcContributor CLOB,\n  "
                    + "dcDate CLOB,\n  " + "dcType CLOB,\n  "
                    + "dcFormat CLOB,\n  " + "dcIdentifier CLOB,\n  "
                    + "dcSource CLOB,\n  " + "dcLanguage CLOB,\n  "
                    + "dcRelation CLOB,\n  " + "dcCoverage CLOB,\n  "
                    + "dcRights CLOB)";

    private final String DC_DATES_TABLE_SPEC =
            "CREATE TABLE dcDates (\n  " + "pid VARCHAR(64) NOT NULL,\n  "
                    + "dcDate BIGINT NOT NULL)";

    private final String DO_FIELDS_INDEX =
            "CREATE INDEX doFields_pid ON doFields (pid)";

    private final String DC_DATES_INDEX =
            "CREATE INDEX dcDates_pid ON dcDates (pid)";

    @Before
    public void setUp() throws Exception {
        converter = new DerbyDDLConverter();
        expectedStmts = new LinkedList<String>();

        expectedStmts.add(OBJECT_PATHS_TABLE_SPEC);
        expectedStmts.add(DATASTREAM_PATHS_TABLE_SPEC);
        expectedStmts.add(PID_GEN_TABLE_SPEC);
        expectedStmts.add(DO_REGISTRY_TABLE_SPEC);
        expectedStmts.add(MODEL_DEPLOYMENT_MAP_TABLE_SPEC);
        expectedStmts.add(DO_FIELDS_TABLE_SPEC);
        expectedStmts.add(DC_DATES_TABLE_SPEC);
        expectedStmts.add(DO_FIELDS_INDEX);
        expectedStmts.add(DC_DATES_INDEX);
    }

    @After
    public void tearDown() throws Exception {
        converter = null;
        expectedStmts = null;
    }

    @Test
    public void testGetDDL() throws InconsistentTableSpecException, IOException {
        List<TableSpec> tableSpecs =
                TableSpec.getTableSpecs(MockTableSpec.getTableSpecStream());
        verifyInputTableSpec(tableSpecs);

        for (TableSpec spec : tableSpecs) {
            List<String> statements = converter.getDDL(spec);
            for (String stmt : statements) {
                verifyAndEliminateStatement(stmt);
            }
        }

        assertTrue("Some statements unverified: " + expectedStmts.size(),
                   expectedStmts.isEmpty());
    }

    private void verifyInputTableSpec(List<TableSpec> tableSpecs)
            throws InconsistentTableSpecException, IOException {

        final int NUM_SPECS = 7;
        int numTableSpecs = tableSpecs.size();
        assertTrue("There should be " + NUM_SPECS + " tableSpecs: "
                + numTableSpecs, numTableSpecs == NUM_SPECS);
    }

    private void verifyAndEliminateStatement(String stmt) {
        assertNotNull(stmt);

        String foundStmt = null;
        for (String expected : expectedStmts) {
            if (stmt.equalsIgnoreCase(expected)) {
                foundStmt = expected;
            }
        }
        assertTrue("Creation statement invalid: '" + stmt + "'",
                   foundStmt != null);
        expectedStmts.remove(foundStmt);
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(DerbyDDLConverterIntegrationTest.class);
    }
}
