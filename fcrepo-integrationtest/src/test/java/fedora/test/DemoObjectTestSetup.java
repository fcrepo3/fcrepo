/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test;

import junit.extensions.TestSetup;
import junit.framework.Test;

import fedora.common.FedoraTestConstants;

public class DemoObjectTestSetup
        extends TestSetup
        implements FedoraTestConstants {

    public DemoObjectTestSetup(Test test) {
        super(test);
    }

    @Override
    public void setUp() throws Exception {
        System.out.println("Ingesting demo objects...");
        FedoraServerTestCase.ingestDemoObjects();
    }

    @Override
    public void tearDown() throws Exception {
        System.out.println("Purging demo objects...");
        FedoraServerTestCase.purgeDemoObjects();
    }
}
