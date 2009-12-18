/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.search;

import java.util.ArrayList;
import java.util.List;

import fedora.server.errors.InvalidOperatorException;
import fedora.server.errors.QueryParseException;

/**
 * @author Chris Wilper
 */
public class Condition {

    private final String m_property;

    private final Operator m_operator;

    private final String m_value;

    public Condition(String property, Operator operator, String value)
            throws QueryParseException {
        m_property = property;
        m_operator = operator;
        if (value.indexOf("'") != -1) {
            throw new QueryParseException("Query cannot contain the ' character.");
        }
        m_value = value;
    }

    public Condition(String property, String operator, String value)
            throws InvalidOperatorException, QueryParseException {
        m_property = property;
        m_operator = Operator.fromAbbreviation(operator);
        if (value.indexOf("'") != -1) {
            throw new QueryParseException("Query cannot contain the ' character.");
        }
        m_value = value;
    }

    /**
     * Gets a List of Conditions from a string like: a=x b~'that\'s' c>='z'
     * 
     * @param query
     *        The query string.
     * @return The Conditions.
     */
    public static List<Condition> getConditions(String query)
            throws QueryParseException {
        StringBuffer prop = new StringBuffer();
        Operator oper = null;
        StringBuffer val = new StringBuffer();
        ArrayList<Condition> ret = new ArrayList<Condition>();
        boolean inProp = true;
        boolean inValue = false;
        boolean firstValueChar = false;
        boolean valueStartsWithQuote = false;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (inProp) {
                if (c == ' ') {
                    throw new QueryParseException("Found <space> at character "
                            + i + " but expected <operator> or <alphanum>");
                } else if (c == '=') {
                    oper = Operator.EQUALS;
                    inProp = false;
                    inValue = true;
                    firstValueChar = true;
                } else if (c == '~') {
                    oper = Operator.CONTAINS;
                    inProp = false;
                    inValue = true;
                    firstValueChar = true;
                } else if (c == '>') {
                    if (i + 1 < query.length()) {
                        char d = query.charAt(i + 1);
                        if (d == '=') {
                            i++;
                            oper = Operator.GREATER_OR_EQUAL;
                        } else {
                            oper = Operator.GREATER_THAN;
                        }
                        inProp = false;
                        inValue = true;
                        firstValueChar = true;
                    } else {
                        throw new QueryParseException("Found <end-of-string> "
                                + "immediately following '>' operator, but "
                                + "expected a value.");
                    }
                } else if (c == '<') {
                    if (i + 1 < query.length()) {
                        char d = query.charAt(i + 1);
                        if (d == '=') {
                            i++;
                            oper = Operator.LESS_OR_EQUAL;
                        } else {
                            oper = Operator.LESS_THAN;
                        }
                        inProp = false;
                        inValue = true;
                        firstValueChar = true;
                    } else {
                        throw new QueryParseException("Found <end-of-string> "
                                + "immediately following '<' operator, but "
                                + "expected a value.");
                    }
                } else {
                    prop.append(c);
                }
            } else if (inValue) {
                if (prop.toString().length() == 0) {
                    throw new QueryParseException("Found "
                            + "operator but expected a non-zero length "
                            + "property.");
                }
                if (firstValueChar) {
                    // allow ', and mark it if it's there, add one to i
                    if (c == '\'') {
                        i++;
                        if (i >= query.length()) {
                            throw new QueryParseException("Found <end-of-string> "
                                    + "immediately following start quote, but "
                                    + "expected a value.");
                        }
                        c = query.charAt(i);
                        valueStartsWithQuote = true;
                    }
                    firstValueChar = false;
                }
                if (c == '\'') {
                    if (!valueStartsWithQuote) {
                        throw new QueryParseException("Found ' character in "
                                + "value at position " + i + ", but the value "
                                + "did not start with a string, so this can't "
                                + " be a value terminator.");
                    }
                    // end of value part
                    // next must be space or empty... check
                    i++;
                    if (i < query.length()) {
                        if (query.charAt(i) != ' ') {
                            throw new QueryParseException("Found value-terminator "
                                    + "' but it was not followed by <end-of-string> "
                                    + "or <space>.");
                        }
                    }
                    ret
                            .add(new Condition(prop.toString(), oper, val
                                    .toString()));
                    prop = new StringBuffer();
                    oper = null;
                    val = new StringBuffer();
                    inValue = false;
                    inProp = true;
                    valueStartsWithQuote = false;
                } else if (c == '\\') {
                    i++;
                    if (i >= query.length()) {
                        throw new QueryParseException("Found character-escaping "
                                + "character as last item in string.");
                    }
                    val.append(query.charAt(i));
                } else if (c == ' ') {
                    // end of value part... or inside string?
                    if (valueStartsWithQuote) {
                        // was inside string..ok
                        val.append(c);
                    } else {
                        // end of value part...cuz not quotes
                        ret.add(new Condition(prop.toString(), oper, val
                                .toString()));
                        prop = new StringBuffer();
                        oper = null;
                        val = new StringBuffer();
                        inValue = false;
                        inProp = true;
                    }
                } else if (c == '=') {
                    throw new QueryParseException("Found <operator> at position "
                            + i + ", but expected <value>");
                } else if (c == '~') {
                    throw new QueryParseException("Found <operator> at position "
                            + i + ", but expected <value>");
                } else if (c == '>') {
                    throw new QueryParseException("Found <operator> at position "
                            + i + ", but expected <value>");
                } else if (c == '<') {
                    throw new QueryParseException("Found <operator> at position "
                            + i + ", but expected <value>");
                } else {
                    val.append(c);
                }
            }
        }
        if (inProp) {
            if (prop.toString().length() > 0) {
                throw new QueryParseException("String ended before operator "
                        + "was found");
            }
        }
        if (inValue) {
            if (valueStartsWithQuote) {
                throw new QueryParseException("String ended before quoted value"
                        + "'s ending quote.");
            }
            ret.add(new Condition(prop.toString(), oper, val.toString()));
        }
        return ret;
    }

    public String getProperty() {
        return m_property;
    }

    public Operator getOperator() {
        return m_operator;
    }

    public String getValue() {
        return m_value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!o.getClass().equals(this.getClass())) {
            return false;
        }

        Condition that = (Condition) o;
        return equivalent(m_property, that.m_property)
                && equivalent(m_value, that.m_value)
                && equivalent(m_operator, that.m_operator);
    }

    @Override
    public int hashCode() {
        return m_property.hashCode() ^ m_operator.hashCode()
                ^ m_value.hashCode();
    }

    @Override
    public String toString() {
        return "Condition[" + m_property + m_operator + m_value + "]";
    }

    private boolean equivalent(Object one, Object two) {
        return one == null ? two == null : one.equals(two);
    }

}
