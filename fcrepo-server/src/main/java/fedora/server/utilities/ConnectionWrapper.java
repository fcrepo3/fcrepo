/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

/* JDBC_4_ANT_TOKEN_BEGIN -
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLXML;
import java.sql.Struct;
- JDBC_4_ANT_TOKEN_END */
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;

import java.util.Map;

/**
 * A wrapper around a java.sql.Connection that calls the wrapped Connection's
 * methods for all calls.
 * 
 * @author Chris Wilper
 */
public abstract class ConnectionWrapper
        implements Connection {

    private final Connection m_wrappedConnection;

    public ConnectionWrapper(Connection wrapped) {
        m_wrappedConnection = wrapped;
    }
    
    /* JDBC_4_ANT_TOKEN_BEGIN -

    public boolean isWrapperFor(Class<?> iface) {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public <T> T unwrap(Class<T> a) {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public Struct createStruct(String a, Object[] b) {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public Array createArrayOf(String a, Object[] b) {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public java.util.Properties getClientInfo() {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public void setClientInfo(java.util.Properties a) {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public void setClientInfo(String a, String b) {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public boolean isValid(int a) {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public Clob createClob() {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public Blob createBlob() {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public NClob createNClob() {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public SQLXML createSQLXML() {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    public String getClientInfo(String a) {
        throw new UnsupportedOperationException(
                "Java 1.6 Connection methods are not supported");
    }

    - JDBC_4_ANT_TOKEN_END */

    public Statement createStatement() throws SQLException {
        return m_wrappedConnection.createStatement();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return m_wrappedConnection.createStatement(resultSetType,
                                                   resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return m_wrappedConnection.prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql,
                                              int resultSetType,
                                              int resultSetConcurrency)
            throws SQLException {
        return m_wrappedConnection.prepareStatement(sql,
                                                    resultSetType,
                                                    resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return m_wrappedConnection.prepareCall(sql);
    }

    public CallableStatement prepareCall(String sql,
                                         int resultSetType,
                                         int resultSetConcurrency)
            throws SQLException {
        return m_wrappedConnection.prepareCall(sql,
                                               resultSetType,
                                               resultSetConcurrency);
    }

    public String nativeSQL(String sql) throws SQLException {
        return m_wrappedConnection.nativeSQL(sql);
    }

    public boolean getAutoCommit() throws SQLException {
        return m_wrappedConnection.getAutoCommit();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        m_wrappedConnection.setAutoCommit(autoCommit);
    }

    public void commit() throws SQLException {
        m_wrappedConnection.commit();
    }

    public void rollback() throws SQLException {
        m_wrappedConnection.rollback();
    }

    public void close() throws SQLException {
        m_wrappedConnection.close();
    }

    public boolean isClosed() throws SQLException {
        return m_wrappedConnection.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return m_wrappedConnection.getMetaData();
    }

    public boolean isReadOnly() throws SQLException {
        return m_wrappedConnection.isReadOnly();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        m_wrappedConnection.setReadOnly(readOnly);
    }

    public String getCatalog() throws SQLException {
        return m_wrappedConnection.getCatalog();
    }

    public void setCatalog(String catalog) throws SQLException {
        m_wrappedConnection.setCatalog(catalog);
    }

    public int getTransactionIsolation() throws SQLException {
        return m_wrappedConnection.getTransactionIsolation();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        m_wrappedConnection.setTransactionIsolation(level);
    }

    public SQLWarning getWarnings() throws SQLException {
        return m_wrappedConnection.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        m_wrappedConnection.clearWarnings();
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return m_wrappedConnection.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        m_wrappedConnection.setTypeMap(map);
    }

    public void setHoldability(int holdability) throws SQLException {
        m_wrappedConnection.setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return m_wrappedConnection.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return m_wrappedConnection.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return m_wrappedConnection.setSavepoint(name);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        m_wrappedConnection.rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        m_wrappedConnection.releaseSavepoint(savepoint);
    }

    public Statement createStatement(int resultSetType,
                                     int resultSetConcurrency,
                                     int resultSetHoldability)
            throws SQLException {
        return m_wrappedConnection.createStatement(resultSetType,
                                                   resultSetConcurrency,
                                                   resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql,
                                              int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability)
            throws SQLException {
        return m_wrappedConnection.prepareStatement(sql,
                                                    resultSetType,
                                                    resultSetConcurrency,
                                                    resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql,
                                         int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability)
            throws SQLException {
        return m_wrappedConnection.prepareCall(sql,
                                               resultSetType,
                                               resultSetConcurrency,
                                               resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        return m_wrappedConnection.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        return m_wrappedConnection.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        return m_wrappedConnection.prepareStatement(sql, columnNames);
    }

}
