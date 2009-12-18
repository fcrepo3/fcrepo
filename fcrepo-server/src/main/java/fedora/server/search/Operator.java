/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.search;

import fedora.server.errors.InvalidOperatorException;

/**
 * The {@link Operator}s that can be used in a {@link FieldSearchQuery}.
 * 
 * @author Jim Blake
 */
public enum Operator {
    EQUALS("=", "eq"), CONTAINS("~", "has"), GREATER_THAN(">", "gt"),
    GREATER_OR_EQUAL(">=", "ge"), LESS_THAN("<", "lt"), LESS_OR_EQUAL("<=",
            "le");

    private final String symbol;

    private final String abbreviation;

    private Operator(String symbol, String abbreviation) {
        this.symbol = symbol;
        this.abbreviation = abbreviation;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public static Operator fromAbbreviation(String abbreviation)
            throws InvalidOperatorException {
        for (Operator operator : Operator.values()) {
            if (operator.abbreviation.equals(abbreviation)) {
                return operator;
            }
        }
        throw new InvalidOperatorException("Operator, '" + abbreviation
                + "' does not match one of eq, has, gt, ge, lt, or le.");

    }

    @Override
    public String toString() {
        return name() + "[" + symbol + ", " + abbreviation + "]";
    }

}
