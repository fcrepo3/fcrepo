/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package mock.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * A partial implementation of {@link Statement} for use in unit tests. Add more
 * mocking to this class as needed, or override methods in sub-classes.
 *
 * @author Jim Blake
 */
public class MockStatement
        implements Statement {

    // ----------------------------------------------------------------------
    // Mocking infrastructure
    // ----------------------------------------------------------------------

    protected boolean closed;

    protected boolean executed;

    public void reset() {
        closed = false;
        executed = false;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean hasBeenExecuted() {
        return executed;
    }

    // ----------------------------------------------------------------------
    // Mocked methods
    // ----------------------------------------------------------------------

    public void close() throws SQLException {
        closed = true;
    }

    // ----------------------------------------------------------------------
    // Un-implemented methods
    // ----------------------------------------------------------------------

    public void addBatch(String arg0) throws SQLException {
        throw new RuntimeException("MockStatement.addBatch not implemented");
    }

    public void cancel() throws SQLException {
        throw new RuntimeException("MockStatement.cancel not implemented");
    }

    public void clearBatch() throws SQLException {
        throw new RuntimeException("MockStatement.clearBatch not implemented");
    }

    public void clearWarnings() throws SQLException {
        throw new RuntimeException("MockStatement.clearWarnings not implemented");
    }

    public boolean execute(String arg0) throws SQLException {
        throw new RuntimeException("MockStatement.execute not implemented");
    }

    public boolean execute(String arg0, int arg1) throws SQLException {
        throw new RuntimeException("MockStatement.execute not implemented");
    }

    public boolean execute(String arg0, int[] arg1) throws SQLException {
        throw new RuntimeException("MockStatement.execute not implemented");
    }

    public boolean execute(String arg0, String[] arg1) throws SQLException {
        throw new RuntimeException("MockStatement.execute not implemented");
    }

    public int[] executeBatch() throws SQLException {
        throw new RuntimeException("MockStatement.executeBatch not implemented");
    }

    public ResultSet executeQuery(String arg0) throws SQLException {
        throw new RuntimeException("MockStatement.executeQuery not implemented");
    }

    public int executeUpdate(String arg0) throws SQLException {
        throw new RuntimeException("MockStatement.executeUpdate not implemented");
    }

    public int executeUpdate(String arg0, int arg1) throws SQLException {
        throw new RuntimeException("MockStatement.executeUpdate not implemented");
    }

    public int executeUpdate(String arg0, int[] arg1) throws SQLException {
        throw new RuntimeException("MockStatement.executeUpdate not implemented");
    }

    public int executeUpdate(String arg0, String[] arg1) throws SQLException {
        throw new RuntimeException("MockStatement.executeUpdate not implemented");
    }

    public Connection getConnection() throws SQLException {
        throw new RuntimeException("MockStatement.getConnection not implemented");
    }

    public int getFetchDirection() throws SQLException {
        throw new RuntimeException("MockStatement.getFetchDirection not implemented");
    }

    public int getFetchSize() throws SQLException {
        throw new RuntimeException("MockStatement.getFetchSize not implemented");
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        throw new RuntimeException("MockStatement.getGeneratedKeys not implemented");
    }

    public int getMaxFieldSize() throws SQLException {
        throw new RuntimeException("MockStatement.getMaxFieldSize not implemented");
    }

    public int getMaxRows() throws SQLException {
        throw new RuntimeException("MockStatement.getMaxRows not implemented");
    }

    public boolean getMoreResults() throws SQLException {
        throw new RuntimeException("MockStatement.getMoreResults not implemented");
    }

    public boolean getMoreResults(int arg0) throws SQLException {
        throw new RuntimeException("MockStatement.getMoreResults not implemented");
    }

    public int getQueryTimeout() throws SQLException {
        throw new RuntimeException("MockStatement.getQueryTimeout not implemented");
    }

    public ResultSet getResultSet() throws SQLException {
        throw new RuntimeException("MockStatement.getResultSet not implemented");
    }

    public int getResultSetConcurrency() throws SQLException {
        throw new RuntimeException("MockStatement.getResultSetConcurrency not implemented");
    }

    public int getResultSetHoldability() throws SQLException {
        throw new RuntimeException("MockStatement.getResultSetHoldability not implemented");
    }

    public int getResultSetType() throws SQLException {
        throw new RuntimeException("MockStatement.getResultSetType not implemented");
    }

    public int getUpdateCount() throws SQLException {
        throw new RuntimeException("MockStatement.getUpdateCount not implemented");
    }

    public SQLWarning getWarnings() throws SQLException {
        throw new RuntimeException("MockStatement.getWarnings not implemented");
    }

    public void setCursorName(String arg0) throws SQLException {
        throw new RuntimeException("MockStatement.setCursorName not implemented");
    }

    public void setEscapeProcessing(boolean arg0) throws SQLException {
        throw new RuntimeException("MockStatement.setEscapeProcessing not implemented");
    }

    public void setFetchDirection(int arg0) throws SQLException {
        throw new RuntimeException("MockStatement.setFetchDirection not implemented");
    }

    public void setFetchSize(int arg0) throws SQLException {
        throw new RuntimeException("MockStatement.setFetchSize not implemented");
    }

    public void setMaxFieldSize(int arg0) throws SQLException {
        throw new RuntimeException("MockStatement.setMaxFieldSize not implemented");
    }

    public void setMaxRows(int arg0) throws SQLException {
        throw new RuntimeException("MockStatement.setMaxRows not implemented");
    }

    public void setQueryTimeout(int arg0) throws SQLException {
        throw new RuntimeException("MockStatement.setQueryTimeout not implemented");
    }

    /* JDBC_4_ANT_TOKEN_BEGIN -
    @Override
    public boolean isPoolable() throws SQLException {
        throw new UnsupportedOperationException(
            "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setPoolable(boolean arg0) throws SQLException {
        throw new UnsupportedOperationException(
            "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        throw new UnsupportedOperationException(
            "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        throw new UnsupportedOperationException(
            "Java 1.6 JDBC methods are not supported");
    }
    - JDBC_4_ANT_TOKEN_END */
}
