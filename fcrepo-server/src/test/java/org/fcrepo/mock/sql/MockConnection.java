/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.mock.sql;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * A partial implementation of {@link Connection} for use in unit tests. Add
 * more mocking to this class as needed, or override methods in sub-classes.
 *
 * @author Jim Blake
 */
public class MockConnection
        implements Connection {

    // ----------------------------------------------------------------------
    // Mocking infrastructure
    // ----------------------------------------------------------------------

    protected boolean closed;

    protected boolean autoCommit;

    protected boolean readOnly = false;

    protected SQLWarning warnings;

    protected final List<MockStatement> statements =
            new ArrayList<MockStatement>();

    protected final List<MockPreparedStatement> preparedStatements =
            new ArrayList<MockPreparedStatement>();

    public void reset() {
        closed = false;
        autoCommit = false;
        warnings = null;
        statements.clear();
        preparedStatements.clear();
    }

    public List<MockStatement> getStatements() {
        return new ArrayList<MockStatement>(statements);
    }

    public List<MockPreparedStatement> getPreparedStatements() {
        return new ArrayList<MockPreparedStatement>(preparedStatements);
    }

    // ----------------------------------------------------------------------
    // Mocked methods
    // ----------------------------------------------------------------------

    public void clearWarnings() throws SQLException {
        warnings = null;
    }

    public void close() throws SQLException {
        for (MockPreparedStatement stmt : preparedStatements) {
            stmt.close();
        }
        closed = true;
    }

    public boolean getAutoCommit() throws SQLException {
        return autoCommit;
    }

    public SQLWarning getWarnings() throws SQLException {
        return warnings;
    }

    public boolean isClosed() throws SQLException {
        return closed;
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        MockPreparedStatement stmt = new MockPreparedStatement(sql);
        preparedStatements.add(stmt);
        return stmt;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.autoCommit = autoCommit;
    }

    public Statement createStatement() throws SQLException {
        MockStatement stmt = new MockStatement();
        statements.add(stmt);
        return stmt;
    }

    public boolean isReadOnly() throws SQLException {
        return readOnly;
    }

    public void setReadOnly(boolean arg0) throws SQLException {
        readOnly = arg0;
    }

    // ----------------------------------------------------------------------
    // Un-implemented methods
    // ----------------------------------------------------------------------

    public void commit() throws SQLException {
        throw new RuntimeException("MockConnection.commit not implemented");
    }

    public Statement createStatement(int arg0, int arg1) throws SQLException {
        throw new RuntimeException("MockConnection.createStatement not implemented");
    }

    public Statement createStatement(int arg0, int arg1, int arg2)
            throws SQLException {
        throw new RuntimeException("MockConnection.createStatement not implemented");
    }

    public String getCatalog() throws SQLException {
        throw new RuntimeException("MockConnection.getCatalog not implemented");
    }

    public int getHoldability() throws SQLException {
        throw new RuntimeException("MockConnection.getHoldability not implemented");
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        throw new RuntimeException("MockConnection.getMetaData not implemented");
    }

    public int getTransactionIsolation() throws SQLException {
        throw new RuntimeException("MockConnection.getTransactionIsolation not implemented");
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw new RuntimeException("MockConnection.getTypeMap not implemented");
    }

    public String nativeSQL(String arg0) throws SQLException {
        throw new RuntimeException("MockConnection.nativeSQL not implemented");
    }

    public CallableStatement prepareCall(String arg0) throws SQLException {
        throw new RuntimeException("MockConnection.prepareCall not implemented");
    }

    public CallableStatement prepareCall(String arg0, int arg1, int arg2)
            throws SQLException {
        throw new RuntimeException("MockConnection.prepareCall not implemented");
    }

    public CallableStatement prepareCall(String arg0,
                                         int arg1,
                                         int arg2,
                                         int arg3) throws SQLException {
        throw new RuntimeException("MockConnection.prepareCall not implemented");
    }

    public PreparedStatement prepareStatement(String arg0, int arg1)
            throws SQLException {
        throw new RuntimeException("MockConnection.prepareStatement not implemented");
    }

    public PreparedStatement prepareStatement(String arg0, int[] arg1)
            throws SQLException {
        throw new RuntimeException("MockConnection.prepareStatement not implemented");
    }

    public PreparedStatement prepareStatement(String arg0, String[] arg1)
            throws SQLException {
        throw new RuntimeException("MockConnection.prepareStatement not implemented");
    }

    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2)
            throws SQLException {
        throw new RuntimeException("MockConnection.prepareStatement not implemented");
    }

    public PreparedStatement prepareStatement(String arg0,
                                              int arg1,
                                              int arg2,
                                              int arg3) throws SQLException {
        throw new RuntimeException("MockConnection.prepareStatement not implemented");
    }

    public void releaseSavepoint(Savepoint arg0) throws SQLException {
        throw new RuntimeException("MockConnection.releaseSavepoint not implemented");
    }

    public void rollback() throws SQLException {
        throw new RuntimeException("MockConnection.rollback not implemented");
    }

    public void rollback(Savepoint arg0) throws SQLException {
        throw new RuntimeException("MockConnection.rollback not implemented");
    }

    public void setCatalog(String arg0) throws SQLException {
        throw new RuntimeException("MockConnection.setCatalog not implemented");
    }

    public void setHoldability(int arg0) throws SQLException {
        throw new RuntimeException("MockConnection.setHoldability not implemented");
    }

    public Savepoint setSavepoint() throws SQLException {
        throw new RuntimeException("MockConnection.setSavepoint not implemented");
    }

    public Savepoint setSavepoint(String arg0) throws SQLException {
        throw new RuntimeException("MockConnection.setSavepoint not implemented");
    }

    public void setTransactionIsolation(int arg0) throws SQLException {
        throw new RuntimeException("MockConnection.setTransactionIsolation not implemented");
    }

    public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException {
        throw new RuntimeException("MockConnection.setTypeMap not implemented");
    }

    @Override
    public Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public Clob createClob() throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public String getClientInfo(String arg0) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public boolean isValid(int arg0) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setClientInfo(Properties arg0) throws SQLClientInfoException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setClientInfo(String arg0, String arg1)
            throws SQLClientInfoException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

	public void abort(Executor arg0) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.7 JDBC methods are not supported");
	}

	public int getNetworkTimeout() throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.7 JDBC methods are not supported");
	}

	public String getSchema() throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.7 JDBC methods are not supported");
	}

	public void setNetworkTimeout(Executor arg0, int arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.7 JDBC methods are not supported");
	}

	public void setSchema(String arg0) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.7 JDBC methods are not supported");
	}
}
