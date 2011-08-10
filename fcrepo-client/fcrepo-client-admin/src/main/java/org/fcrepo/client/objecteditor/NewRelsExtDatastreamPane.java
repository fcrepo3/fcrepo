/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.client.objecteditor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Arrays;

import org.fcrepo.client.Administrator;

import org.fcrepo.common.Constants;

import org.fcrepo.server.types.mtom.gen.Datastream;
import org.fcrepo.server.types.mtom.gen.DatastreamControlGroup;
import org.fcrepo.server.utilities.TypeUtility;




class NewRelsExtDatastreamPane
        extends DatastreamPane {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected static String s_dsid = "RELS-EXT";

    public NewRelsExtDatastreamPane(ObjectEditorFrame gramps,
                                    String pid,
                                    DatastreamsPane owner)
            throws Exception {
        super(gramps, pid, Arrays.asList(MakeBlankRelsExtDatastream()), owner);
        m_undoButton.setVisible(false);
        m_saveButton.setEnabled(true);
        m_saveButton.setText("Save Datastream");
        m_saveButton.setPreferredSize(null);
        Administrator.constrainHeight(m_saveButton);
        m_currentVersionPane.m_MIMETextField.setEditable(false);
        m_currentVersionPane.m_actionPane
                .remove(m_currentVersionPane.m_exportButton);
        m_currentVersionPane.m_actionPane
                .remove(m_currentVersionPane.m_purgeButton);
        m_currentVersionPane.m_checksumTypeComboBox.insertItemAt("Default", 0);
        revalidate();
        m_currentVersionPane.m_editCustomButton.doClick();
    }

    private static Datastream[] MakeBlankRelsExtDatastream() {
        Datastream[] ds = new Datastream[1];
        ds[0] = new Datastream();
        ds[0].setControlGroup(DatastreamControlGroup.fromValue("X"));
        ds[0].setID("s_dsid");
        ds[0].setVersionID("RELS-EXT.0");
        ds[0].setLabel("RDF Statements about this object");
        ds[0].setVersionable(true);
        ds[0].setMIMEType("application/rdf+xml");
        ds[0].setFormatURI(Constants.RELS_EXT1_0.uri);
        ds[0].setSize(0L);
        ds[0].setState("A");
        return ds;
    }

    @Override
    public InputStream getDatastreamContent(String pid,
                                            String id,
                                            String createDate)
            throws IOException {
        String initialContent =
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rel=\"info:fedora/fedora-system:def/relations-external#\">"
                        + "    <rdf:Description rdf:about=\"info:fedora/"
                        + pid
                        + "\">" + "    </rdf:Description>" + "</rdf:RDF>";
        ByteArrayInputStream is =
                new ByteArrayInputStream(initialContent.getBytes());
        return is;
    }

    @Override
    public void updateButtonVisibility() {
        // do nothing, simply override super class implementation
    }

    @Override
    public void saveChanges(String logMessage) throws Exception {
        if (m_currentVersionPane.isDirty()) {
            // defer to the currentVersionPane if anything else changed
            try {
                String state =
                        s_stateComboBoxValues[m_stateComboBox
                                .getSelectedIndex()];
                String label =
                        m_currentVersionPane.m_labelTextField.getText().trim();
                String mimeType =
                        m_currentVersionPane.m_MIMETextField.getText().trim();
                String formatURI =
                        m_currentVersionPane.m_formatURITextField.getText()
                                .trim();
                String[] altIDs =
                        m_currentVersionPane.m_altIDsTextField.getText().trim()
                                .split(" ");
                String checksumType =
                        m_currentVersionPane.m_checksumTypeComboBox
                                .getSelectedItem().toString();
                if (checksumType.equals("Default")) {
                    checksumType = null;
                }
                String location = null;
                location =
                        Administrator.UPLOADER
                                .upload(m_currentVersionPane.m_editor
                                        .getContent());
                boolean versionable =
                        m_versionableComboBox.getSelectedIndex() == NEW_VERSION_ON_UPDATE ? true
                                : false;
                String newID =
                        Administrator.APIM.addDatastream(m_pid,
                                                         s_dsid,
                                                         TypeUtility.convertStringtoAOS(altIDs),
                                                         label,
                                                         versionable, // DEFAULT_VERSIONABLE
                                                         mimeType,
                                                         formatURI,
                                                         location,
                                                         "X",
                                                         state,
                                                         checksumType,
                                                         null, // checksum type and checksum
                                                         logMessage); // DEFAULT_LOGMESSAGE

            } catch (Exception e) {
                throw e;
            }
            m_owner.addDatastreamTab(s_dsid, false);
        }
    }
}
