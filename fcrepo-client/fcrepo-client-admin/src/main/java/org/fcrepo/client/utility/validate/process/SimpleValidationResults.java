package org.fcrepo.client.utility.validate.process;

import org.fcrepo.client.utility.validate.ValidationResult;
import org.fcrepo.client.utility.validate.ValidationResultNotation;
import org.fcrepo.client.utility.validate.ValidationResults;

/**
 * A simple implementation of {@link ValidationResults} for use with the
 * {@link ValidatorProcess}. When {@link #record(ValidationResult)} is called,
 * the result is printed to standard output.
 *
 * @author Chris Wilper
 */
public class SimpleValidationResults
        implements ValidationResults {

    private final boolean verbose;

    private long indeterminateCount;
    private long invalidCount;
    private long validCount;

    /**
     * Creates an instance.
     *
     * @param verbose if true, all INFO level messages will be printed
     *                in addition to WARNings and ERRORs.
     */
    public SimpleValidationResults(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void record(ValidationResult result) {
        ValidationResult.Level level = result.getSeverityLevel();
        String kind;
        switch (level) {
            case ERROR:
                invalidCount++;
                kind = "Invalid";
                break;
            case WARN:
                indeterminateCount++;
                kind = "Indeterminate";
                break;
            default:
                validCount++;
                kind = "Valid";
                break;
        }
        if (verbose || level != ValidationResult.Level.INFO) {
            System.out.println(kind + " object: " + result.getObject().getPid());
            for (ValidationResultNotation note: result.getNotes()) {
                if (verbose && note.getLevel() != ValidationResult.Level.INFO) {
                    System.out.println("  " + note.getLevel() + ":"
                            + note.getCategory() + " " + note.getMessage());
                }
            }
        }
    }

    @Override
    public void closeResults() {
        System.out.println();
        System.out.println("Validation Summary");
        System.out.println("------------------");
        long totalCount = invalidCount + indeterminateCount + validCount;
        System.out.println("Total Objects: " + totalCount);
        if (validCount > 0) {
            System.out.println("Valid Objects: " + validCount);
        }
        if (invalidCount > 0) {
            System.out.println("Invalid Objects: " + invalidCount);
        }
        if (indeterminateCount > 0) {
            System.out.println("Indeterminate Objects: " + indeterminateCount);
        }
    }

}
