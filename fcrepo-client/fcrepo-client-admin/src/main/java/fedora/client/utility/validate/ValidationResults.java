/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate;

/**
 * Specifies the results accumulator for the {@link ObjectValidator}.
 * 
 * @author Jim Blake
 */
public interface ValidationResults {

    /**
     * Record a {@link ValidationResult} that is the result of validating an
     * object.
     * 
     * @throws IllegalStateException
     *         if {@link #closeResults()} has already been called.
     */
    void record(ValidationResult result);

    /**
     * Print, summarize, store, or display the accumulated results. This should
     * be called once, after all calls to {@link #record(ValidationResult)} have
     * been made.
     */
    void closeResults();
}
