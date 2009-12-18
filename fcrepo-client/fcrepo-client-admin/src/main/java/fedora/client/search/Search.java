/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.search;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import fedora.client.Administrator;

import fedora.server.types.gen.ComparisonOperator;
import fedora.server.types.gen.Condition;
import fedora.server.types.gen.FieldSearchQuery;

/**
 * @author Chris Wilper
 */
public class Search
        extends JInternalFrame {

    private static final long serialVersionUID = 1L;

    private final List<String> m_displayFields;

    private final JTextField m_simpleQueryField;

    private final ConditionsTableModel m_model;

    private final JTabbedPane m_tabbedPane;

    protected static String[] s_fieldArray =
            {"pid", "label", "state",
                    "ownerId", "cDate", "mDate", "dcmDate", "title", "creator",
                    "subject", "description", "publisher", "contributor",
                    "date", "type", "format", "identifier", "source",
                    "language", "relation", "coverage", "rights"};

    protected static String[] s_operatorArray =
            {"contains", "equals", "is less than", "is less than or equal to",
                    "is greater than", "is greater than or equal to"};

    protected static String[] s_operatorActuals =
            {"has", "eq", "lt", "le", "gt", "ge"};

    public Search() {
        super("Search Repository", true, //resizable
              true, //closable
              true, //maximizable
              true);//iconifiable

        m_displayFields = new ArrayList<String>();
        m_displayFields.add("pid");
        m_displayFields.add("cDate");
        m_displayFields.add("title");

        // NORTH: fieldsPanel(selectedFieldsLabel, modifySelectedFieldsButtonPanel)

        // CENTER: selectedFieldsLabel
        JLabel selectedFieldsLabel = new JLabel();
        StringBuffer text = new StringBuffer();
        text.append("<html><i>");
        for (int i = 0; i < m_displayFields.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(m_displayFields.get(i));
        }
        text.append("</i></html>");
        selectedFieldsLabel.setText(text.toString());

        // EAST: modifySelectedFieldsButton
        JButton modifySelectedFieldsButton = new JButton("Change..");
        ChangeFieldsButtonListener cfbl =
                new ChangeFieldsButtonListener(selectedFieldsLabel,
                                               m_displayFields);
        modifySelectedFieldsButton.addActionListener(cfbl);

        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BorderLayout());
        fieldsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
                .createTitledBorder(BorderFactory.createEtchedBorder(),
                                    "Fields to Display"), BorderFactory
                .createEmptyBorder(0, 6, 6, 6)));
        fieldsPanel.add(selectedFieldsLabel, BorderLayout.CENTER);
        fieldsPanel.add(modifySelectedFieldsButton, BorderLayout.EAST);

        // CENTER: tabbedPaneContainer(m_tabbedPane)

        // CENTER: m_tabbedPane(simpleSearchPanel, advancedSearchPanel)

        // PANE 1: simpleSearchPanel(simplePromptPanel, simpleInstructionsLabel)

        // NORTH: simplePromptPanel(promptLabel, m_simpleQueryField)

        // FLOW: promptLabel

        JLabel promptLabel = new JLabel("Search all fields for ");

        // FLOW: m_simpleQueryField

        m_simpleQueryField = new JTextField("*", 15);

        JPanel simplePromptPanel = new JPanel();
        simplePromptPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        simplePromptPanel.add(promptLabel);
        simplePromptPanel.add(m_simpleQueryField);

        // SOUTH: simpleInstructionsLabel

        JLabel simpleInstructionsLabel =
                new JLabel("<html>Note: You may use the ? and * wildcards.  '?' means <i>any one</i> character, and '*' means <i>any number of any characters</i>. Searches are case-insensitive.");

        JPanel simpleSearchPanel = new JPanel();
        simpleSearchPanel.setLayout(new BorderLayout());
        simpleSearchPanel
                .setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        simpleSearchPanel.add(simplePromptPanel, BorderLayout.NORTH);
        simpleSearchPanel.add(simpleInstructionsLabel, BorderLayout.CENTER);

        // PANE 2: advancedSearchPanel(innerConditionsPanel, modifyConditionsOuterPanel)

        // CENTER: innerConditionsPanel(conditionsScrollPane)

        // CENTER: conditionsScrollPane(conditionsTable)

        // WRAPS: conditionsTable
        m_model = new ConditionsTableModel();
        JTable conditionsTable = new JTable(m_model);

        JScrollPane conditionsScrollPane = new JScrollPane(conditionsTable);
        conditionsScrollPane.setBorder(BorderFactory.createEmptyBorder(0,
                                                                       0,
                                                                       6,
                                                                       6));

        JPanel innerConditionsPanel = new JPanel();
        innerConditionsPanel.setLayout(new BorderLayout());
        innerConditionsPanel.add(conditionsScrollPane, BorderLayout.CENTER);

        // EAST: modifyConditionsOuterPanel(modifyConditionsInnerPanel)

        // NORTH: modifyConditionsInnerPanel

        // GRID: addConditionButton
        JButton addConditionButton = new JButton("Add..");

        // GRID: modifyConditionButton
        JButton modifyConditionButton = new JButton("Change..");

        // GRID: deleteConditionButton
        JButton deleteConditionButton = new JButton("Delete");

        // Now that buttons are available, register the
        // list selection listener that sets their enabled state.
        conditionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ConditionSelectionListener sListener =
                new ConditionSelectionListener(modifyConditionButton,
                                               deleteConditionButton,
                                               -1);
        conditionsTable.getSelectionModel().addListSelectionListener(sListener);
        // ..and add listeners to the buttons

        addConditionButton
                .addActionListener(new AddConditionButtonListener(m_model));
        modifyConditionButton
                .addActionListener(new ChangeConditionButtonListener(m_model,
                                                                     sListener));
        deleteConditionButton
                .addActionListener(new DeleteConditionButtonListener(m_model,
                                                                     sListener));

        JPanel modifyConditionsInnerPanel = new JPanel();
        modifyConditionsInnerPanel.setLayout(new GridLayout(3, 1));
        modifyConditionsInnerPanel.add(addConditionButton);
        modifyConditionsInnerPanel.add(modifyConditionButton);
        modifyConditionsInnerPanel.add(deleteConditionButton);

        JPanel modifyConditionsOuterPanel = new JPanel();
        modifyConditionsOuterPanel.setLayout(new BorderLayout());
        modifyConditionsOuterPanel.add(modifyConditionsInnerPanel,
                                       BorderLayout.NORTH);

        JPanel advancedSearchPanel = new JPanel();
        advancedSearchPanel.setLayout(new BorderLayout());
        advancedSearchPanel.setBorder(BorderFactory.createEmptyBorder(6,
                                                                      6,
                                                                      6,
                                                                      6));
        advancedSearchPanel.add(innerConditionsPanel, BorderLayout.CENTER);
        advancedSearchPanel.add(modifyConditionsOuterPanel, BorderLayout.EAST);

        m_tabbedPane = new JTabbedPane();
        m_tabbedPane.addTab("Simple", simpleSearchPanel);
        m_tabbedPane.setSelectedIndex(0);
        m_tabbedPane.addTab("Advanced", advancedSearchPanel);

        JPanel tabbedPaneContainer = new JPanel();
        tabbedPaneContainer.setLayout(new BorderLayout());
        tabbedPaneContainer
                .setBorder(BorderFactory
                        .createCompoundBorder(BorderFactory
                                                      .createEmptyBorder(6,
                                                                         0,
                                                                         6,
                                                                         0),
                                              BorderFactory
                                                      .createCompoundBorder(BorderFactory
                                                                                    .createTitledBorder(BorderFactory
                                                                                                                .createEtchedBorder(),
                                                                                                        "Query"),
                                                                            BorderFactory
                                                                                    .createEmptyBorder(0,
                                                                                                       6,
                                                                                                       6,
                                                                                                       6))));
        tabbedPaneContainer.add(m_tabbedPane, BorderLayout.CENTER);

        // SOUTH: finishButtonsPanel

        // FLOW: searchButton
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new SearchButtonListener(cfbl, m_model));

        // FLOW: cancelButton
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doDefaultCloseAction();
            }
        });

        JPanel finishButtonsPanel = new JPanel();
        finishButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        finishButtonsPanel.add(searchButton);
        finishButtonsPanel.add(cancelButton);

        JPanel outerPane = new JPanel();
        outerPane.setLayout(new BorderLayout());
        outerPane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        outerPane.add(fieldsPanel, BorderLayout.NORTH);
        outerPane.add(tabbedPaneContainer, BorderLayout.CENTER);
        outerPane.add(finishButtonsPanel, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(outerPane, BorderLayout.CENTER);

        ImageIcon searchIcon =
            new ImageIcon(ClassLoader.
                          getSystemResource("images/client/standard/general/Search16.gif"));
        setFrameIcon(searchIcon);

        setSize(400, 400);
    }

    public class ConditionSelectionListener
            implements ListSelectionListener {

        private int m_selectedRow;

        private final JButton m_modifyButton;

        private final JButton m_deleteButton;

        public ConditionSelectionListener(JButton modifyButton,
                                          JButton deleteButton,
                                          int selectedRow) {
            m_selectedRow = selectedRow;
            m_modifyButton = modifyButton;
            m_deleteButton = deleteButton;
            updateButtons();
        }

        public void valueChanged(ListSelectionEvent e) {
            //Ignore extra messages.
            if (e.getValueIsAdjusting()) {
                return;
            }

            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (lsm.isSelectionEmpty()) {
                m_selectedRow = -1;
            } else {
                m_selectedRow = lsm.getMinSelectionIndex();
            }
            updateButtons();
        }

        public int getSelectedRow() {
            return m_selectedRow;
        }

        private void updateButtons() {
            if (getSelectedRow() == -1) {
                m_modifyButton.setEnabled(false);
                m_deleteButton.setEnabled(false);
            } else {
                m_modifyButton.setEnabled(true);
                m_deleteButton.setEnabled(true);
            }
        }
    }

    public class SelectFieldsDialog
            extends JDialog {

        private static final long serialVersionUID = 1L;

        private List<String> m_selectedFields;

        private final JCheckBox pidBox, typeBox, labelBox,
                formatBox, titleBox, identifierBox,
                creatorBox, sourceBox, stateBox, subjectBox, languageBox,
                ownerIdBox, descriptionBox, relationBox, cDateBox,
                publisherBox, coverageBox, mDateBox, contributorBox, rightsBox,
                dcmDateBox, dateBox;

        public SelectFieldsDialog(List fieldList) {
            super(Administrator.getInstance(), "Select Fields to Display", true);

            // mainPanel(northPanel, noteLabel, southPanel)

            // NORTH: northPanel(bunch of JCheckBoxes)

            pidBox = new JCheckBox("pid", fieldList.contains("pid"));
            pidBox.setToolTipText("a globally unique id");
            typeBox = new JCheckBox("type", fieldList.contains("type"));
            typeBox
                    .setToolTipText("a list of dc:type values, indicating nature or genre");
            labelBox = new JCheckBox("label", fieldList.contains("label"));
            labelBox.setToolTipText("a human-readable name");
            formatBox = new JCheckBox("format", fieldList.contains("format"));
            formatBox
                    .setToolTipText("a list of dc:format values, indicating physical or digital forms");

            titleBox = new JCheckBox("title", fieldList.contains("title"));
            titleBox.setToolTipText("a list of dc:title values (names)");
            identifierBox =
                    new JCheckBox("identifier", fieldList
                            .contains("identifier"));
            identifierBox
                    .setToolTipText("a list of dc:identifier values, providing unambiguous ids in certain contexts");
            creatorBox =
                    new JCheckBox("creator", fieldList.contains("creator"));
            creatorBox
                    .setToolTipText("a list of dc:creator values, identifying primary maker(s) of the content");
            sourceBox = new JCheckBox("source", fieldList.contains("source"));
            sourceBox
                    .setToolTipText("a list of dc:source values, identifying resources from which this resource is derived");
            stateBox = new JCheckBox("state", fieldList.contains("state"));
            stateBox.setToolTipText("the state of the object, A for active");
            subjectBox =
                    new JCheckBox("subject", fieldList.contains("subject"));
            subjectBox
                    .setToolTipText("a list of dc:subject values, indicating the topic of the resource");
            languageBox =
                    new JCheckBox("language", fieldList.contains("language"));
            languageBox
                    .setToolTipText("a list of dc:language values, language(s) of the intellectual content");
            ownerIdBox =
                    new JCheckBox("ownerId", fieldList.contains("ownerId"));
            ownerIdBox
                    .setToolTipText("owner id, the identity of the repository user who owns the object");
            descriptionBox =
                    new JCheckBox("description", fieldList
                            .contains("description"));
            descriptionBox
                    .setToolTipText("a list of description values; accounts of the content of the resouces");
            relationBox =
                    new JCheckBox("relation", fieldList.contains("relation"));
            relationBox
                    .setToolTipText("a list of dc:relation values, identifying related resources");
            cDateBox = new JCheckBox("cDate", fieldList.contains("cDate"));
            cDateBox
                    .setToolTipText("creation date, the date the object was first created in the repository");
            publisherBox =
                    new JCheckBox("publisher", fieldList.contains("publisher"));
            publisherBox
                    .setToolTipText("a list of dc:publisher values, entities responsible for making the resouce available");
            coverageBox =
                    new JCheckBox("coverage", fieldList.contains("coverage"));
            coverageBox
                    .setToolTipText("a list of dc:coverage values, indicating the extent or scope of the content");
            mDateBox = new JCheckBox("mDate", fieldList.contains("mDate"));
            mDateBox
                    .setToolTipText("modified date, the last date the object was changed");
            contributorBox =
                    new JCheckBox("contributor", fieldList
                            .contains("contributor"));
            contributorBox
                    .setToolTipText("a list of dc:contributor values, identifying content-contributing entities");
            rightsBox = new JCheckBox("rights", fieldList.contains("rights"));
            rightsBox
                    .setToolTipText("a list of dc:rights values, regarding rights held in and over the resource");
            dcmDateBox =
                    new JCheckBox("dcmDate", fieldList.contains("dcmDate"));
            dcmDateBox
                    .setToolTipText("dublin core modified date, the date of the last change to the DC record");
            dateBox = new JCheckBox("date", fieldList.contains("date"));
            dateBox
                    .setToolTipText("a list of dc:date values, identifying significant events in the resource's lifecycle");

            JPanel northPanel = new JPanel();
            northPanel.setLayout(new GridLayout(9, 3));
            northPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            northPanel.add(pidBox);
            northPanel.add(typeBox);
            northPanel.add(labelBox);
            northPanel.add(formatBox);
            northPanel.add(titleBox);
            northPanel.add(identifierBox);
            northPanel.add(creatorBox);
            northPanel.add(sourceBox);
            northPanel.add(stateBox);
            northPanel.add(subjectBox);
            northPanel.add(languageBox);
            northPanel.add(ownerIdBox);
            northPanel.add(descriptionBox);
            northPanel.add(relationBox);
            northPanel.add(cDateBox);
            northPanel.add(publisherBox);
            northPanel.add(coverageBox);
            northPanel.add(mDateBox);
            northPanel.add(contributorBox);
            northPanel.add(rightsBox);
            northPanel.add(dcmDateBox);
            northPanel.add(dateBox);

            // CENTER: noteLabel

            JLabel noteLabel =
                    new JLabel("<html><i> Note: Hold your mouse over a field's name to see a brief description.</i></html>");

            // SOUTH: southPanel(cancelButton, okButton)

            JButton okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    updateSelectedFields();
                    setVisible(false);
                }
            });

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });

            JPanel southPanel = new JPanel();
            southPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            southPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            southPanel.add(okButton);
            southPanel.add(cancelButton);

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(northPanel, BorderLayout.NORTH);
            getContentPane().add(noteLabel, BorderLayout.CENTER);
            getContentPane().add(southPanel, BorderLayout.SOUTH);
            pack();
            setLocation(Administrator.getInstance()
                    .getCenteredPos(getSize().width, getSize().height));
        }

        public void updateSelectedFields() {
            m_selectedFields = new ArrayList<String>();
            if (pidBox.isSelected()) {
                m_selectedFields.add("pid");
            }
            if (labelBox.isSelected()) {
                m_selectedFields.add("label");
            }
            if (stateBox.isSelected()) {
                m_selectedFields.add("state");
            }
            if (ownerIdBox.isSelected()) {
                m_selectedFields.add("ownerId");
            }
            if (cDateBox.isSelected()) {
                m_selectedFields.add("cDate");
            }
            if (mDateBox.isSelected()) {
                m_selectedFields.add("mDate");
            }
            if (dcmDateBox.isSelected()) {
                m_selectedFields.add("dcmDate");
            }
            if (titleBox.isSelected()) {
                m_selectedFields.add("title");
            }
            if (creatorBox.isSelected()) {
                m_selectedFields.add("creator");
            }
            if (subjectBox.isSelected()) {
                m_selectedFields.add("subject");
            }
            if (descriptionBox.isSelected()) {
                m_selectedFields.add("description");
            }
            if (publisherBox.isSelected()) {
                m_selectedFields.add("publisher");
            }
            if (contributorBox.isSelected()) {
                m_selectedFields.add("contributor");
            }
            if (dateBox.isSelected()) {
                m_selectedFields.add("date");
            }
            if (typeBox.isSelected()) {
                m_selectedFields.add("type");
            }
            if (formatBox.isSelected()) {
                m_selectedFields.add("format");
            }
            if (identifierBox.isSelected()) {
                m_selectedFields.add("identifier");
            }
            if (sourceBox.isSelected()) {
                m_selectedFields.add("source");
            }
            if (languageBox.isSelected()) {
                m_selectedFields.add("language");
            }
            if (relationBox.isSelected()) {
                m_selectedFields.add("relation");
            }
            if (coverageBox.isSelected()) {
                m_selectedFields.add("coverage");
            }
            if (rightsBox.isSelected()) {
                m_selectedFields.add("rights");
            }
        }

        public List getSelectedFields() {
            return m_selectedFields;
        }

    }

    public class AddConditionButtonListener
            implements ActionListener {

        private final ConditionsTableModel m_model;

        public AddConditionButtonListener(ConditionsTableModel model) {
            m_model = model;
        }

        public void actionPerformed(ActionEvent e) {
            ModConditionDialog dialog = new ModConditionDialog(m_model, -1);
            dialog.setVisible(true);
        }
    }

    public class ChangeConditionButtonListener
            implements ActionListener {

        private final ConditionsTableModel m_model;

        private final ConditionSelectionListener m_sListener;

        public ChangeConditionButtonListener(ConditionsTableModel model,
                                             ConditionSelectionListener sListener) {
            m_model = model;
            m_sListener = sListener;
        }

        public void actionPerformed(ActionEvent e) {
            // will only be invoked if an existing row is selected
            ModConditionDialog dialog =
                    new ModConditionDialog(m_model, m_sListener
                            .getSelectedRow());
            dialog.setVisible(true);
        }
    }

    public class DeleteConditionButtonListener
            implements ActionListener {

        private final ConditionsTableModel m_model;

        private final ConditionSelectionListener m_sListener;

        public DeleteConditionButtonListener(ConditionsTableModel model,
                                             ConditionSelectionListener sListener) {
            m_model = model;
            m_sListener = sListener;
        }

        public void actionPerformed(ActionEvent e) {
            // will only be invoked if an existing row is selected
            int r = m_sListener.getSelectedRow();
            m_model.getConditions().remove(r);
            m_model.fireTableRowsDeleted(r, r);
        }
    }

    public class ModConditionDialog
            extends JDialog {

        private static final long serialVersionUID = 1L;

        private final ConditionsTableModel m_model;

        private final int m_rowNum;

        private final JComboBox m_fieldBox;

        private final JComboBox m_operatorBox;

        private final JTextField m_valueField;

        public ModConditionDialog(ConditionsTableModel model, int rowNum) {
            super(Administrator.getInstance(), "Enter Condition", true);
            m_model = model;
            m_rowNum = rowNum;

            // mainPanel(northPanel, southPanel)

            // NORTH: northPanel(fieldBox,operatorBox,valueField)

            m_fieldBox = new JComboBox(s_fieldArray);
            m_operatorBox = new JComboBox(s_operatorArray);
            m_valueField = new JTextField(10);
            if (rowNum != -1) {
                // if this is an edit, start with current values
                m_fieldBox.setSelectedIndex(indexOf((String) m_model
                        .getValueAt(rowNum, 0)));
                m_operatorBox.setSelectedIndex(indexOf((String) m_model
                        .getValueAt(rowNum, 1)));
                m_valueField.setText((String) m_model.getValueAt(rowNum, 2));
            }

            JPanel northPanel = new JPanel();
            northPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            northPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            northPanel.add(m_fieldBox);
            northPanel.add(m_operatorBox);
            northPanel.add(m_valueField);

            // SOUTH: southPanel(cancelButton, okButton)

            JButton okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    updateModelAndNotify();
                    setVisible(false);
                }
            });

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });

            JPanel southPanel = new JPanel();
            southPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            southPanel.add(okButton);
            southPanel.add(cancelButton);

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(northPanel, BorderLayout.NORTH);
            getContentPane().add(southPanel, BorderLayout.SOUTH);
            pack();
            setLocation(Administrator.getInstance()
                    .getCenteredPos(getSize().width, getSize().height));
        }

        private int indexOf(String s) {
            for (int i = 0; i < s_fieldArray.length; i++) {
                if (s_fieldArray[i].equals(s)) {
                    return i;
                }
            }
            for (int i = 0; i < s_operatorArray.length; i++) {
                if (s_operatorArray[i].equals(s)) {
                    return i;
                }
            }
            return -1;
        }

        public void updateModelAndNotify() {
            // create a Condition given the current values
            Condition cond = new Condition();
            cond.setProperty(s_fieldArray[m_fieldBox.getSelectedIndex()]);
            cond.setOperator(ComparisonOperator
                    .fromValue(s_operatorActuals[m_operatorBox
                            .getSelectedIndex()]));
            cond.setValue(m_valueField.getText());
            // if rowNum is -1, add it
            if (m_rowNum == -1) {
                // if it wasn't there before, add it
                m_model.getConditions().add(cond);
            } else {
                // else replace existing condition
                m_model.getConditions().set(m_rowNum, cond);
            }
            m_model.fireTableDataChanged();
        }

    }

    public class SearchButtonListener
            implements ActionListener {

        private final ChangeFieldsButtonListener m_fieldSelector;

        private final ConditionsTableModel m_model;

        public SearchButtonListener(ChangeFieldsButtonListener fieldSelector,
                                    ConditionsTableModel model) {
            m_fieldSelector = fieldSelector;
            m_model = model;
        }

        public void actionPerformed(ActionEvent e) {
            List fields = m_fieldSelector.getFieldList();
            String[] displayFields = new String[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                displayFields[i] = (String) fields.get(i);
            }
            FieldSearchQuery query = new FieldSearchQuery();
            if (m_tabbedPane.getSelectedIndex() == 0) {
                query.setTerms(m_simpleQueryField.getText());
            } else {
                List conditions = m_model.getConditions();
                Condition[] cond = new Condition[conditions.size()];
                for (int i = 0; i < conditions.size(); i++) {
                    cond[i] = (Condition) conditions.get(i);
                }
                query.setConditions(cond);
            }
            ResultFrame frame =
                    new ResultFrame("Search Results", displayFields, 100, query);
            frame.setVisible(true);
            Administrator.getDesktop().add(frame);
            try {
                frame.setSelected(true);
            } catch (java.beans.PropertyVetoException pve) {
            }
        }
    }

    public class ChangeFieldsButtonListener
            implements ActionListener {

        private final JLabel m_fieldLabel;

        private List m_fieldList;

        public ChangeFieldsButtonListener(JLabel fieldLabel, List fieldList) {
            m_fieldLabel = fieldLabel;
            m_fieldList = fieldList;
        }

        public void actionPerformed(ActionEvent e) {
            // launch an editor for the fields to search on,
            // and put the values in
            // - the label (with html and italics)
            // - the fieldList

            // first, construct the dialog with the values from fieldList
            SelectFieldsDialog dialog = new SelectFieldsDialog(m_fieldList);
            dialog.setVisible(true);
            if (dialog.getSelectedFields() != null) {
                m_fieldList = dialog.getSelectedFields();
                // if they clicked cancel, just exit.
                // otherwise, set the values in m_fieldList,
                // then set the text of m_fieldLabel based on those.
                StringBuffer text = new StringBuffer();
                text.append("<html><i>");
                for (int i = 0; i < m_fieldList.size(); i++) {
                    if (i > 0) {
                        text.append(", ");
                    }
                    text.append((String) m_fieldList.get(i));
                }
                text.append("</i></html>");
                m_fieldLabel.setText(text.toString());
            }
        }

        public List getFieldList() {
            return m_fieldList;
        }
    }

    public class ConditionsTableModel
            extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        List<Condition> m_conditions;

        public ConditionsTableModel() {
            m_conditions = new ArrayList<Condition>();
        }

        public ConditionsTableModel(List<Condition> conditions) {
            m_conditions = conditions;
        }

        public List<Condition> getConditions() {
            return m_conditions;
        }

        @Override
        public String getColumnName(int col) {
            if (col == 0) {
                return "Field";
            } else if (col == 1) {
                return "Operator";
            } else {
                return "Value";
            }
        }

        public int getRowCount() {
            return m_conditions.size();
        }

        public int getColumnCount() {
            return 3;
        }

        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public Object getValueAt(int row, int col) {
            Condition cond = m_conditions.get(row);
            if (col == 0) {
                return cond.getProperty();
            } else if (col == 1) {
                return getNiceName(cond.getOperator().toString());
            } else {
                return cond.getValue();
            }
        }

        private String getNiceName(String operString) {
            if (operString.equals("has")) {
                return "contains";
            }
            if (operString.equals("eq")) {
                return "equals";
            }
            if (operString.equals("lt")) {
                return "is less than";
            }
            if (operString.equals("le")) {
                return "is less than or equal to";
            }
            if (operString.equals("gt")) {
                return "is greater than";
            }
            return "is greater than or equal to";
        }

    }

}
