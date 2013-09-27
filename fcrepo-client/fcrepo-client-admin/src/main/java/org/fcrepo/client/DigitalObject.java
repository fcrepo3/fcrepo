/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.client;

import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Chris Wilper
 */
public class DigitalObject {

    protected HashMap<?, BasisDataStream> basisStreams =
            new HashMap<Object, BasisDataStream>();

    protected HashMap<?, InlineDataStream> inlineStreams =
            new HashMap<Object, InlineDataStream>();

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
        Iterator<BasisDataStream> b_iter =
                basisStreams.values().iterator();
        while (b_iter.hasNext()) {
            BasisDataStream basis = b_iter.next();
            if (basis.isDirty()) {
                return true;
            }
        }
        Iterator<InlineDataStream> i_iter = inlineStreams.values().iterator();
        while (i_iter.hasNext()) {
            InlineDataStream inline = i_iter.next();
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
        Iterator<BasisDataStream> b_iter =
                basisStreams.values().iterator();
        while (b_iter.hasNext()) {
            BasisDataStream basis = b_iter.next();
            basis.setClean();
        }
        Iterator<InlineDataStream> i_iter = inlineStreams.values().iterator();
        while (i_iter.hasNext()) {
            InlineDataStream inline = i_iter.next();
            inline.setClean();
        }
    }

}
