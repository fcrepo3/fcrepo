/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.console;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import fedora.server.utilities.DateUtility;

/**
 * Creates an input panel for entering datetime stamps.
 * 
 * The input format must be of the form:
 * <code>YYYY-MM-DDTHH:mm:ss</code>, where:
 * <ul>
 * <li>YYYY - 4 digit year</li>
 * <li>MM - 2 digit month</li>
 * <li>DD - 2 digit day of month</li>
 * <li>hh - 2 digit hour of day using 24 hour clock</li>
 * <li>mm - 2 digit minutes of the hour</li>
 * <li>ss - 2 digit seconds of the minute</li>
 * </ul>
 * 
 * @author Ross Wayland
 */
public class DateTimeInputPanel
        extends InputPanel {

    private static final long serialVersionUID = 1L;

    private final JRadioButton m_nullRadioButton;

    private final JTextField m_textField;

    public DateTimeInputPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel nullPanel = new JPanel();
        nullPanel.setLayout(new BorderLayout());
        m_nullRadioButton = new JRadioButton("Use null");
        m_nullRadioButton.setSelected(true);
        nullPanel.add(m_nullRadioButton, BorderLayout.WEST);
        add(nullPanel);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.X_AXIS));
        JRadioButton textRadioButton = new JRadioButton("Use text: ");
        textRadioButton.setSelected(false);
        textPanel.add(textRadioButton);
        m_textField = new JTextField(10);
        textPanel.add(m_textField);
        add(textPanel);

        ButtonGroup g = new ButtonGroup();
        g.add(m_nullRadioButton);
        g.add(textRadioButton);

    }

    @Override
    public Object getValue() {
        if (m_nullRadioButton.isSelected()) {
            return null;
        } else {
            return DateUtility.convertStringToDate(m_textField.getText());
        }
    }

}
