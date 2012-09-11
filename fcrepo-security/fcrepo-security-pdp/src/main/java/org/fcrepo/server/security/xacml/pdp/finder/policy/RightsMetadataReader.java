package org.fcrepo.server.security.xacml.pdp.finder.policy;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

        
public class RightsMetadataReader implements org.xml.sax.ContentHandler {
    private Map<String,Set<String>> assertions = new HashMap<String,Set<String>>();
    public Map<String,Set<String>> getAssertions() {
        return this.assertions;
    }
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
            
    }

    @Override
    public void endDocument() throws SAXException {
            
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
            
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
            
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
            
    }

    @Override
    public void processingInstruction(String target, String data)
            throws SAXException {
            
    }

    @Override
    public void setDocumentLocator(Locator locator) {
            
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
            
    }

    @Override
    public void startDocument() throws SAXException {
            
    }

    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes atts) throws SAXException {
            if (localName.equals("access")) {
                
            }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
            
    }

}

    