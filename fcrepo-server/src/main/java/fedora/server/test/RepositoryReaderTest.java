/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.test;

import fedora.common.Constants;
import fedora.common.Models;
import fedora.server.Server;
import fedora.server.storage.DOReader;
import fedora.server.storage.DirectoryBasedRepositoryReader;
import fedora.server.storage.ServiceDefinitionReader;
import fedora.server.storage.ServiceDeploymentReader;
import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.translation.DOSerializer;
import fedora.server.storage.translation.DOTranslatorImpl;
import fedora.server.storage.translation.METSFedoraExt1_1DODeserializer;
import fedora.server.storage.translation.METSFedoraExt1_1DOSerializer;
import junit.framework.TestCase;

import java.io.File;
import java.util.HashMap;

/**
 * Tests the implementation of the RepositoryReader interface,
 * DirectoryBasedRepositoryReader.
 * 
 * @author Chris Wilper
 */
public class RepositoryReaderTest
        extends TestCase
        implements Constants {

    private final File m_repoDir;

    private DirectoryBasedRepositoryReader m_repoReader;

    public RepositoryReaderTest(String fedoraHome, String label) {
        super(label);
        m_repoDir = new File(new File(fedoraHome), "demo");
    }

    @Override
    public void setUp() {
        try {
            String mets = METS_EXT1_1.uri;
            HashMap<String, DOSerializer> sers = new HashMap<String, DOSerializer>();
            sers.put(mets, new METSFedoraExt1_1DOSerializer());
            HashMap<String, DODeserializer> desers = new HashMap<String, DODeserializer>();
            desers.put(mets, new METSFedoraExt1_1DODeserializer());
            DOTranslatorImpl translator = new DOTranslatorImpl(sers, desers);
            m_repoReader =
                    new DirectoryBasedRepositoryReader(m_repoDir,
                                                       translator,
                                                       mets,
                                                       mets,
                                                       "UTF-8");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getName() + ": "
                    + e.getMessage());
        }
    }

    public void testList() {
        try {
            String[] pids = m_repoReader.listObjectPIDs(null);
            System.out.println("Repository has " + pids.length + " objects.");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getName() + ": "
                    + e.getMessage());
        }
    }

    public void testGetReader() {
        try {
            String[] pids = m_repoReader.listObjectPIDs(null);
            for (String element : pids) {
                DOReader r =
                        m_repoReader.getReader(Server.USE_DEFINITIVE_STORE,
                                               null,
                                               element);
                System.out.println(r.GetObjectPID() + " found via DOReader.");
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getName() + ": "
                    + e.getMessage());
        }
    }

    public void testGetSDefReader() {
        try {
            String[] pids = m_repoReader.listObjectPIDs(null);
            for (String element : pids) {
                DOReader r =
                        m_repoReader.getReader(Server.USE_DEFINITIVE_STORE,
                                               null,
                                               element);
                if (r.hasContentModel(
                                      Models.SERVICE_DEPLOYMENT_3_0)) {
                    ServiceDefinitionReader dr =
                            m_repoReader
                                    .getServiceDefinitionReader(Server.USE_DEFINITIVE_STORE,
                                                                null,
                                                                element);
                    System.out.println(dr.GetObjectPID()
                            + " found via getSDefReader.");
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getName() + ": "
                    + e.getMessage());
        }
    }

    public void testGetSDepReader() {
        try {
            String[] pids = m_repoReader.listObjectPIDs(null);
            for (String element : pids) {
                DOReader r =
                        m_repoReader.getReader(Server.USE_DEFINITIVE_STORE,
                                               null,
                                               element);
                if (r.hasContentModel(
                                      Models.SERVICE_DEPLOYMENT_3_0)) {
                    ServiceDeploymentReader mr =
                            m_repoReader
                                    .getServiceDeploymentReader(Server.USE_DEFINITIVE_STORE,
                                                                null,
                                                                element);
                    System.out.println(mr.GetObjectPID()
                            + " found via getSDepReader.");
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getClass().getName() + ": "
                    + e.getMessage());
        }
    }

    public static void main(String[] args) {
        RepositoryReaderTest test =
                new RepositoryReaderTest(Constants.FEDORA_HOME,
                                         "Testing DirectoryBasedRepositoryReader");
        test.setUp();
        test.testList();
        test.testGetReader();
        test.testGetSDefReader();
        test.testGetSDepReader();
    }

}