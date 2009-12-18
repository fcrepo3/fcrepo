/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package mock.sql;

import java.io.InputStream;
import java.io.Reader;

import java.net.URL;

/* JDBC_4_ANT_TOKEN_BEGIN -
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLXML;
- JDBC_4_ANT_TOKEN_END */

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.Arrays;
import java.util.Calendar;

import java.math.BigDecimal;

/**
 * A partial implementation of {@link PreparedStatement} for use in unit tests.
 * Add more mocking to this class as needed, or override methods in sub-classes.
 *
 * @author Jim Blake
 */
public class MockPreparedStatement
        implements PreparedStatement {

    // ----------------------------------------------------------------------
    // Mocking infrastructure
    // ----------------------------------------------------------------------

    private final String sql;

    /** Don't forget, first parameter is 1, not 0, so indexes are off by 1. */
    private final Object[] parameters;

    private boolean closed;

    private boolean executed;

    public MockPreparedStatement(String sql) {
        this.sql = sql;

        int howManyParameters = 0;
        for (int i = 0; i < sql.length(); i++) {
            if ('?' == sql.charAt(i)) {
                howManyParameters++;
            }
        }
        parameters = new Object[howManyParameters];
    }

    public void reset() {
        Arrays.fill(parameters, null);
        closed = false;
        executed = false;
    }

    public String getSql() {
        return sql;
    }

    public Object[] getParameters() {
        return parameters.clone();
    }

    /** Insure that a closed statement doesn't do anything else. */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Attempt to operate on closed statement.");
        }
    }

    /** Insure that a statement isn't executed twice. */
    private void checkExecuted() {
        if (executed) {
            throw new IllegalStateException("Attempt to operate on a "
                    + "statement that has already been executed.");
        }
    }

    /**
     * If the user tries to set a parameter, make sure that the index is within
     * a valid range, and adjust by one to an index into the parameter array.
     */
    private int convertIndex(int index) throws SQLException {
        if (index < 1 || index > parameters.length) {
            throw new SQLException("Index out of range: value is " + index
                    + ", must be between 1 and " + parameters.length);
        }
        return index - 1;
    }

    // ----------------------------------------------------------------------
    // Mocked methods
    // ----------------------------------------------------------------------

    public void close() throws SQLException {
        closed = true;
    }

    public int executeUpdate() throws SQLException {
        checkClosed();
        checkExecuted();
        executed = true;
        return 0;
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        checkClosed();
        checkExecuted();
        parameters[convertIndex(parameterIndex)] = x;
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        checkClosed();
        checkExecuted();
        parameters[convertIndex(parameterIndex)] = x;
    }

    // ----------------------------------------------------------------------
    // Un-implemented methods
    // ----------------------------------------------------------------------

    public void addBatch() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.addBatch not implemented");
    }

    public void clearParameters() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.clearParameters not implemented");
    }

    public boolean execute() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.execute not implemented");
    }

    public ResultSet executeQuery() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.executeQuery not implemented");
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getMetaData not implemented");
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getParameterMetaData not implemented");
    }

    public void setArray(int arg0, Array arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setArray not implemented");
    }

    public void setAsciiStream(int arg0, InputStream arg1, int arg2)
            throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setAsciiStream not implemented");
    }

    public void setBigDecimal(int arg0, BigDecimal arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setBigDecimal not implemented");
    }

    public void setBinaryStream(int arg0, InputStream arg1, int arg2)
            throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setBinaryStream not implemented");
    }

    public void setBlob(int arg0, Blob arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setBlob not implemented");
    }

    public void setBoolean(int arg0, boolean arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setBoolean not implemented");
    }

    public void setByte(int arg0, byte arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setByte not implemented");
    }

    public void setBytes(int arg0, byte[] arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setBytes not implemented");
    }

    public void setCharacterStream(int arg0, Reader arg1, int arg2)
            throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setCharacterStream not implemented");
    }

    public void setClob(int arg0, Clob arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setClob not implemented");
    }

    public void setDate(int arg0, Date arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setDate not implemented");
    }

    public void setDate(int arg0, Date arg1, Calendar arg2) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setDate not implemented");
    }

    public void setDouble(int arg0, double arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setDouble not implemented");
    }

    public void setFloat(int arg0, float arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setFloat not implemented");
    }

    public void setInt(int arg0, int arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setInt not implemented");
    }

    public void setNull(int arg0, int arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setNull not implemented");
    }

    public void setNull(int arg0, int arg1, String arg2) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setNull not implemented");
    }

    public void setObject(int arg0, Object arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setObject not implemented");
    }

    public void setObject(int arg0, Object arg1, int arg2) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setObject not implemented");
    }

    public void setObject(int arg0, Object arg1, int arg2, int arg3)
            throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setObject not implemented");
    }

    public void setRef(int arg0, Ref arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setRef not implemented");
    }

    public void setShort(int arg0, short arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setShort not implemented");
    }

    public void setTime(int arg0, Time arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setTime not implemented");
    }

    public void setTime(int arg0, Time arg1, Calendar arg2) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setTime not implemented");
    }

    public void setTimestamp(int arg0, Timestamp arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setTimestamp not implemented");
    }

    public void setTimestamp(int arg0, Timestamp arg1, Calendar arg2)
            throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setTimestamp not implemented");
    }

    public void setURL(int arg0, URL arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setURL not implemented");
    }

    public void setUnicodeStream(int arg0, InputStream arg1, int arg2)
            throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setUnicodeStream not implemented");
    }

    public void addBatch(String arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.addBatch not implemented");
    }

    public void cancel() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.cancel not implemented");
    }

    public void clearBatch() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.clearBatch not implemented");
    }

    public void clearWarnings() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.clearWarnings not implemented");
    }

    public boolean execute(String arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.execute not implemented");
    }

    public boolean execute(String arg0, int arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.execute not implemented");
    }

    public boolean execute(String arg0, int[] arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.execute not implemented");
    }

    public boolean execute(String arg0, String[] arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.execute not implemented");
    }

    public int[] executeBatch() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.executeBatch not implemented");
    }

    public ResultSet executeQuery(String arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.executeQuery not implemented");
    }

    public int executeUpdate(String arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.executeUpdate not implemented");
    }

    public int executeUpdate(String arg0, int arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.executeUpdate not implemented");
    }

    public int executeUpdate(String arg0, int[] arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.executeUpdate not implemented");
    }

    public int executeUpdate(String arg0, String[] arg1) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.executeUpdate not implemented");
    }

    public Connection getConnection() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getConnection not implemented");
    }

    public int getFetchDirection() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getFetchDirection not implemented");
    }

    public int getFetchSize() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getFetchSize not implemented");
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getGeneratedKeys not implemented");
    }

    public int getMaxFieldSize() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getMaxFieldSize not implemented");
    }

    public int getMaxRows() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getMaxRows not implemented");
    }

    public boolean getMoreResults() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getMoreResults not implemented");
    }

    public boolean getMoreResults(int arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getMoreResults not implemented");
    }

    public int getQueryTimeout() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getQueryTimeout not implemented");
    }

    public ResultSet getResultSet() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getResultSet not implemented");
    }

    public int getResultSetConcurrency() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getResultSetConcurrency not implemented");
    }

    public int getResultSetHoldability() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getResultSetHoldability not implemented");
    }

    public int getResultSetType() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getResultSetType not implemented");
    }

    public int getUpdateCount() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getUpdateCount not implemented");
    }

    public SQLWarning getWarnings() throws SQLException {
        throw new RuntimeException("MockPreparedStatement.getWarnings not implemented");
    }

    public void setCursorName(String arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setCursorName not implemented");
    }

    public void setEscapeProcessing(boolean arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setEscapeProcessing not implemented");
    }

    public void setFetchDirection(int arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setFetchDirection not implemented");
    }

    public void setFetchSize(int arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setFetchSize not implemented");
    }

    public void setMaxFieldSize(int arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setMaxFieldSize not implemented");
    }

    public void setMaxRows(int arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setMaxRows not implemented");
    }

    public void setQueryTimeout(int arg0) throws SQLException {
        throw new RuntimeException("MockPreparedStatement.setQueryTimeout not implemented");
    }

    /* JDBC_4_ANT_TOKEN_BEGIN -
    @Override
    public void setAsciiStream(int arg0, InputStream arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setAsciiStream(int arg0, InputStream arg1, long arg2)
            throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1, long arg2)
            throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setBlob(int arg0, InputStream arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setBlob(int arg0, InputStream arg1, long arg2)
            throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1, long arg2)
            throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setClob(int arg0, Reader arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setClob(int arg0, Reader arg1, long arg2) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setNCharacterStream(int arg0, Reader arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setNCharacterStream(int arg0, Reader arg1, long arg2)
            throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setNClob(int arg0, NClob arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setNClob(int arg0, Reader arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setNClob(int arg0, Reader arg1, long arg2) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setNString(int arg0, String arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setRowId(int arg0, RowId arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setSQLXML(int arg0, SQLXML arg1) throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public boolean isClosed() throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public boolean isPoolable() throws SQLException {
        throw new UnsupportedOperationException(
        "Java 1.6 JDBC methods are not supported");
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
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
    - JDBC_4_ANT_TOKEN_END */
}
