/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {TestDOTranslatorImpl.class,
        TestFOXML1_0DOSerializer.class, TestFOXML1_0DODeserializer.class,
        TestFOXML1_1DOSerializer.class, TestFOXML1_1DODeserializer.class,
        TestMETSFedoraExt1_0DOSerializer.class,
        TestMETSFedoraExt1_0DODeserializer.class,
        TestMETSFedoraExt1_1DOSerializer.class,
        TestMETSFedoraExt1_1DODeserializer.class,
        TestAtomDOSerializer.class,
        TestAtomDODeserializer.class})

public class AllUnitTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllUnitTests.class.getName());

        suite.addTest(TestDOTranslatorImpl.suite());

        suite.addTest(TestFOXML1_0DOSerializer.suite());
        suite.addTest(TestFOXML1_0DODeserializer.suite());

        suite.addTest(TestFOXML1_1DOSerializer.suite());
        suite.addTest(TestFOXML1_1DODeserializer.suite());

        suite.addTest(TestMETSFedoraExt1_0DOSerializer.suite());
        suite.addTest(TestMETSFedoraExt1_0DODeserializer.suite());

        suite.addTest(TestMETSFedoraExt1_1DOSerializer.suite());
        suite.addTest(TestMETSFedoraExt1_1DODeserializer.suite());

        suite.addTest(TestAtomDOSerializer.suite());
        suite.addTest(TestAtomDODeserializer.suite());

        return suite;
    }
}
