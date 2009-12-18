/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Chris Wilper
 */
public class DigitalObject {

    protected HashMap basisStreams = new HashMap();

    protected HashMap inlineStreams = new HashMap();

    private boolean m_dirty = true;

    private String m_name = "Untitled";

    public DigitalObject() {
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_dirty = true;
        m_name = name;
    }

    public boolean isDirty() {
        if (m_dirty) {
            return true;
        }
        Iterator iter;
        iter = basisStreams.values().iterator();
        while (iter.hasNext()) {
            BasisDataStream basis = (BasisDataStream) iter.next();
            if (basis.isDirty()) {
                return true;
            }
        }
        iter = inlineStreams.values().iterator();
        while (iter.hasNext()) {
            InlineDataStream inline = (InlineDataStream) iter.next();
            if (inline.isDirty()) {
                return true;
            }
        }
        return false;
    }

    public void setClean() {
        m_dirty = false;
    }

    public void setAllClean() {
        m_dirty = false;
        Iterator iter;
        iter = basisStreams.values().iterator();
        while (iter.hasNext()) {
            BasisDataStream basis = (BasisDataStream) iter.next();
            basis.setClean();
        }
        iter = inlineStreams.values().iterator();
        while (iter.hasNext()) {
            InlineDataStream inline = (InlineDataStream) iter.next();
            inline.setClean();
        }
    }

}
