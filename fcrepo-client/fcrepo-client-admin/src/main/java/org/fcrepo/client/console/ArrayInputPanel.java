/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.client.console;

import java.util.ArrayList;

import javax.swing.JLabel;

/**
 * @author Chris Wilper
 * @param <T>
 * @param <T>
 */
public class ArrayInputPanel<T>
        extends InputPanel<T[]> {

    private static final long serialVersionUID = 1L;

    private final ArrayList<T> m_inputPanels;

    public ArrayInputPanel() {
        m_inputPanels = new ArrayList<T>();
        add(new JLabel("Array handler not implemented, will be null."));
    }

    @SuppressWarnings("unchecked")
    @Override
    public T[] getValue() {
        return (T[]) m_inputPanels.toArray();
    }
    
    public static <E> ArrayInputPanel<E> getInstance(Class<E> type) {
        return new ArrayInputPanel<E>();
    }

}
