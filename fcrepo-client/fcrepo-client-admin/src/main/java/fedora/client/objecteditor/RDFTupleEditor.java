/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.objecteditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;

import org.jrdf.graph.Literal;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.Triple;

import org.trippi.RDFFormat;
import org.trippi.TripleIterator;
import org.trippi.TrippiException;

import fedora.client.Administrator;

import fedora.common.Constants;
import fedora.common.PID;

import fedora.server.storage.types.RelationshipTuple;
import fedora.server.storage.types.TupleArrayTripleIterator;

import static fedora.common.Constants.MODEL;

/**
 * An RDF editor/viewer.
 */
public class RDFTupleEditor
        extends ContentEditor
        implements DocumentListener, ActionListener, PropertyChangeListener {

    /** This class handles the RDF MIME type. */
    public static String[] s_types = new String[] {"application/rdf+xml"};

    protected boolean m_dirty;

    protected ActionListener m_dataChangeListener;

    protected JTable m_editor;

    protected JScrollPane m_scrollPane;

    protected JPanel m_component;

    protected RDFDataModel m_origContent;

    protected RDFDataModel m_dataModel;

    protected boolean m_isEditable;

    protected JButton m_add;

    protected JButton m_edit;

    protected JButton m_delete;

    protected String pid;

    protected String dsid; // not used

    protected HashMap<String, String> m_map;

    private static boolean s_registered = false;

    public RDFTupleEditor() {
        if (!s_registered) {
            ContentHandlerFactory.register(this);
            s_registered = true;
        }
    }

    @Override
    public String[] getTypes() {
        return s_types;
    }

    @Override
    public void setPIDAndDSID(String pid, String dsid) {
        this.pid = pid;
        this.dsid = dsid;
    }

    @Override
    public void init(String type, InputStream data, boolean viewOnly)
            throws IOException {
        m_editor = new JTable() {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                super.valueChanged(e);
                firePropertyChange("selection", e.getFirstIndex(), -1);
            }
        };
        m_editor.setFont(new Font("monospaced", Font.PLAIN, 12));
        setContent(data);
        m_isEditable = !viewOnly;
        m_scrollPane = new JScrollPane(m_editor);
        m_component = new JPanel();
        m_component.setLayout(new BorderLayout());
        m_component.add(m_scrollPane, BorderLayout.CENTER);
        m_editor.addPropertyChangeListener("selection", this);

        m_map = new HashMap<String, String>();
        m_map.put(Constants.RELS_EXT.prefix, Constants.RELS_EXT.uri);
        m_map.put(Constants.MODEL.prefix, Constants.MODEL.uri);
        m_map.put(Constants.RDF.prefix, Constants.RDF.uri);

        // Lay out the buttons from left to right.
        if (!viewOnly) {
            JPanel buttonPane = new JPanel();
            buttonPane
                    .setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
            buttonPane
                    .setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            buttonPane.add(Box.createHorizontalGlue());
            buttonPane.add(m_add = MakeButton("Add...", this));
            buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
            buttonPane.add(m_edit = MakeButton("Edit...", this));
            buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
            buttonPane.add(m_delete = MakeButton("Delete", this));
            buttonPane.add(Box.createHorizontalGlue());
            m_edit.setEnabled(false);
            m_delete.setEnabled(false);
            m_component.add(buttonPane, BorderLayout.SOUTH);
        }
    }

    static private JButton MakeButton(String label, ActionListener listener) {
        JButton button = new JButton(label);
        button.setActionCommand(label);
        button.addActionListener(listener);
        Administrator.constrainHeight(button);
        return button;
    }

    @Override
    public void setContent(InputStream data) throws IOException {
        // get a string from the inputstream, assume it's UTF-8
        m_dataModel = new RDFDataModel(data);
        m_editor.setModel(m_dataModel);
        if (m_origContent == null) {
            m_origContent = m_dataModel.clone();
        }
    }

    @Override
    public JComponent getComponent() {
        return m_component;
    }

    @Override
    public void changesSaved() {
        m_origContent = m_dataModel.clone();
        dataChanged();
    }

    @Override
    public void undoChanges() {
        m_dataModel = m_origContent.clone();
        m_editor.setModel(m_dataModel);
        dataChanged();
    }

    @Override
    public boolean isDirty() {
        return !m_origContent.serializeAsString().equals(m_dataModel
                .serializeAsString());
    }

    @Override
    public void setContentChangeListener(ActionListener listener) {
        m_dataChangeListener = listener;
    }

    @Override
    public InputStream getContent() throws IOException {
        return m_dataModel.serializeAsStream();
    }

    // Forward DocumentListener's events to the passed-in ActionListener
    public void changedUpdate(DocumentEvent e) {
        dataChanged();
    }

    public void insertUpdate(DocumentEvent e) {
        dataChanged();
    }

    public void removeUpdate(DocumentEvent e) {
        dataChanged();
    }

    private void dataChanged() {
        if (m_dataChangeListener != null) {
            m_dataChangeListener
                    .actionPerformed(new ActionEvent(this, 0, "dataChanged"));
        }
        m_editor.revalidate();
        m_editor.repaint();
    }

    class RDFDataModel
            extends AbstractTableModel {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        // Create columns names
        String columnNames[] = {"Subject", "Predicate", "Object"};

        ArrayList<RelationshipTuple> entries = null;

        private RDFDataModel() {
        } // only for use by clone

        public RDFDataModel(InputStream data) {
            TripleIterator iter;
            try {
                iter = TripleIterator.fromStream(data, RDFFormat.RDF_XML);
                entries = new ArrayList<RelationshipTuple>();
                for (int i = 0; iter.hasNext(); i++) {
                    Triple triple = iter.next();
                    String object = null;
                    boolean isLiteral = false;
                    String datatype = null;
                    ObjectNode oNode = triple.getObject();
                    if (oNode instanceof Literal) {
                        isLiteral = true;
                        URI typeURI = ((Literal) oNode).getDatatypeURI();
                        datatype = typeURI == null ? null : typeURI.toString();
                    }
                    object = oNode.toString();
                    entries.add(new RelationshipTuple(triple.getSubject()
                                                              .toString(),
                                                      triple.getPredicate()
                                                              .toString(),
                                                      object,
                                                      isLiteral,
                                                      datatype));

                }
            } catch (TrippiException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public RDFDataModel clone() {
            RDFDataModel clone = new RDFDataModel();
            clone.entries = (ArrayList<RelationshipTuple>) entries.clone();
            return clone;
        }

        public void deleteRow(int i) {
            entries.remove(i);
            dataChanged();
        }

        public void addRow(String subject,
                           String predicate,
                           String object,
                           boolean isLiteral,
                           String datatype) {
            entries.add(new RelationshipTuple(subject,
                                              predicate,
                                              object,
                                              isLiteral,
                                              datatype));
            dataChanged();
        }

        public void replaceRow(int selectedRow,
                               String subject,
                               String predicate,
                               String object,
                               boolean isLiteral,
                               String datatype) {
            entries.set(selectedRow, new RelationshipTuple(subject,
                                                           predicate,
                                                           object,
                                                           isLiteral,
                                                           datatype));
            dataChanged();
        }

        public InputStream serializeAsStream() {
            TupleArrayTripleIterator iter =
                    new TupleArrayTripleIterator(entries);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                iter.toStream(os, RDFFormat.RDF_XML, false);
            } catch (TrippiException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            ByteArrayInputStream is =
                    new ByteArrayInputStream(os.toByteArray());
            return is;
        }

        public String serializeAsString() {
            TupleArrayTripleIterator iter =
                    new TupleArrayTripleIterator(entries);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                iter.toStream(os, RDFFormat.RDF_XML, false);
            } catch (TrippiException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String results = new String(os.toByteArray());
            return results;
        }

        public Object getValueAt(int iRowIndex, int iColumnIndex) {
            RelationshipTuple tuple = entries.get(iRowIndex);
            switch (iColumnIndex) {
                case 0:
                    return tuple.subject;
                case 1:
                    return tuple.getRelationship();
                case 2:
                    if (tuple.isLiteral) {
                        if (tuple.datatype == null) {
                            return String.format("\"%s\"", tuple.object);
                        } else {
                            String trimmedDataType = tuple.datatype;
                            if (tuple.datatype.startsWith(Constants.XML_XSD.uri
                                    + "#")) {
                                trimmedDataType =
                                        tuple.datatype
                                                .substring(Constants.XML_XSD.uri
                                                        .length() + 1);
                            }
                            return String.format("\"%s\"^^<%s>",
                                                 tuple.object,
                                                 trimmedDataType);
                        }
                    } else {
                        return tuple.object;
                    }
            }
            return "";
        }

        @Override
        public void setValueAt(Object aValue, int iRowIndex, int iColumnIndex) {
        }

        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int i) {
            return columnNames[i];
        }

        public int getRowCount() {
            return entries.size();
        }
    }

    public void propertyChange(PropertyChangeEvent arg0) {
        boolean rowSelected = m_editor.getSelectedRow() >= 0;
        m_edit.setEnabled(rowSelected);
        m_delete.setEnabled(rowSelected);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Add...")) {
            TripleEditDialog dialog = new TripleEditDialog(null, m_map);
            dialog.setVisible(true);
            if (!dialog.isCancelled()) {
                String subject = dialog.getSubject();
                String predicate = dialog.getPredicate();
                String objectURI = dialog.getObjectURI();
                boolean literalValue = dialog.getIsLiteral();
                String literalType = dialog.getLiteralType();
                m_dataModel.addRow(subject,
                                   predicate,
                                   objectURI,
                                   literalValue,
                                   literalType);
            }
            dialog = null;
        } else if (e.getActionCommand().equals("Edit...")) {
            if (m_editor.getSelectedRow() != -1) {
                TripleEditDialog dialog =
                        new TripleEditDialog(m_dataModel.entries.get(m_editor
                                .getSelectedRow()), m_map);
                dialog.setVisible(true);
                if (!dialog.isCancelled()) {
                    String subject = dialog.getSubject();
                    String predicate = dialog.getPredicate();
                    String objectURI = dialog.getObjectURI();
                    boolean literalValue = dialog.getIsLiteral();
                    String literalType = dialog.getLiteralType();
                    m_dataModel.replaceRow(m_editor.getSelectedRow(),
                                           subject,
                                           predicate,
                                           objectURI,
                                           literalValue,
                                           literalType);
                }
                dialog = null;
            }
        } else if (e.getActionCommand().equals("Delete")) {
            if (m_editor.getSelectedRow() != -1) {
                m_dataModel.deleteRow(m_editor.getSelectedRow());
            }
        }
    }

    class TripleEditDialog
            extends JDialog
            implements ActionListener, DocumentListener {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private JTextField m_subject;

        private final JComboBox m_predicate;

        private JTextField m_objectURI;

        private JCheckBox m_isLiteral;

        private JComboBox m_literalType;

        private JLabel lab1, lab2, lab3, lab4, lab5;

        private boolean cancelled = true;

        public TripleEditDialog(RelationshipTuple tuple,
                                HashMap<String, String> map) {
            super(Administrator.getInstance(),
                  tuple == null ? "Enter Relationship" : "Edit Relationship",
                  true);

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new SpringLayout());

            mainPanel.add(lab1 = new JLabel("Subject:", SwingConstants.RIGHT));
            mainPanel.add(m_subject =
                    new JTextField(tuple != null ? tuple.subject : PID
                            .toURI(pid)));
            m_subject.setBackground(Administrator.BACKGROUND_COLOR);
            m_subject.setEditable(false);

            mainPanel
                    .add(lab2 = new JLabel("Predicate:", SwingConstants.RIGHT));
            String rels[] =
                    {"",
                     MODEL.HAS_MODEL.toString(),
                     MODEL.HAS_SERVICE.toString(),
                     MODEL.IS_CONTRACTOR_OF.toString(),
                     MODEL.IS_DEPLOYMENT_OF.toString(),
                     Constants.RELS_EXT.IS_MEMBER_OF.toString()};
            m_predicate = new JComboBox(rels);
            m_predicate.setEditable(true);
            Administrator.constrainHeight(m_predicate);
            mainPanel.add(m_predicate);

            mainPanel.add(lab3 = new JLabel("Object:", SwingConstants.RIGHT));
            mainPanel.add(m_objectURI = new JTextField(""));
            m_objectURI.getDocument().addDocumentListener(this);

            mainPanel
                    .add(lab4 = new JLabel("isLiteral:", SwingConstants.RIGHT));
            mainPanel.add(m_isLiteral = new JCheckBox());
            m_isLiteral.setSelected(false);
            m_isLiteral.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    updateFields();
                    // setVisible(false);
                }
            });

            mainPanel.add(lab5 =
                    new JLabel("      Type:", SwingConstants.RIGHT));
            String types[] =
                    {"<untyped>", "long", "int", "float", "double", "dateTime"};
            mainPanel.add(m_literalType = new JComboBox(types));
            m_literalType.setEditable(false);
            Administrator.constrainHeight(m_literalType);

            lab3.setLabelFor(m_objectURI);
            lab4.setLabelFor(m_isLiteral);
            lab5.setLabelFor(m_literalType);

            // Lay out the panel.
            SpringUtilities.makeCompactGrid(mainPanel, 5, 2, // rows, cols
                                            6,
                                            6, // initX, initY
                                            6,
                                            6); // xPad, yPad

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(mainPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            JButton save;
            buttonPanel.add(save = MakeButton("OK", this));
            save.setDefaultCapable(true);
            getRootPane().setDefaultButton(save);
            buttonPanel.add(MakeButton("Cancel", this));
            getContentPane().add(buttonPanel, BorderLayout.SOUTH);

            if (tuple != null) {
                m_subject.setText(tuple.subject);
                m_predicate.setSelectedItem(tuple.getRelationship());
                m_objectURI.setText(tuple.object == null ? "" : tuple.object);
                m_isLiteral.setSelected(tuple.isLiteral);
                String trimmedDataType = null;
                if (tuple.datatype == null) {
                    trimmedDataType = "<untyped>";
                } else if (tuple.datatype.startsWith(Constants.XML_XSD.uri
                        + "#")) {
                    trimmedDataType =
                            tuple.datatype.substring(Constants.XML_XSD.uri
                                    .length() + 1);
                } else {
                    trimmedDataType = tuple.datatype;
                }
                m_literalType.setSelectedItem(trimmedDataType);
                if (m_isLiteral.isSelected()) {
                    m_literalType.setEnabled(true);
                } else {
                    m_literalType.setEnabled(false);
                }
            }
            validate();
            pack();
            setLocationRelativeTo(Administrator.getInstance());

        }

        public void actionPerformed(ActionEvent arg0) {
            if (arg0.getActionCommand().equals("OK")) {
                String msg = "predicate";
                try {
                    URI uriSub = new URI(m_subject.getText());
                    TupleArrayTripleIterator
                            .makePredicateResourceFromRel(getPredicate(), m_map);
                    msg = "object";
                    TupleArrayTripleIterator
                            .makeObjectFromURIandLiteral(getObjectURI(),
                                                         getIsLiteral(),
                                                         getLiteralType());
                } catch (URISyntaxException e) {
                    JOptionPane.showMessageDialog(this,
                                                  "Error: Invalid URI in "
                                                          + msg);
                    return;
                } catch (IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(this,
                                                  "Error: Invalid URI in "
                                                          + msg);
                    return;
                }
                cancelled = false;
                setVisible(false);
            }
            if (arg0.getActionCommand().equals("Cancel")) {
                cancelled = true;
                setVisible(false);
            }
        }

        public void insertUpdate(DocumentEvent arg0) {
            updateFields();
        }

        public void removeUpdate(DocumentEvent arg0) {
            updateFields();
        }

        public void changedUpdate(DocumentEvent arg0) {
            updateFields();
        }

        public void updateFields() {
            if (m_isLiteral.isSelected()) {
                m_literalType.setEnabled(true);
            } else {
                m_literalType.setEnabled(false);
            }
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public String getLiteralType() {
            if (!getIsLiteral()) {
                return null;
            }
            if (m_literalType.getSelectedItem().toString().equals("<untyped>")) {
                return null;
            }
            String litType = m_literalType.getSelectedItem().toString();
            if (litType.startsWith(Constants.XML_XSD.uri)) {
                return litType;
            }
            return Constants.XML_XSD.uri + "#" + litType;
        }

        public boolean getIsLiteral() {
            return m_isLiteral.isSelected();
        }

        public String getObjectURI() {
            if (m_objectURI.getText().length() == 0) {
                return null;
            }
            return m_objectURI.getText();
        }

        public String getPredicate() {
            String predicate = m_predicate.getSelectedItem().toString();
            if (predicate.startsWith(Constants.RELS_EXT.prefix)) {
                predicate =
                        Constants.RELS_EXT.uri
                                + predicate.substring(Constants.RELS_EXT.prefix
                                        .length() + 1);
            } else if (predicate.startsWith(Constants.MODEL.prefix)) {
                predicate =
                        Constants.MODEL.uri
                                + predicate.substring(Constants.MODEL.prefix
                                        .length() + 1);
            }
            return predicate;
        }

        public String getSubject() {
            return m_subject.getText();
        }

    }

}
