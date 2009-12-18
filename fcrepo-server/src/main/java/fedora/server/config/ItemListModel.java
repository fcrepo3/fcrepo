/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

/**
 * A ListModel, backed by a java-util-List.
 */
public class ItemListModel
        extends DefaultListModel {

    private static final long serialVersionUID = 1L;

    public ItemListModel(List items) {
        for (int i = 0; i < items.size(); i++) {
            addElement(items.get(i));
        }
    }

    public List toList() {
        ArrayList out = new ArrayList();
        Object[] array = toArray();
        for (Object element : array) {
            out.add(element);
        }
        return out;
    }

}
