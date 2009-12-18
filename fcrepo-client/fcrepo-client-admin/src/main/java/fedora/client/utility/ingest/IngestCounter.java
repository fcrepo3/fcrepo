/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.utility.ingest;

/**
 * Class to hold counts of ingest failures and successes. This
 * class will be passed as a by reference argument to methods of the Ingest
 * utility class.
 * 
 * @author Chris Wilper
 */
public class IngestCounter {

    public int successes;

    public int failures;

    public IngestCounter() {
        successes = 0;
        failures = 0;
    }

    public int getTotal() {
        return successes + failures;
    }
}