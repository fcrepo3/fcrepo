/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.objecteditor.types;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Turns a datastream input spec into a java object(s).
 */
class DatastreamInputSpecDeserializer
        extends DefaultHandler {

    /** The deserialized input spec */
    private DatastreamInputSpec m_result;

    // values gathered and built while parsing
    private String m_label;

    private List m_bindingRules;

    private String m_min;

    private String m_max;

    private String m_orderMatters;

    private String m_key;

    private StringBuffer m_inputLabel;

    private StringBuffer m_inputInstruction;

    private StringBuffer m_types;

    // for character content, which element are we reading?
    private int m_readingContent;

    private static int NONE = 0;

    private static int INPUT_LABEL = 1;

    private static int TYPES = 2;

    private static int INPUT_INSTRUCTION = 3;

    public DatastreamInputSpecDeserializer(InputStream xml)
            throws IOException {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser parser = spf.newSAXParser();
            parser.parse(xml, this);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                throw new IOException(e.getMessage());
            } else {
                throw new IOException("Error parsing datastream input spec: "
                        + e.getClass().getName());
            }
        }
    }

    public DatastreamInputSpec getResult() {
        return m_result;
    }

    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes a) {
        if (localName.equals("DSInputSpec")) {
            m_bindingRules = new ArrayList();
            for (int i = 0; i < a.getLength(); i++) {
                String name = a.getLocalName(i);
                if (name.equals("label")) {
                    m_label = a.getValue(i);
                }
            }
        } else if (localName.equals("DSInput")) {
            m_inputLabel = new StringBuffer();
            m_inputInstruction = new StringBuffer();
            m_types = new StringBuffer();
            for (int i = 0; i < a.getLength(); i++) {
                String name = a.getLocalName(i);
                if (name.equals("DSMin")) {
                    m_min = a.getValue(i);
                } else if (name.equals("DSMax")) {
                    m_max = a.getValue(i);
                } else if (name.equals("DSOrdinality")) {
                    m_orderMatters = a.getValue(i);
                } else if (name.equals("wsdlMsgPartName")) {
                    m_key = a.getValue(i);
                }
            }
        } else if (localName.equals("DSInputLabel")) {
            m_readingContent = INPUT_LABEL;
        } else if (localName.equals("DSMIME")) {
            m_readingContent = TYPES;
        } else if (localName.equals("DSInputInstruction")) {
            m_readingContent = INPUT_INSTRUCTION;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (m_readingContent == INPUT_LABEL) {
            m_inputLabel.append(ch, start, length);
        } else if (m_readingContent == TYPES) {
            m_types.append(ch, start, length);
        } else if (m_readingContent == INPUT_INSTRUCTION) {
            m_inputInstruction.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (localName.equals("DSInputLabel") || localName.equals("DSMIME")
                || localName.equals("DSInputInstruction")) {
            m_readingContent = NONE;
            if (localName.equals("DSMIME")) {
                // there might be multiple elements, so add a space,
                // which can't hurt since later it's trimmed
                m_types.append(" ");
            }
        } else if (localName.equals("DSInput")) {
            // first interpret what we read, 
            // converting to appropriate types and defualt values
            int min;
            try {
                min = Integer.parseInt(m_min);
            } catch (Exception e) {
                min = 1; // default the same way xsd:minOccurs does
            }
            int max;
            try {
                max = Integer.parseInt(m_max);
            } catch (Exception e) {
                if (m_max != null
                        && (m_max.startsWith("-") || m_max.startsWith("n")
                                || m_max.startsWith("N")
                                || m_max.startsWith("u")
                                || m_max.startsWith("U")
                                || m_max.startsWith("i") || m_max
                                .startsWith("I"))) {
                    // covers various spellings of "n/a", "none", "unbounded", 
                    // "unrestricted", "infinite", and -1
                    max = -1;
                } else {
                    max = 1; // default the same way xsd:maxOccurs does
                }
            }
            boolean orderMatters = false;
            if (m_orderMatters != null
                    && (m_orderMatters.startsWith("y")
                            || m_orderMatters.startsWith("Y")
                            || m_orderMatters.startsWith("t")
                            || m_orderMatters.startsWith("T") || m_orderMatters
                            .equals("1"))) {
                // covers various spellings of "yes", "true", and 1
                orderMatters = true;
            }
            String[] types =
                    m_types.toString().replaceAll(" +", " ").trim().split(" ");
            // then add it to the list of rules
            m_bindingRules.add(new DatastreamBindingRule(m_key,
                                                         m_inputLabel
                                                                 .toString(),
                                                         m_inputInstruction
                                                                 .toString(),
                                                         min,
                                                         max,
                                                         orderMatters,
                                                         types));
        } else if (localName.equals("DSInputSpec")) {
            m_result = new DatastreamInputSpec(m_label, m_bindingRules);
        }
    }
}
