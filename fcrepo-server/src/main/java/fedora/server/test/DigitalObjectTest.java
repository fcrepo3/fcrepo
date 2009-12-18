/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.test;

import java.util.Date;

import junit.framework.TestCase;

import fedora.server.storage.types.AuditRecord;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DigitalObject;

/**
 * Tests the implementation of the DigitalObject interface, BasicDigitalObject.
 * 
 * @author Chris Wilper
 */
public class DigitalObjectTest
        extends TestCase {

    private DigitalObject m_obj, m_sdef, m_sdep;

    private Date m_startTime;

    private Datastream m_ds1_0, m_ds1_1, m_ds2_0;

    private AuditRecord m_audit1, m_audit2, m_audit3, m_audit4, m_audit5,
            m_audit6, m_audit7;

    public DigitalObjectTest(String label) {
        super(label);
    }

    @Override
    public void setUp() {
        // init common values
        m_startTime = new Date();
        // init data object
        m_obj = new BasicDigitalObject();
        m_obj.setCreateDate(m_startTime);
        //m_obj.addModel(Models.FEDORA_OBJECT_3_0.uri);
        m_obj.setLabel("Test Object");
        m_obj.setLastModDate(m_startTime);
        m_obj.setOwnerId("userId1");
        m_obj.setPid("test:1");
        m_obj.setState("A");
        // add some datastreams
        m_ds1_0 = new Datastream();
        m_ds1_0.DatastreamID = "DS1.0";
        m_ds1_0.DSVersionID = "DS1";
        m_ds1_1 = new Datastream();
        m_ds1_1.DatastreamID = "DS1.1";
        m_ds1_1.DSVersionID = "DS1";
        m_ds2_0 = new Datastream();
        m_ds2_0.DatastreamID = "DS2.0";
        m_ds2_0.DSVersionID = "DS2";
        // ... and some audit records
        m_audit1 = new AuditRecord();
        m_audit1.id = "AUDIT1";
        m_audit1.action = "Object Created";
        m_audit2 = new AuditRecord();
        m_audit2.id = "AUDIT2";
        m_audit2.action = "Datastream 1 Added";
        m_audit3 = new AuditRecord();
        m_audit3.id = "AUDIT3";
        m_audit3.action = "Datastream 1 Versioned";
        m_audit4 = new AuditRecord();
        m_audit4.id = "AUDIT4";
        m_audit4.action = "Datastream 2 Added";
        m_audit5 = new AuditRecord();
        m_audit5.id = "AUDIT5";
        m_audit5.action = "Disseminator 1 Added";
        m_audit6 = new AuditRecord();
        m_audit6.id = "AUDIT6";
        m_audit6.action = "Disseminator 1 Versioned";
        m_audit7 = new AuditRecord();
        m_audit7.id = "AUDIT7";
        m_audit7.action = "Disseminator 2 Added";
        // init sdef
        m_sdef = new BasicDigitalObject();
        m_sdef.setCreateDate(m_startTime);
        m_sdef.setLabel("Test Service Definition Object");
        m_sdef.setLastModDate(m_startTime);
        m_sdef.setOwnerId("userId2");
        m_sdef.setPid("test:2");
        m_sdef.setState("W");
        m_sdep = new BasicDigitalObject();
        m_sdep.setCreateDate(m_startTime);
        m_sdep.setLabel("Test Service Deployment Object");
        m_sdep.setLastModDate(m_startTime);
        m_sdep.setOwnerId("userId3");
        m_sdep.setPid("test:3");
        m_sdep.setState("D");
    }

    public void testSimpleParts() {
        assertEquals(m_obj.getCreateDate(), m_startTime);
        assertEquals(m_sdef.getCreateDate(), m_startTime);
        assertEquals(m_sdep.getCreateDate(), m_startTime);
        assertEquals(m_obj.getLabel(), "Test Object");
        assertEquals(m_sdef.getLabel(), "Test Service Deployment Object");
        assertEquals(m_sdep.getLabel(), "Test Service Deployment Object");
        assertEquals(m_obj.getLastModDate(), m_startTime);
        assertEquals(m_sdef.getLastModDate(), m_startTime);
        assertEquals(m_sdep.getLastModDate(), m_startTime);
        assertEquals(m_obj.getOwnerId(), "userId1");
        assertEquals(m_sdef.getOwnerId(), "userId2");
        assertEquals(m_sdep.getOwnerId(), "userId3");
        assertEquals(m_obj.getPid(), "test:1");
        assertEquals(m_sdef.getPid(), "test:2");
        assertEquals(m_sdep.getPid(), "test:3");
        assertEquals(m_obj.getState(), "A");
        assertEquals(m_sdef.getState(), "W");
        assertEquals(m_sdep.getState(), "D");
    }

    public void testAuditRecordComposition() {
        m_obj.getAuditRecords().add(m_audit1);
    }

    public void testDatastreamComposition() {
        m_obj.getAuditRecords().add(m_audit2);
        m_obj.addDatastreamVersion(m_ds1_0, true);

        m_obj.getAuditRecords().add(m_audit3);
        m_obj.addDatastreamVersion(m_ds1_1, true);
        
        m_obj.getAuditRecords().add(m_audit4);
        m_obj.addDatastreamVersion(m_ds2_0, true);
    }

    public void testDisseminatorComposition() {
    }

}
