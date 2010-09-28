/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test;

import java.io.FileInputStream;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import org.antlr.stringtemplate.StringTemplate;


/**
 * Iterator that takes a set of attribute values from a values file
 * and iterates by repeatedly substituting in the next set of values into the
 * template
 *
 * values file format: java properties, with
 * template.attribute.sets.count=n :
 *   number of sets to iterate over
 * template.attribute.set.0.someattribute1=value1:
 * template.attribute.set.0.someattribute2=value2:
 *   when on set iteration 0, substitute $someattribute1$ with value1
 *   and similarly $someattribute2$ with value2
 * nb: sets are zero-indexed, numbered 0 to count - 1
 *     escape backslashes to \\
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class TemplatedResourceIterator implements Iterator<String> {

    // template source
    private final String m_templateSource;

    // properties file contains attributes and values to substitute
    private final String m_valuesFilename;
    private final Properties m_values;
    private final Set<String> m_propertyNames;

    // number of sets to iterate
    private final int m_setCount;
    // index of next set to return with next()
    private int m_nextSet = 0;

    // for validation of consistent attribute counts across sets
    int m_lastAttributeCount = -1;

    // character escaping to perform
    private final boolean m_escapeXML;

    public TemplatedResourceIterator(String template, String valuesFilename) throws Exception {
        this(template, valuesFilename, false);
    }
    public TemplatedResourceIterator(String template, String valuesFilename, boolean escapeXML) throws Exception {
        m_escapeXML = escapeXML;

        m_values = new Properties();
        m_valuesFilename = valuesFilename;
        m_values.load(new FileInputStream(m_valuesFilename));

        m_setCount = Integer.parseInt(m_values.getProperty("template.attribute.sets.count"));
        // java 1.6: m_propertyNames = m_values.stringPropertyNames();
        m_propertyNames = new HashSet<String>();
        for (Object objPropertyName : m_values.keySet()) {
            m_propertyNames.add((String)objPropertyName);
        }

        // the stuff to template
        m_templateSource = template;

    }

    public boolean hasNext() {
        return (m_nextSet < m_setCount);
    }

    public String next() {

        if (m_nextSet >= m_setCount)
            throw new NoSuchElementException();

        // all properties in the current set start with...
        String setPropertyNamePrefix = "template.attribute.set." + m_nextSet + ".";

        StringTemplate tpl = new StringTemplate(m_templateSource);

        int attributeCount = 0; // count number of attributes specified for replacement
        for (String propertyName : m_propertyNames) {
            // property for an attribute for the current set?
            if (propertyName.startsWith(setPropertyNamePrefix)) {
                // get attribute name and value
                String attributeName = propertyName.replace(setPropertyNamePrefix, "");
                String attributeValue = m_values.getProperty(propertyName);

                // escape value if necessary
                String value;
                if (m_escapeXML) {
                    value = escapeXML(attributeValue);
                } else {
                    value = attributeValue;
                }

                tpl.setAttribute(attributeName, value);

                attributeCount++;
            }
        }
        // some checks
        if (attributeCount == 0)
            throw new RuntimeException("No attributes found for set " + m_nextSet + " (" + m_valuesFilename + ")" );
        if (m_lastAttributeCount != -1) {
            if (m_lastAttributeCount != attributeCount)
                throw new RuntimeException("Number of attributes is different in sets " + (m_nextSet -1) + " and " + m_nextSet + " (" + m_valuesFilename + ")" );
        }
        m_lastAttributeCount = attributeCount;

        m_nextSet++;
        return tpl.toString();
    }

    public String getAttributeValue(String attributeName) {
        // gets a named attribute for the current set
        String propertyName = "template.attribute.set." + m_nextSet + "." + attributeName;
        return m_values.getProperty(propertyName);
    }

    public void remove() {
        throw new RuntimeException("Method remove() is not supported");

    }

    // TODO: make generic, allow different forms of escaping?
    private String escapeXML(String toEscape) {
        String res = toEscape;
        // standard xml entities
        // nb cast to CharSequence to prevent regex matching
        res = res.replace("\"", "&quot;");
        res = res.replace("'", "&apos;");
        res = res.replace("&", "&amp;");
        res = res.replace("<", "&lt;");
        res = res.replace(">", "&gt;");

        return res;
    }
}
