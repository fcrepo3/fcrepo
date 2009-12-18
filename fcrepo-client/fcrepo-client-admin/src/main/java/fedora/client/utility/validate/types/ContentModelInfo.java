/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.types;

import java.util.Collection;

/**
 * An abstraction of a content model object, containing only those fields and
 * attributes that are needed for validation. (As validation becomes more
 * elaborate, this interface will also.)
 * 
 * @author Jim Blake
 */
public interface ContentModelInfo {

    String DS_COMPOSITE_MODEL = "DS-COMPOSITE-MODEL";

    String DS_COMPOSITE_MODEL_FORMAT =
            "info:fedora/fedora-system:FedoraDSCompositeModel-1.0";

    String getPid();

    Collection<DsTypeModel> getTypeModels();

    /**
     * The content model is made up of type models.
     */
    public interface DsTypeModel {

        String getId();

        Collection<Form> getForms();
    }

    /**
     * The type model may have 0 or more forms.
     */
    public interface Form {

        String getMimeType();

        String getFormatUri();
    }
}
