/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.types;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import fedora.client.utility.validate.InvalidContentModelException;

import static fedora.client.utility.validate.types.ContentModelInfo.DS_COMPOSITE_MODEL;

/**
 * Parses the DS-COMPOSITE-MODEL XML document, and presents the contents in a
 * way that is compatible with {@link ContentModelInfo}.
 * 
 * @author Jim Blake
 */
public class DsCompositeModelDoc {

    private static final String ELEMENT_DS_TYPE_MODEL = "dsTypeModel";

    private final Set<DsTypeModel> typeModels;

    /**
     * Parse the datastream XML.
     * 
     * @param pid
     *        used in error messages.
     * @param bytes
     *        from the datastream of the content model.
     * @throws InvalidContentModelException
     */
    public DsCompositeModelDoc(String pid, byte[] bytes)
            throws InvalidContentModelException {
        Document doc = parseBytesToDocument(pid, bytes);

        NodeList typeModelNodes =
                doc.getElementsByTagName(ELEMENT_DS_TYPE_MODEL);
        Set<DsTypeModel> typeModels = new HashSet<DsTypeModel>();
        for (int i = 0; i < typeModelNodes.getLength(); i++) {
            Element typeModelElement = (Element) typeModelNodes.item(i);
            typeModels.add(new DsTypeModel(pid, typeModelElement));
        }

        this.typeModels = Collections.unmodifiableSet(typeModels);
    }

    /**
     * Get the type models of the content model.
     */
    public Set<ContentModelInfo.DsTypeModel> getTypeModels() {
        return new HashSet<ContentModelInfo.DsTypeModel>(typeModels);
    }

    /**
     * Create a DOM document from the original XML.
     */
    private Document parseBytesToDocument(String pid, byte[] bytes)
            throws InvalidContentModelException {
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(bytes));
        } catch (ParserConfigurationException e) {
            throw new InvalidContentModelException(pid, "Failed to parse "
                    + DS_COMPOSITE_MODEL, e);
        } catch (SAXException e) {
            throw new InvalidContentModelException(pid, "Failed to parse "
                    + DS_COMPOSITE_MODEL, e);
        } catch (IOException e) {
            throw new InvalidContentModelException(pid, "Failed to parse "
                    + DS_COMPOSITE_MODEL, e);
        }
    }

    /**
     * An implementation of {@link ContentModelInfo.DsTypeModel} that includes a
     * constructor that parses a &lt;dsTypeModel&gt; tag.
     */
    public static class DsTypeModel
            implements ContentModelInfo.DsTypeModel {

        private static final String ATTRIBUTE_ID = "ID";

        private static final String ELEMENT_FORM = "form";

        private final String id;

        private final Set<Form> forms;

        public DsTypeModel(String pid, Element typeModelElement)
                throws InvalidContentModelException {
            id = typeModelElement.getAttribute(ATTRIBUTE_ID);
            if (id.length() == 0) {
                throw new InvalidContentModelException(pid, "<"
                        + ELEMENT_DS_TYPE_MODEL + "> has no '" + ATTRIBUTE_ID
                        + "'");
            }

            NodeList formNodes =
                    typeModelElement.getElementsByTagName(ELEMENT_FORM);
            Set<Form> forms = new HashSet<Form>();
            for (int i = 0; i < formNodes.getLength(); i++) {
                forms.add(new Form((Element) formNodes.item(i)));
            }

            this.forms = Collections.unmodifiableSet(forms);

        }

        public String getId() {
            return id;
        }

        public Set<ContentModelInfo.Form> getForms() {
            return new HashSet<ContentModelInfo.Form>(forms);
        }

    }

    /**
     * An implementation of {@link ContentModelInfo.DsTypeModel} that includes a
     * constructor that parses a &lt;form&gt; tag.
     */
    public static class Form
            implements ContentModelInfo.Form {

        private static final String ATTRIBUTE_MIME = "MIME";

        private static final String ATTRIBUTE_FORMAT_URI = "FORMAT_URI";

        private final String mime;

        private final String formatUri;

        public Form(Element formElement) {
            String mime = formElement.getAttribute(ATTRIBUTE_MIME);
            this.mime = mime.length() == 0 ? null : mime;

            String formatUri = formElement.getAttribute(ATTRIBUTE_FORMAT_URI);
            this.formatUri = formatUri.length() == 0 ? null : formatUri;
        }

        public String getMimeType() {
            return mime;
        }

        public String getFormatUri() {
            return formatUri;
        }

        @Override
        public String toString() {
            return "Form[mime=" + mime + ", formatUri=" + formatUri + "]";
        }

    }
}
