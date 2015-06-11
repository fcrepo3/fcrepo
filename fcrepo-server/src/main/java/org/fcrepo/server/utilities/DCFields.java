/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.fcrepo.common.Constants;
import org.fcrepo.common.rdf.RDFName;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.RepositoryConfigurationException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.utilities.ReadableCharArrayWriter;
import org.fcrepo.utilities.XmlTransformUtility;



import javax.xml.XMLConstants;

/**
 * Dublin Core Fields.
 * 
 * @author Chris Wilper
 * @version $Id$
 */
public class DCFields
        extends DefaultHandler
        implements Constants {
    
    private static final char[] TITLE = "title".toCharArray();
    private static final char[] CREATOR = "creator".toCharArray();
    private static final char[] SUBJECT = "subject".toCharArray();
    private static final char[] DESCRIPTION = "description".toCharArray();
    private static final char[] PUBLISHER = "publisher".toCharArray();
    private static final char[] CONTRIBUTOR = "contributor".toCharArray();
    private static final char[] DATE = "date".toCharArray();
    private static final char[] TYPE = "type".toCharArray();
    private static final char[] FORMAT = "format".toCharArray();
    private static final char[] IDENTIFIER = "identifier".toCharArray();
    private static final char[] SOURCE = "source".toCharArray();
    private static final char[] LANGUAGE = "language".toCharArray();
    private static final char[] RELATION = "relation".toCharArray();
    private static final char[] COVERAGE = "coverage".toCharArray();
    private static final char[] RIGHTS = "rights".toCharArray();
    
    private static final char[] XML_OPEN = xmlOpen();
    private static final char[] XML_CLOSE = "</oai_dc:dc>\n".toCharArray();
    private static final char[] DC_OPEN_ELEMENT_PREFIX =
            "  <dc:".toCharArray();
    private static final char[] DC_CLOSE_ELEMENT_PREFIX =
            "</dc:".toCharArray();
    private static final char[] XML_LANG_START =
            " xml:lang=\"".toCharArray();
    private static final char[] BRACKET_NEWLINE =
            new char[]{'>','\n'};

    private ArrayList<DCField> m_titles = null;

    private ArrayList<DCField> m_creators = null;

    private ArrayList<DCField> m_subjects = null;

    private ArrayList<DCField> m_descriptions = null;

    private ArrayList<DCField> m_publishers = null;

    private ArrayList<DCField> m_contributors = null;

    private ArrayList<DCField> m_dates = null;

    private ArrayList<DCField> m_types = null;

    private ArrayList<DCField> m_formats = null;

    private ArrayList<DCField> m_identifiers = null;

    private ArrayList<DCField> m_sources = null;

    private ArrayList<DCField> m_languages = null;

    private ArrayList<DCField> m_relations = null;

    private ArrayList<DCField> m_coverages = null;

    private ArrayList<DCField> m_rights = null;

    private StringBuffer m_currentContent;
    
    private String m_lang;

    public DCFields() {}

    public DCFields(InputStream in)
            throws RepositoryConfigurationException, ObjectIntegrityException,
            StreamIOException {
        try {
            XmlTransformUtility.parseWithoutValidating(in, this);
        } catch (SAXException saxe) {
            throw new ObjectIntegrityException("Parse error parsing DC XML Metadata: "
                    + saxe.getMessage());
        } catch (IOException ioe) {
            throw new StreamIOException("Stream error parsing DC XML Metadata: "
                    + ioe.getMessage());
        }
    }

    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes attrs) {
        m_currentContent = new StringBuffer();
        m_lang = attrs.getValue(XMLConstants.XML_NS_URI, "lang");
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        m_currentContent.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (localName.equals("title")) {
            titles().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("creator")) {
            creators().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("subject")) {
            subjects().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("description")) {
            descriptions().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("publisher")) {
            publishers().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("contributor")) {
            contributors().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("date")) {
            dates().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("type")) {
            types().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("format")) {
            formats().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("identifier")) {
            identifiers().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("source")) {
            sources().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("language")) {
            languages().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("relation")) {
            relations().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("coverage")) {
            coverages().add(new DCField(m_currentContent.toString().trim(), m_lang));
        } else if (localName.equals("rights")) {
            rights().add(new DCField(m_currentContent.toString().trim(), m_lang));
        }
    }

    /**
     * Returns a Map with RDFName keys, each value containing List of String
     * values for that field.
     */
    public Map<RDFName, List<DCField>> getMap() {
        Map<RDFName, List<DCField>> map = new HashMap<RDFName, List<DCField>>(15);

        if (m_titles != null) map.put(DC.TITLE, m_titles);
        if (m_creators != null) map.put(DC.CREATOR, m_creators);
        if (m_subjects != null) map.put(DC.SUBJECT, m_subjects);
        if (m_descriptions != null) map.put(DC.DESCRIPTION, m_descriptions);
        if (m_publishers != null) map.put(DC.PUBLISHER, m_publishers);
        if (m_contributors != null) map.put(DC.CONTRIBUTOR, m_contributors);
        if (m_dates != null) map.put(DC.DATE, m_dates);
        if (m_types != null) map.put(DC.TYPE, m_types);
        if (m_formats != null) map.put(DC.FORMAT, m_formats);
        if (m_identifiers != null) map.put(DC.IDENTIFIER, m_identifiers);
        if (m_sources != null) map.put(DC.SOURCE, m_sources);
        if (m_languages != null) map.put(DC.LANGUAGE, m_languages);
        if (m_relations != null) map.put(DC.RELATION, m_relations);
        if (m_coverages != null) map.put(DC.COVERAGE, m_coverages);
        if (m_rights != null) map.put(DC.RIGHTS, m_rights);

        return map;
    }

    public List<DCField> titles() {
        if (m_titles == null) m_titles = new ArrayList<DCField>(2);
        return m_titles;
    }

    public List<DCField> creators() {
        if (m_creators == null) m_creators = new ArrayList<DCField>(2);
        return m_creators;
    }

    public List<DCField> subjects() {
        if (m_subjects == null) m_subjects = new ArrayList<DCField>(2);
        return m_subjects;
    }

    public List<DCField> descriptions() {
        if (m_descriptions == null) m_descriptions = new ArrayList<DCField>(2);
        return m_descriptions;
    }

    public List<DCField> publishers() {
        if (m_publishers == null) m_publishers = new ArrayList<DCField>(2);
        return m_publishers;
    }

    public List<DCField> contributors() {
        if (m_contributors == null) m_contributors = new ArrayList<DCField>(2);
        return m_contributors;
    }

    public List<DCField> dates() {
        if (m_dates == null) m_dates = new ArrayList<DCField>(2);
        return m_dates;
    }

    public List<DCField> types() {
        if (m_types == null) m_types = new ArrayList<DCField>(2);
        return m_types;
    }

    public List<DCField> formats() {
        if (m_formats == null) m_formats = new ArrayList<DCField>(2);
        return m_formats;
    }

    public List<DCField> identifiers() {
        if (m_identifiers == null) m_identifiers = new ArrayList<DCField>(2);
        return m_identifiers;
    }

    public List<DCField> sources() {
        if (m_sources == null) m_sources = new ArrayList<DCField>(2);
        return m_sources;
    }

    public List<DCField> languages() {
        if (m_languages == null) m_languages = new ArrayList<DCField>(2);
        return m_languages;
    }

    public List<DCField> relations() {
        if (m_relations == null) m_relations = new ArrayList<DCField>(2);
        return m_relations;
    }

    public List<DCField> coverages() {
        if (m_coverages == null) m_coverages = new ArrayList<DCField>(2);
        return m_coverages;
    }

    public List<DCField> rights() {
        if (m_rights == null) m_rights = new ArrayList<DCField>(2);
        return m_rights;
    }

    /**
     * Get the DCFields as a String in namespace-qualified XML form, matching
     * the oai_dc schema.... but without the xml declaration.
     */
    public String getAsXML() {
        return getAsXML((String)null);
    }
    
    public void getAsXML(Writer out) throws IOException {
        getAsXML(null, out);
    }

    /**
     * Ensure the dc:identifiers include the pid of the target object
            * @param targetPid
            * @return String xml
     */
    public String getAsXML(String targetPid) {
        ReadableCharArrayWriter out = new ReadableCharArrayWriter(512);
        try {
            getAsXML(targetPid, out);
        } catch (IOException wonthappen) {
            throw new RuntimeException(wonthappen);
        }
        return out.getString();
    }
    
    public void getAsXML(String targetPid, Writer out) throws IOException {
        boolean addPid = (targetPid != null);
        if (addPid) {
        for (DCField dcField : identifiers()) {
            if (dcField.getValue().equals(targetPid)) {
                addPid = false;
            }
        }
        }
        out.write(XML_OPEN);
        appendXML(m_titles, TITLE, out);
        appendXML(m_creators, CREATOR, out);
        appendXML(m_subjects, SUBJECT, out);
        appendXML(m_descriptions, DESCRIPTION, out);
        appendXML(m_publishers, PUBLISHER, out);
        appendXML(m_contributors, CONTRIBUTOR, out);
        appendXML(m_dates, DATE, out);
        appendXML(m_types, TYPE, out);
        appendXML(m_formats, FORMAT, out);
        if (addPid) {
            appendXML(new DCField(targetPid), IDENTIFIER, out);
        }
        appendXML(m_identifiers, IDENTIFIER, out);
        appendXML(m_sources, SOURCE, out);
        appendXML(m_languages, LANGUAGE, out);
        appendXML(m_relations, RELATION, out);
        appendXML(m_coverages, COVERAGE, out);
        appendXML(m_rights, RIGHTS, out);
        out.write(XML_CLOSE);
    }

    private void appendXML(List<DCField> values, char[] name, Writer out)
        throws IOException {
        if (values == null || values.size() == 0) return;
        for (DCField value : values) {
            appendXML(value, name, out);
        }
    }
    
    private void appendXML(DCField value, char[] name, Writer out)
        throws IOException {
        out.write(DC_OPEN_ELEMENT_PREFIX);
        out.write(name);
        if (value.getLang() != null) {
            out.write(XML_LANG_START);
            out.append(value.getLang()).append('"');
        }
        out.append('>');
        StreamUtility.enc(value.getValue(), out);
        out.write(DC_CLOSE_ELEMENT_PREFIX);
        out.write(name);
        out.write(BRACKET_NEWLINE);
    }
    
    private static final char[] xmlOpen() {
        return ("<" + OAI_DC.prefix + ":dc" + " xmlns:" + OAI_DC.prefix
                + "=\"" + OAI_DC.uri + "\"" + "\nxmlns:" + DC.prefix + "=\""
                + DC.uri + "\"\nxmlns:xsi=\"" + XSI.uri
                + "\"\nxsi:schemaLocation=\"" + OAI_DC.uri + " "
                + OAI_DC2_0.xsdLocation + "\">\n").toCharArray();
    }
}
