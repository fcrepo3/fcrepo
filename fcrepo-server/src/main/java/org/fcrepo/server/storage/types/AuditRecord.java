/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.types;

import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.sun.xml.txw2.annotation.XmlElement;

/**
 * @author Chris Wilper
 */
@XmlRootElement(name="record",namespace="info:fedora/fedora-system:def/audit#")
public class AuditRecord {

    public AuditRecord() {
    }

    @XmlAttribute(name="id")
    public String id;

    @XmlAttribute(name="process-type")
    public String processType;

    @XmlAttribute(name="action")
    public String action;

    @XmlAttribute(name="componentID")
    public String componentID;

    @XmlAttribute(name="responsibility")
    public String responsibility;

    @XmlAttribute(name="date")
    public Date date;

    @XmlAttribute(name="justification")
    public String justification;
}
