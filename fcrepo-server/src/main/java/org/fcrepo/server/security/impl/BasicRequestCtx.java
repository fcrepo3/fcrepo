
/*
 * @(#)RequestCtx.java
 *
 * Copyright 2003-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

package org.fcrepo.server.security.impl;



import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.fcrepo.server.security.Attribute;
import org.fcrepo.server.security.RequestCtx;
import org.jboss.security.xacml.sunxacml.Indenter;
import org.jboss.security.xacml.sunxacml.ParsingException;
import org.jboss.security.xacml.sunxacml.SunxacmlUtil;
import org.jboss.security.xacml.sunxacml.ctx.Subject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Represents a request made to the PDP. This is the class that contains all
 * the data used to start a policy evaluation.
 *
 * @since 1.0
 * @author Seth Proctor
 * @author Marco Barreno
 * @author barmintor
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BasicRequestCtx implements RequestCtx
{

    // There must be at least one subject
    private List<Subject> subjects = null;

    private List<Attribute> resource = null;

    private List<Attribute> action = null;

    // There may be any number of environment attributes
    private List <Attribute>environment = null;

    // Hold onto the root of the document for XPath searches
    private Node documentRoot = null;

    // The optional, generic resource content
    private String resourceContent;

    /**
     * Constructor that creates a <code>RequestCtx</code> from components.
     *
     * @param subjects a <code>Set</code> of <code>Subject</code>s
     * @param resource a <code>Set</code> of <code>Attribute</code>s
     * @param action a <code>Set</code> of <code>Attribute</code>s
     * @param environment a <code>Set</code> of environment attributes
     */
    public BasicRequestCtx(List<Subject> subjects, List<Attribute> resource, List<Attribute> action,
                      List<Attribute> environment) {
        this(subjects, resource, action, environment, null, null);
    }

    /**
     * Constructor that creates a <code>RequestCtx</code> from components.
     *
     * @param subjects a <code>Set</code> of <code>Subject</code>s
     * @param resource a <code>Set</code> of <code>Attribute</code>s
     * @param action a <code>Set</code> of <code>Attribute</code>s
     * @param environment a <code>Set</code> of environment attributes
     * @param documentRoot the root node of the DOM tree for this request
     */
    public BasicRequestCtx(Set<Subject> subjects, Set<Attribute> resource, Set<Attribute> action, 
                      Set<Attribute> environment, Node documentRoot) {
        this(subjects, resource, action, environment, documentRoot, null);
    }
    
    /**
     * Constructor that creates a <code>RequestCtx</code> from components.
     *
     * @param subjects a <code>Set</code> of <code>Subject</code>s
     * @param resource a <code>Set</code> of <code>Attribute</code>s
     * @param action a <code>Set</code> of <code>Attribute</code>s
     * @param environment a <code>Set</code> of environment attributes
     * @param documentRoot the root node of the DOM tree for this request
     */
    public BasicRequestCtx(List<Subject> subjects, List<Attribute> resource, List<Attribute> action, 
                      List<Attribute> environment, Node documentRoot) {
        this(subjects, resource, action, environment, documentRoot, null);
    }

    /**
     * Constructor that creates a <code>RequestCtx</code> from components.
     *
     * @param subjects a <code>Set</code> of <code>Subject</code>s
     * @param resource a <code>Set</code> of <code>Attribute</code>s
     * @param action a <code>Set</code> of <code>Attribute</code>s
     * @param environment a <code>Set</code> of environment attributes
     * @param resourceContent a text-encoded version of the content, suitable
     *                        for including in the RequestType, including the
     *                        root <code>RequestContent</code> node
     */
    public BasicRequestCtx(Set<Subject> subjects, Set<Attribute> resource, Set<Attribute> action, 
                      Set<Attribute> environment, String resourceContent) {
        this(subjects, resource, action, environment, null, resourceContent);
    }

    /**
     * Constructor that creates a <code>RequestCtx</code> from components.
     *
     * @param subjects a <code>Set</code> of <code>Subject</code>s
     * @param resource a <code>Set</code> of <code>Attribute</code>s
     * @param action a <code>Set</code> of <code>Attribute</code>s
     * @param environment a <code>Set</code> of environment attributes
     * @param documentRoot the root node of the DOM tree for this request
     * @param resourceContent a text-encoded version of the content, suitable
     *                        for including in the RequestType, including the
     *                        root <code>RequestContent</code> node
     *
     * @throws IllegalArgumentException if the inputs are not well formed
     */
    public BasicRequestCtx(Set<Subject> subjects, Set<Attribute> resource, 
            Set<Attribute> action, Set<Attribute> environment, Node documentRoot,
            String resourceContent) throws IllegalArgumentException {
       
       this( new ArrayList<Subject>( subjects ), new ArrayList<Attribute>( resource ),
             new ArrayList<Attribute>(action), new ArrayList<Attribute>( environment ),
             documentRoot, resourceContent );
      
    }
    
    /**
     * Constructor that creates a <code>RequestCtx</code> from components.
     *
     * @param subjects a <code>Set</code> of <code>Subject</code>s
     * @param resource a <code>Set</code> of <code>Attribute</code>s
     * @param action a <code>Set</code> of <code>Attribute</code>s
     * @param environment a <code>Set</code> of environment attributes
     * @param documentRoot the root node of the DOM tree for this request
     * @param resourceContent a text-encoded version of the content, suitable
     *                        for including in the RequestType, including the
     *                        root <code>RequestContent</code> node
     *
     * @throws IllegalArgumentException if the inputs are not well formed
     */
    public BasicRequestCtx( List<Subject> subjects, List<Attribute> resource,
                      List<Attribute> action, List<Attribute> environment, Node documentRoot,
                      String resourceContent) throws IllegalArgumentException {
      
        this.subjects = Collections.unmodifiableList(subjects);

        this.resource = Collections.unmodifiableList( resource );

        this.action = Collections.unmodifiableList( action );
        
        this.environment =
            Collections.unmodifiableList( environment );

        this.documentRoot = documentRoot;
        this.resourceContent = resourceContent;
    }

    /**
     * Create a new <code>RequestCtx</code> by parsing a node.  This
     * node should be created by schema-verified parsing of an
     * <code>XML</code> document.
     *
     * @param root the node to parse for the <code>RequestCtx</code>
     *
     * @return a new <code>RequestCtx</code> constructed by parsing
     *
     * @throws URISyntaxException if there is a badly formed URI
     * @throws ParsingException if the DOM node is invalid
     */
    public static RequestCtx getInstance(Node root) throws ParsingException {
        List newSubjects = new ArrayList();
        List newResource = null;
        List newAction = null;
        List newEnvironment = null; 

        // First check to be sure the node passed is indeed a Request node.
        String tagName = SunxacmlUtil.getNodeName(root); 
        if (! tagName.equals("Request")) {
            throw new ParsingException("Request cannot be constructed using " +
                                       "type: " + SunxacmlUtil.getNodeName(root));
        }
        
        // Now go through its child nodes, finding Subject,
        // Resource, Action, and Environment data
        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            String tag = SunxacmlUtil.getNodeName(node);

            if (tag.equals("Subject")) {
                // see if there is a category
                Node catNode =
                    node.getAttributes().getNamedItem("SubjectCategory");
                URI category = null;

                if (catNode != null) {
                    try {
                        category = new URI(catNode.getNodeValue());
                    } catch (Exception e) {
                        throw new ParsingException("Invalid Category URI", e);
                    }
                }
                
                // now we get the attributes
                List attributes = parseAttributes(node);

                // finally, add the list to the set of subject attributes
                newSubjects.add(new Subject(category, attributes));
            } else if (tag.equals("Resource")) {
                // For now, this code doesn't parse the content, since it's
                // a set of anys with a set of anyAttributes, and therefore
                // no useful data can be gleaned from it anyway. The theory
                // here is that it's only useful in the instance doc, so
                // we won't bother parse it, but we may still want to go
                // back and provide some support at some point...
                newResource = parseAttributes(node);
            } else if (tag.equals("Action")) {
                newAction = parseAttributes(node);
            } else if (tag.equals("Environment")) {
                newEnvironment = parseAttributes(node);
            }
        }

        // if we didn't have an environment section, the only optional section
        // of the four, then create a new empty set for it
        if (newEnvironment == null)
            newEnvironment = new ArrayList();

        // Now create and return the RequestCtx from the information
        // gathered
        return new BasicRequestCtx(newSubjects, newResource,
                              newAction, newEnvironment, root);
    }

    /* 
     * Helper method that parses a set of Attribute types. The Subject,
     * Action and Environment sections all look like this.
     */
    private static List parseAttributes(Node root) throws ParsingException {
        List set = new ArrayList();

        // the Environment section is just a list of Attributes
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (SunxacmlUtil.getNodeName(node).equals("Attribute"))
                set.add(BasicAttribute.getInstance(node));
        }

        return set;
    }

    /**
     * Creates a new <code>RequestCtx</code> by parsing XML from an
     * input stream. Note that this a convenience method, and it will
     * not do schema validation by default. You should be parsing the data
     * yourself, and then providing the root node to the other
     * <code>getInstance</code> method. If you use this convenience
     * method, you probably want to turn on validation by setting the
     * context schema file (see the programmer guide for more information
     * on this).
     *
     * @param input a stream providing the XML data
     *
     * @return a new <code>RequestCtx</code>
     *
     * @throws ParserException if there is an error parsing the input
     */
    public static RequestCtx getInstance(InputStream input)
        throws ParsingException
    {
        return getInstance(InputParser.parseInput(input, "Request"));
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#getSubjects()
     */
    @Override
    public Set getSubjects() {
        return Collections.unmodifiableSet( new HashSet( subjects )) ;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#getResource()
     */
    @Override
    public Set getResource() {
        return Collections.unmodifiableSet( new HashSet( resource ));
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#getAction()
     */
    @Override
    public Set getAction() {
        return Collections.unmodifiableSet( new HashSet( action ));
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#getEnvironmentAttributes()
     */
    @Override
    public Set getEnvironmentAttributes() {
        return Collections.unmodifiableSet( new HashSet( environment ));
    }
    
    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#getSubjectsAsList()
     */
    @Override
    public List<Subject> getSubjectsAsList() {
        return subjects;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#getResourceAsList()
     */
    @Override
    public List<Attribute> getResourceAsList() {
        return resource;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#getActionAsList()
     */
    @Override
    public List<Attribute> getActionAsList() {
        return action;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#getEnvironmentAttributesAsList()
     */
    @Override
    public List<Attribute> getEnvironmentAttributesAsList() {
        return environment;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#getDocumentRoot()
     */
    @Override
    public Node getDocumentRoot() {
        return documentRoot;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#encode(java.io.OutputStream)
     */
    @Override
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }
    
    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#encode(java.io.OutputStream, java.lang.String)
     */
    @Override
    public void encode(OutputStream output, String nsURI) {
        encode(output, new Indenter(0), nsURI);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#encode(java.io.OutputStream, org.jboss.security.xacml.sunxacml.Indenter)
     */
    @Override
    public void encode(OutputStream output, Indenter indenter) {
        encode(output, indenter, null);
    }
    
    /* (non-Javadoc)
     * @see org.fcrepo.server.security.impl.RequestCtx#encode(java.io.OutputStream, org.jboss.security.xacml.sunxacml.Indenter, java.lang.String)
     */
    @Override
    public void encode(OutputStream output, Indenter indenter, String nsURI) {

        // Make a PrintStream for a nicer printing interface
        PrintStream out = new PrintStream(output);

        // Prepare the indentation string
        char[] topIndent = indenter.makeString().toCharArray();
        out.print(topIndent);
        if (nsURI != null) {
            out.append("<Request xmlns=\"").append(nsURI).println("\">");
        } else {
            out.println("<Request>");
        }
        // go in one more for next-level elements...
        indenter.in();
        char[] indent = indenter.makeString().toCharArray();

        // ...and go in again for everything else
        indenter.in();

        // first off, go through all subjects
        Iterator it = subjects.iterator();
        while (it.hasNext()) {
            Subject subject = (Subject)(it.next());
            encodeSubject(subject, out, indenter);
        }

        // next do the resource
        if ((resource.size() != 0) || (resourceContent != null)) {
            out.print(indent);
            out.println("<Resource>");
            if (resourceContent != null)
                out.append(indenter.makeString()).append("<ResourceContent>")
                   .append(resourceContent).println("</ResourceContent>");
            encodeAttributes(resource, out, indenter);
            out.print(indent);
            out.println("</Resource>");
        } else {
            out.print(indent);
            out.println("<Resource/>");
        }

        // now the action
        if (action.size() != 0) {
            out.print(indent);
            out.println("<Action>");
            encodeAttributes(action, out, indenter);
            out.print(indent);
            out.println("</Action>");
        } else {
            out.print(indent);
            out.println("<Action/>");
        }


        //Bug ID:1745062 
        out.print(indent);
        out.println("<Environment>");
        // finally the environment, if there are any attrs
        if (environment.size() != 0) {
            encodeAttributes(environment, out, indenter);
        } 
        out.print(indent);
        out.println("</Environment>");

        // we're back to the top
        indenter.out();
        indenter.out();
        
        out.print(topIndent);
        out.println("</Request>");
    } 
    
    /**
     * Private helper function to encode the attribute sets
     */
    private void encodeAttributes(List attributes, PrintStream out,
                                  Indenter indenter) {
        Iterator it = attributes.iterator();
        while (it.hasNext()) {
            Attribute attr = (Attribute)(it.next());
            attr.encode(out, indenter);
        }
    } 

    /**
     * Private helper function to encode the subjects
     */
    private void encodeSubject(Subject subject, PrintStream out,
                                  Indenter indenter) {
        char [] indent = indenter.makeString().toCharArray();
        out.print(indent);
        out.append("<Subject SubjectCategory=\"")
           .append(subject.getCategory().toString()).append('"');

        List subjectAttrs = subject.getAttributesAsList();
        
        if (subjectAttrs.size() == 0) {
            // there's nothing in this Subject, so just close the tag
            out.println("/>");
        } else {
            // there's content, so fill it in
            out.println('>');

            encodeAttributes(subjectAttrs, out, indenter);
        
            out.print(indent);
            out.println("</Subject>");
        }
    }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((action == null) ? 0 : action.hashCode()); 
      result = prime * result + ((environment == null) ? 0 : environment.hashCode());
      result = prime * result + ((resource == null) ? 0 : resource.hashCode()); 
      result = prime * result + ((subjects == null) ? 0 : subjects.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      BasicRequestCtx other = (BasicRequestCtx) obj;
      if (action == null) {
          if (other.action != null) return false;
      } else if (!action.equals(other.action)) {
          return false; 
      }
      if (environment == null) { 
          if (other.environment != null) return false;
      } else if (!environment.equals(other.environment)) {
          return false;
      }
      if (resource == null) {
          if (other.resource != null) return false;
      } else if (!resource.equals(other.resource)) {
          return false;
      }
      if (resourceContent == null) {
          if (other.resourceContent != null) return false;
      } else if (!resourceContent.equals(other.resourceContent)) {
         return false;
      }
      if (subjects == null) {
          if (other.subjects != null) return false;
      }
      else if (!subjects.equals(other.subjects)) {
          return false;
      }
      return true;
   } 
}