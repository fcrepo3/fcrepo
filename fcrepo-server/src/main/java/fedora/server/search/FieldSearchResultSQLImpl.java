/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.search;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.QueryParseException;
import fedora.server.errors.RepositoryConfigurationException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StorageDeviceException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.UnrecognizedFieldException;
import fedora.server.storage.ConnectionPool;
import fedora.server.storage.DOReader;
import fedora.server.storage.RepositoryReader;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.MD5Utility;

/**
 * A FieldSearchResults object returned as the result of a FieldSearchSQLImpl
 * search.
 * <p>
 * A FieldSearchResultSQLImpl is intended to be re-used in cases where the
 * results of a query require more than one call to the server.
 * </p>
 *
 * @author Chris Wilper
 */
public class FieldSearchResultSQLImpl
        implements FieldSearchResult {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(FieldSearchResultSQLImpl.class.getName());

    /* fields supporting public accessors */
    private ArrayList m_objectFields;

    private String m_token;

    private long m_cursor = -1;

    private final long m_completeListSize = -1;

    private Date m_expirationDate;

    private String m_nextPID;

    /* invariants */
    private final Connection m_conn;

    private final ConnectionPool m_cPool;

    private final RepositoryReader m_repoReader;

    private final String[] m_resultFields;

    private final int m_maxResults;

    private final int m_maxSeconds;

    private long m_startMillis;

    /* internal state */
    private Statement m_statement;

    private ResultSet m_resultSet;

    private long m_nextCursor = 0;

    private boolean m_expired;

    /**
     * Construct a FieldSearchResultSQLImpl object.
     * <p />
     * Upon construction, a connection is obtained from the connectionPool, and
     * the query is executed. (The connection will be returned to the pool only
     * after the last result has been obtained from the ResultSet, the session
     * is expired, or some non-recoverable error has occurred)
     * <p />
     * Once the ResultSet is obtained, one result is requested of it (and
     * remembered for use in step()); then the call returns.
     *
     * @param cPool
     *        the connectionPool
     * @param repoReader
     *        the provider of object field information for results
     * @param resultFields
     *        which fields should be returned in results
     * @param maxResults
     *        how many results should be returned at one time. This should be
     *        the smaller of a) the FieldSearchImpl's limit [the server limit]
     *        and b) the requested limit [the client limit]
     * @param query
     *        the end-user query
     */
    protected FieldSearchResultSQLImpl(ConnectionPool cPool,
                                       RepositoryReader repoReader,
                                       String[] resultFields,
                                       int maxResults,
                                       int maxSeconds,
                                       FieldSearchQuery query)
            throws SQLException, QueryParseException {
        m_cPool = cPool;
        m_repoReader = repoReader;
        m_resultFields = resultFields;
        m_maxResults = maxResults;
        m_maxSeconds = maxSeconds;
        m_conn = m_cPool.getConnection();
        try {
            m_statement = m_conn.createStatement();
            m_resultSet =
                    m_statement
                            .executeQuery(logAndGetQueryText(query,
                                                             m_resultFields)); //2004.05.02 wdn5e
        } catch (SQLException sqle) {
            // if there's any kind of problem getting the resultSet,
            // give the connection back to the pool
            try {
                if (m_resultSet != null) {
                    m_resultSet.close();
                }
                if (m_statement != null) {
                    m_statement.close();
                }
                if (m_conn != null) {
                    m_cPool.free(m_conn);
                }
                throw sqle;
            } catch (SQLException sqle2) {
                throw sqle2;
            } finally {
                m_resultSet = null;
                m_statement = null;
            }
        }
    }

    //2004.05.02 wdn5e -- sort on selected fields
    private String logAndGetQueryText(FieldSearchQuery query,
                                      String[] resultFields) //2004.05.02 wdn5e
            throws SQLException, QueryParseException {
        StringBuffer queryText = new StringBuffer("SELECT");
        if (query.getType() == FieldSearchQuery.TERMS_TYPE) {
            queryText.append(" doFields.pid FROM doFields"
                    + getWhereClause(query.getTerms()));
        } else {
            StringBuffer resultFieldsString = new StringBuffer();
            if (resultFields.length > 0) {
                String delimiter = " ";
                for (String element : resultFields) {
                    String dbColumn = "doFields." + dcFixup(element);
                    resultFieldsString.append(delimiter + dbColumn);
                    delimiter = ", ";
                }
            }
            queryText.append(resultFieldsString);
            queryText.append(" FROM doFields");
            queryText.append(getWhereClause(query.getConditions()));
            // disabled sorting: see bug 78
            //            queryText.append(" ORDER BY");
            //            queryText.append(resultFieldsString);

        }
        String qt = queryText.toString();
        LOG.debug(qt);
        return qt;
    }

    private String getWhereClause(String terms) throws QueryParseException {
        if (terms.indexOf("'") != -1) {
            throw new QueryParseException("Query cannot contain the ' character.");
        }
        StringBuffer whereClause = new StringBuffer();
        if (!terms.equals("*") && !terms.equals("")) {
            whereClause.append(" WHERE");
            // formulate the where clause if the terms aren't * or ""
            int usedCount = 0;
            boolean needsEscape = false;
            for (String column : FieldSearchSQLImpl.DB_COLUMN_NAMES) {
                // use only stringish columns in query
                boolean use = column.indexOf("Date") == -1;
                if (!use) {
                    if (column.equals("dcDate")) {
                        use = true;
                    }
                }
                if (use) {
                    if (usedCount > 0) {
                        whereClause.append(" OR");
                    }
                    String qPart = toSql(column, terms);
                    if (qPart.charAt(0) == ' ') {
                        needsEscape = true;
                    } else {
                        whereClause.append(" ");
                    }
                    whereClause.append(qPart);
                    usedCount++;
                }
            }
            if (needsEscape) {
                //    whereClause.append(" {escape '/'}");
            }
        }
        return whereClause.toString();
    }

    private String getWhereClause(List conditions) throws QueryParseException {
        StringBuffer whereClause = new StringBuffer();
        boolean willJoin = false;
        if (conditions.size() > 0) {
            boolean needsEscape = false;
            whereClause.append(" WHERE");
            for (int i = 0; i < conditions.size(); i++) {
                Condition cond = (Condition) conditions.get(i);
                if (i > 0) {
                    whereClause.append(" AND");
                }
                String op = cond.getOperator().getSymbol();
                String prop = cond.getProperty();
                if (prop.toLowerCase().endsWith("date")) {
                    // deal with dates ... cDate mDate dcmDate date
                    if (op.equals("~")) {
                        if (prop.equals("date")) {
                            // query for dcDate as string
                            String sqlPart =
                                    toSql("doFields.dcDate", cond.getValue());
                            if (sqlPart.startsWith(" ")) {
                                needsEscape = true;
                            } else {
                                whereClause.append(' ');
                            }
                            whereClause.append(sqlPart);
                        } else {
                            throw new QueryParseException("The ~ operator "
                                    + "cannot be used with cDate, mDate, "
                                    + "or dcmDate because they are not "
                                    + "string-valued fields.");
                        }
                    } else { // =, <, <=, >, >=
                        // property must be parsable as a date... if ok,
                        // do (cDate, mDate, dcmDate)
                        // or (date) <- dcDate from dcDates table
                        Date dt = DateUtility.parseDateAsUTC(cond.getValue());
                        if (dt == null) {
                            throw new QueryParseException("When using "
                                    + "equality or inequality operators "
                                    + "with a date-based value, the date "
                                    + "must be in yyyy-MM-DD[THH:mm:ss[.SSS][Z]] "
                                    + "form.");
                        }
                        if (prop.equals("date")) {
                            // do a left join on the dcDates table...dcDate
                            // query will be of form:
                            // select pid
                            // from doFields
                            // left join dcDates on doFields.pid=dcDates.pid
                            // where...
                            if (!willJoin) {
                                willJoin = true;
                                whereClause.insert(0, " LEFT JOIN dcDates "
                                        + "ON doFields.pid=dcDates.pid");
                            }
                            whereClause.append(" dcDates.dcDate" + op
                                    + dt.getTime());
                        } else {
                            whereClause.append(" doFields." + prop + op
                                    + dt.getTime());
                        }
                    }
                } else {
                    if (op.equals("=")) {
                        if (isDCProp(prop)) {
                            throw new QueryParseException("The = operator "
                                    + "can only be used with dates and "
                                    + "non-repeating fields.");
                        } else {
                            // do a real equals check... do a toSql but
                            // reject it if it uses "LIKE"
                            String sqlPart =
                                    toSql("doFields." + prop, cond.getValue());
                            if (sqlPart.indexOf("LIKE ") != -1) {
                                throw new QueryParseException("The = "
                                        + "operator cannot be used with "
                                        + "wildcards.");
                            }
                            if (sqlPart.startsWith(" ")) {
                                needsEscape = true;
                            } else {
                                whereClause.append(' ');
                            }
                            whereClause.append(sqlPart);
                        }
                    } else if (op.equals("~")) {
                        if (isDCProp(prop)) {
                            // prepend dc and caps the first char first...
                            prop =
                                    "dc" + prop.substring(0, 1).toUpperCase()
                                            + prop.substring(1);
                        }
                        // the field name is ok, so toSql it
                        String sqlPart =
                                toSql("doFields." + prop, cond.getValue());
                        if (sqlPart.startsWith(" ")) {
                            needsEscape = true;
                        } else {
                            whereClause.append(' ');
                        }
                        whereClause.append(sqlPart);
                    } else {
                        throw new QueryParseException("Can't use >, >=, <, "
                                + "or <= operator on a string-based field.");
                    }
                }
            }
            if (needsEscape) {
                //    whereClause.append(" {escape '/'}");
            }
        }
        return whereClause.toString();
    }

    protected boolean isExpired() {
        long passedSeconds =
                (System.currentTimeMillis() - m_startMillis) / 1000;
        m_expired = passedSeconds > m_maxSeconds;
        LOG.debug("has fieldSearchResultSQL expired? "+m_expired +
                  ", passed: "+passedSeconds);
        if (m_expired) {
            // clean up
            try {
                if (m_resultSet != null) {
                    m_resultSet.close();
                }
                if (m_statement != null) {
                    m_statement.close();
                }
                if (m_conn != null) {
                    m_cPool.free(m_conn);
                }
            } catch (SQLException sqle) {
            } finally {
                m_resultSet = null;
                m_statement = null;
            }
        }
        return m_expired;
    }

    /**
     * Update object with the next chunk of results. if getToken() is null after
     * this call, the resultSet was exhausted.
     */
    protected void step() throws UnrecognizedFieldException,
            ObjectIntegrityException, RepositoryConfigurationException,
            StreamIOException, ServerException {
        m_objectFields = new ArrayList();
        int resultCount = 0;
        // Run through resultSet, adding each result to m_objectFields
        // for up to maxResults objects, or until the result set is
        // empty, whichever comes first.
        // Note: If this is the first chunk of results for the entire search,
        // m_nextPID will be null, and will require the cursor to be advanced
        // to the first result.  For all remaining chunks, the cursor will
        // already be in the correct position (and m_nextPID will have been set)
        // so a call to m_resultSet.next() is not initially necessary.
        try {
            while (resultCount < m_maxResults
                    && (m_nextPID != null || m_resultSet.next())) {
                resultCount++;
                // add the current object's info to m_objectFields
                String pid;
                if (m_nextPID == null) {
                    pid = m_resultSet.getString("pid");
                } else {
                    pid = m_nextPID;
                    m_nextPID = null;
                }
                m_objectFields.add(getObjectFields(pid));
            }
            // done with this block. now, are there more results?
            if (resultCount == m_maxResults && m_resultSet.next()) {
                // yes, and we've now advanced the cursor so we must remember
                // the pid so the next chunk can use it
                m_nextPID = m_resultSet.getString("pid");
                // generate a token, make sure the cursor is set,
                // and make sure the expirationDate is set
                long now = System.currentTimeMillis();
                m_token = MD5Utility.getBase16Hash(hashCode() + "" + now);
                m_cursor = m_nextCursor;
                // keep m_nextCursor updated for next block
                m_nextCursor += resultCount;
                m_startMillis = now;
                Date dt = new Date();
                dt.setTime(m_startMillis + 1000 * m_maxSeconds);
                m_expirationDate = dt;
            } else {
                // no, so make sure the token is null and clean up
                m_token = null;
                try {
                    if (m_resultSet != null) {
                        m_resultSet.close();
                    }
                    if (m_statement != null) {
                        m_statement.close();
                    }
                    if (m_conn != null) {
                        m_cPool.free(m_conn);
                    }
                } catch (SQLException sqle2) {
                    throw new StorageDeviceException("Error closing statement "
                            + "or result set." + sqle2.getMessage());
                } finally {
                    m_resultSet = null;
                    m_statement = null;
                }
            }
        } catch (SQLException sqle) {
            try {
                if (m_resultSet != null) {
                    m_resultSet.close();
                }
                if (m_statement != null) {
                    m_statement.close();
                }
                if (m_conn != null) {
                    m_cPool.free(m_conn);
                }
                throw new StorageDeviceException("Error with sql database. "
                        + sqle.getMessage());
            } catch (SQLException sqle2) {
                throw new StorageDeviceException("Error closing statement "
                        + "or result set." + sqle.getMessage()
                        + sqle2.getMessage());
            } finally {
                m_resultSet = null;
                m_statement = null;
            }
        }
    }

    /**
     * For the given pid, get a reader on the object from the repository and
     * return an ObjectFields object with resultFields fields populated.
     *
     * @param pid
     *        the unique identifier of the object for which the information is
     *        requested.
     * @return ObjectFields populated with the requested fields
     * @throws UnrecognizedFieldException
     *         if a resultFields value isn't valid
     * @throws ObjectIntegrityException
     *         if the underlying digital object can't be parsed
     * @throws RepositoryConfigurationException
     *         if the sax parser can't be constructed
     * @throws StreamIOException
     *         if an error occurs while reading the serialized digital object
     *         stream
     * @throws ServerException
     *         if any other kind of error occurs while reading the underlying
     *         object
     */
    private ObjectFields getObjectFields(String pid)
            throws UnrecognizedFieldException, ObjectIntegrityException,
            RepositoryConfigurationException, StreamIOException,
            ServerException {
        DOReader r =
                m_repoReader.getReader(Server.USE_DEFINITIVE_STORE,
                                       ReadOnlyContext.EMPTY,
                                       pid);
        ObjectFields f;
        // If there's a DC record available, use SAX to parse the most
        // recent version of it into f.
        DatastreamXMLMetadata dcmd = null;
        try {
            dcmd = (DatastreamXMLMetadata) r.GetDatastream("DC", null);
        } catch (ClassCastException cce) {
            throw new ObjectIntegrityException("Object " + r.GetObjectPID()
                    + " has a DC datastream, but it's not inline XML.");
        }
        if (dcmd != null) {
            f = new ObjectFields(m_resultFields, dcmd.getContentStream());
            // add dcmDate if wanted
            for (String element : m_resultFields) {
                if (element.equals("dcmDate")) {
                    f.setDCMDate(dcmd.DSCreateDT);
                }
            }
        } else {
            f = new ObjectFields();
        }
        // add non-dc values from doReader for the others in m_resultFields[]
        //        Disseminator[] disses=null;
        for (String n : m_resultFields) {
            if (n.equals("pid")) {
                f.setPid(pid);
            }
            if (n.equals("label")) {
                f.setLabel(r.GetObjectLabel());
            }
            if (n.equals("state")) {
                f.setState(r.GetObjectState());
            }
            if (n.equals("ownerId")) {
                f.setOwnerId(r.getOwnerId());
            }
            if (n.equals("cDate")) {
                f.setCDate(r.getCreateDate());
            }
            if (n.equals("mDate")) {
                f.setMDate(r.getLastModDate());
            }
        }
        return f;
    }

    public List objectFieldsList() {
        return m_objectFields;
    }

    public String getToken() {
        return m_token;
    }

    public long getCursor() {
        return m_cursor;
    }

    public long getCompleteListSize() {
        return m_completeListSize;
    }

    public Date getExpirationDate() {
        return m_expirationDate;
    }

    /**
     * Return a condition suitable for a SQL WHERE clause, given a column name
     * and a string with a possible pattern (using * and questionmark
     * wildcards).
     * <p>
     * </p>
     * If the string has any characters that need to be escaped, it will begin
     * with a space, indicating to the caller that the entire WHERE clause
     * should end with " {escape '/'}".
     *
     * @param name
     *        the name of the field in the database
     * @param in
     *        the query string, where * and ? are treated as wildcards
     * @return String a suitable string for use in a SQL WHERE clause, as
     *         described above
     */
    private static String toSql(String name, String in) {
        if (!name.endsWith("pid")) {
            in = in.toLowerCase(); // if it's not a PID-type field,
        }
        // it's case insensitive
        if (name.startsWith("dc") || name.startsWith("doFields.dc")) {
            StringBuffer newIn = new StringBuffer();
            if (!in.startsWith("*")) {
                newIn.append("* ");
            }
            newIn.append(in);
            if (!in.endsWith("*")) {
                newIn.append(" *");
            }
            in = newIn.toString();
        }
        if (in.indexOf("\\") != -1) {
            // has one or more escapes, un-escape and translate
            StringBuffer out = new StringBuffer();
            out.append("\'");
            boolean needLike = false;
            boolean needEscape = false;
            boolean lastWasEscape = false;
            for (int i = 0; i < in.length(); i++) {
                char c = in.charAt(i);
                if (!lastWasEscape && c == '\\') {
                    lastWasEscape = true;
                } else {
                    char nextChar = '!';
                    boolean useNextChar = false;
                    if (!lastWasEscape) {
                        if (c == '?') {
                            out.append('_');
                            needLike = true;
                        } else if (c == '*') {
                            out.append('%');
                            needLike = true;
                        } else {
                            nextChar = c;
                            useNextChar = true;
                        }
                    } else {
                        nextChar = c;
                        useNextChar = true;
                    }
                    if (useNextChar) {
                        if (nextChar == '\"') {
                            out.append("\\\"");
                            needEscape = true;
                        } else if (nextChar == '\'') {
                            out.append("\\\'");
                            needEscape = true;
                        } else if (nextChar == '%') {
                            out.append("\\%");
                            needEscape = true;
                        } else if (nextChar == '_') {
                            out.append("\\_");
                            needEscape = true;
                        } else {
                            out.append(nextChar);
                        }
                    }
                    lastWasEscape = false;
                }
            }
            out.append("\'");
            if (needLike) {
                out.insert(0, " LIKE ");
            } else {
                // replace any \% and \_ in value string with % or _
                String fixedString =
                        out.toString().replaceAll("\\\\%", "%")
                                .replaceAll("\\\\_", "_");
                out = new StringBuffer();
                out.append(fixedString);
                out.insert(0, " = ");
            }
            out.insert(0, name);
            if (needEscape) {
                out.insert(0, ' ');
            }
            return out.toString();
        } else {
            // no escapes, just translate if needed
            StringBuffer out = new StringBuffer();
            out.append("\'");
            boolean needLike = false;
            boolean needEscape = false;
            for (int i = 0; i < in.length(); i++) {
                char c = in.charAt(i);
                if (c == '?') {
                    out.append('_');
                    needLike = true;
                } else if (c == '*') {
                    out.append('%');
                    needLike = true;
                } else if (c == '\"') {
                    out.append("\\\"");
                    needEscape = true;
                } else if (c == '\'') {
                    out.append("\\\'");
                    needEscape = true;
                } else if (c == '%') {
                    out.append("\\%");
                    needEscape = true;
                } else if (c == '_') {
                    out.append("\\_");
                    needEscape = true;
                } else {
                    out.append(c);
                }
            }
            out.append("\'");
            if (needLike) {
                out.insert(0, " LIKE ");
            } else {
                // replace any \% and \_ in value string with % or _
                String fixedString =
                        out.toString().replaceAll("\\\\%", "%")
                                .replaceAll("\\\\_", "_");
                out = new StringBuffer();
                out.append(fixedString);
                out.insert(0, " = ");
            }
            out.insert(0, name);
            if (needEscape) {
                out.insert(0, ' ');
            }
            return out.toString();
        }
    }

    /**
     * Tell whether a field name, as given in the search request, is a dublin
     * core field.
     *
     * @param the
     *        field
     * @return whether it's a dublin core field
     */
    private static final boolean isDCProp(String in) {//2004.05.18 wdn5e wasn't static final
        if (in.equals("mDate") || in.equals("dcmDate")) {
            return false;
        }
        for (String n : FieldSearchSQLImpl.DB_COLUMN_NAMES) {
            if (n.startsWith("dc")
                    && n.toLowerCase().indexOf(in.toLowerCase()) == 2) { //2004.05.18 wdn5e (was -1)
                return true;
            }
        }
        return false;
    }

    //2004.05.18 wdn5e --  logic now needed > 1 places
    private static final String dcFixup(String st) {
        String dcFixed;
        if (isDCProp(st)) {
            dcFixed = "dc" + st.substring(0, 1).toUpperCase() + st.substring(1);
        } else {
            dcFixed = st;
        }
        return dcFixed;
    }

}
