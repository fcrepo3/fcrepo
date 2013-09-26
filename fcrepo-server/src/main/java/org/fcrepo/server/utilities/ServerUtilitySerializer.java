/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.Date;

/**
 * Used to write output from ServerUtility to a stream
 * Just basic log-style output
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public final class ServerUtilitySerializer {

    PrintWriter m_writer = null;

    private String m_curpid;

    private String m_curdsID;

    private int m_objectCount = 0;
    private int m_datastreamCount = 0;
    private int m_datastreamCountThisObject = 0;

    private static final int INDENT = 4;

    @SuppressWarnings("unused")
    private ServerUtilitySerializer() {}

    public ServerUtilitySerializer(PrintWriter pw) throws IOException {
        m_writer = new PrintWriter(pw);
        start();
    }

    private void indent(int level) throws IOException {
        SpaceCharacters.indent(level * INDENT, m_writer);
    }

    private void start() throws IOException {
        m_writer.println("Updating managed content datastreams");
    }
    public void finish() throws IOException {
        m_writer.println("Finished updating managed content datastreams");
        m_writer.println("Updated " + m_objectCount + " objects and " + m_datastreamCount + " datastreams");
    }

    public void startObject(String pid) throws IOException {
        m_curpid = pid;
        indent(1);
        m_writer.println("Updating object " + pid);
    }
    public void endObject() throws IOException {
        indent(1);
        m_writer.println("Finished updating object " + m_curpid);

    }
    public void startDatastream(String dsID) throws IOException {
        m_datastreamCountThisObject = 0;
        m_curdsID = dsID;
        indent(2);
        m_writer.println("Updating datastream " + dsID);

    }
    public void endDatastream() throws IOException {
        m_datastreamCount = m_datastreamCount + m_datastreamCountThisObject;
        if (m_datastreamCountThisObject > 0)
            m_objectCount ++;
        indent(2);
        m_writer.println("Finished updating datastream" + m_curdsID);

    }
    public void writeVersions(Date[] versions) throws IOException {
        indent(3);
        if (versions == null) {
            m_writer.println("Datastream " + m_curdsID + " not found in this object");
            m_datastreamCountThisObject = 0;
        } else {
            if (versions.length == 0) {
                m_writer.println("Datastream already has desired ControlGroup)");

            } else {
                for (Date version : versions) {
                    indent(4);
                    m_writer.println("Updated version " + version.toString());
                }
            }
            m_datastreamCountThisObject = versions.length;
        }
    }
}
