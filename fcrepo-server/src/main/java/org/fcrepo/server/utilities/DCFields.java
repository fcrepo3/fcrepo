/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import java.io.IOException;
import java.io.InputStream;

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

    private final ArrayList<DCField> m_titles = new ArrayList<DCField>();

    private final ArrayList<DCField> m_creators = new ArrayList<DCField>();

    private final ArrayList<DCField> m_subjects = new ArrayList<DCField>();

    private final ArrayList<DCField> m_descriptions = new ArrayList<DCField>();

    private final ArrayList<DCField> m_publishers = new ArrayList<DCField>();

    private final ArrayList<DCField> m_contributors = new ArrayList<DCField>();

    private final ArrayList<DCField> m_dates = new ArrayList<DCField>();

    private final ArrayList<DCField> m_types = new ArrayList<DCField>();

    private final ArrayList<DCField> m_formats = new ArrayList<DCField>();

    private final ArrayList<DCField> m_identifiers = new ArrayList<DCField>();

    private final ArrayList<DCField> m_sources = new ArrayList<DCField>();

    private final ArrayList<DCField> m_languages = new ArrayList<DCField>();

    private final ArrayList<DCField> m_relations = new ArrayList<DCField>();

    private final ArrayList<DCField> m_coverages = new ArrayList<DCField>();

    private final ArrayList<DCField> m_rights = new ArrayList<DCField>();

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
        Map<RDFName, List<DCField>> map = new HashMap<RDFName, List<DCField>>();

        map.put(DC.TITLE, m_titles);
        map.put(DC.CREATOR, m_creators);
        map.put(DC.SUBJECT, m_subjects);
        map.put(DC.DESCRIPTION, m_descriptions);
        map.put(DC.PUBLISHER, m_publishers);
        map.put(DC.CONTRIBUTOR, m_contributors);
        map.put(DC.DATE, m_dates);
        map.put(DC.TYPE, m_types);
        map.put(DC.FORMAT, m_formats);
        map.put(DC.IDENTIFIER, m_identifiers);
        map.put(DC.SOURCE, m_sources);
        map.put(DC.LANGUAGE, m_languages);
        map.put(DC.RELATION, m_relations);
        map.put(DC.COVERAGE, m_coverages);
        map.put(DC.RIGHTS, m_rights);

        return map;
    }

    public List<DCField> titles() {
        return m_titles;
    }

    public List<DCField> creators() {
        return m_creators;
    }

    public List<DCField> subjects() {
        return m_subjects;
    }

    public List<DCField> descriptions() {
        return m_descriptions;
    }

    public List<DCField> publishers() {
        return m_publishers;
    }

    public List<DCField> contributors() {
        return m_contributors;
    }

    public List<DCField> dates() {
        return m_dates;
    }

    public List<DCField> types() {
        return m_types;
    }

    public List<DCField> formats() {
        return m_formats;
    }

    public List<DCField> identifiers() {
        return m_identifiers;
    }

    public List<DCField> sources() {
        return m_sources;
    }

    public List<DCField> languages() {
        return m_languages;
    }

    public List<DCField> relations() {
        return m_relations;
    }

    public List<DCField> coverages() {
        return m_coverages;
    }

    public List<DCField> rights() {
        return m_rights;
    }

    /**
     * Get the DCFields as a String in namespace-qualified XML form, matching
     * the oai_dc schema.... but without the xml declaration.
     */
    public String getAsXML() {
        return getAsXML(null);
    }
    
    /**
     * Ensure the dc:identifiers include the pid of the target object
            * @param targetPid
            * @return
     */
    public String getAsXML(String targetPid) {
        boolean addPid = (targetPid != null);
        if (addPid) {
        for (DCField dcField : identifiers()) {
            if (dcField.getValue().equals(targetPid)) {
                addPid = false;
            }
        }
        }
        StringBuffer out = new StringBuffer();
        out.append("<" + OAI_DC.prefix + ":dc" + " xmlns:" + OAI_DC.prefix
                + "=\"" + OAI_DC.uri + "\"" + "\nxmlns:" + DC.prefix + "=\""
                + DC.uri + "\"\nxmlns:xsi=\"" + XSI.uri
                + "\"\nxsi:schemaLocation=\"" + OAI_DC.uri + " "
                + OAI_DC2_0.xsdLocation + "\">\n");
        appendXML(titles(), "title", out);
        appendXML(creators(), "creator", out);
        appendXML(subjects(), "subject", out);
        appendXML(descriptions(), "description", out);
        appendXML(publishers(), "publisher", out);
        appendXML(contributors(), "contributor", out);
        appendXML(dates(), "date", out);
        appendXML(types(), "type", out);
        appendXML(formats(), "format", out);
        if (addPid) {
            appendXML(new DCField(targetPid), "identifier", out);
        }
        appendXML(identifiers(), "identifier", out);
        appendXML(sources(), "source", out);
        appendXML(languages(), "language", out);
        appendXML(relations(), "relation", out);
        appendXML(coverages(), "coverage", out);
        appendXML(rights(), "rights", out);
        out.append("</oai_dc:dc>\n");
        return out.toString();    }

    private void appendXML(List<DCField> values, String name, StringBuffer out) {
        for (DCField value : values) {
            appendXML(value, name, out);
        }
    }
    private void appendXML(DCField value, String name, StringBuffer out) {
        out.append("  <dc:" + name);
        if (value.getLang() != null) {
            out.append(" xml:lang=\"" + value.getLang() + "\"");
        }
        out.append(">");
        out.append(StreamUtility.enc(value.getValue()));
        out.append("</dc:" + name + ">\n");
    }
}
