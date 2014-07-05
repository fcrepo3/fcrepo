
package org.fcrepo.server.security;

import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import org.jboss.security.xacml.sunxacml.Indenter;
import org.jboss.security.xacml.sunxacml.ctx.Subject;
import org.w3c.dom.Node;

public interface RequestCtx {

    /**
     * Returns a <code>Set</code> containing <code>Subject</code> objects.
     *
     * @return the request's subject attributes
     * @deprecated
     */
    public abstract Set<Subject> getSubjects();

    /**
     * Returns a <code>Set</code> containing <code>Attribute</code> objects.
     *
     * @return the request's resource attributes
     * @deprecated
     */
    public abstract Set<Attribute> getResource();

    /**
     * Returns a <code>Set</code> containing <code>Attribute</code> objects.
     *
     * @return the request's action attributes
     * @deprecated
     */
    public abstract Set<Attribute> getAction();

    /**
     * Returns a <code>Set</code> containing <code>Attribute</code> objects.
     *
     * @return the request's environment attributes
     * @deprecated
     */
    public abstract Set<Attribute> getEnvironmentAttributes();

    /**
     * Returns a <code>List</code> containing <code>Subject</code> objects.
     *
     * @return the request's subject attributes
     */
    public abstract List<Subject> getSubjectsAsList();

    /**
     * Returns a <code>List</code> containing <code>Attribute</code> objects.
     *
     * @return the request's resource attributes
     */
    public abstract List<Attribute> getResourceAsList();

    /**
     * Returns a <code>List</code> containing <code>Attribute</code> objects.
     *
     * @return the request's action attributes
     */
    public abstract List<Attribute> getActionAsList();

    /**
     * Returns a <code>List</code> containing <code>Attribute</code> objects.
     *
     * @return the request's environment attributes
     */
    public abstract List<Attribute> getEnvironmentAttributesAsList();

    /**
     * Returns the root DOM node of the document used to create this
     * object, or null if this object was created by hand (ie, not through
     * the <code>getInstance</code> method) or if the root node was not
     * provided to the constructor.
     *
     * @return the root DOM node or null
     */
    public abstract Node getDocumentRoot();

    /**
     * Encodes this context into its XML representation and writes this
     * encoding to the given <code>OutputStream</code>.  No
     * indentation is used.
     *
     * @param output a stream into which the XML-encoded data is written
     */
    public abstract void encode(OutputStream output);

    public abstract void encode(OutputStream output, String nsURI);

    /**
     * Encodes this context into its XML representation and writes
     * this encoding to the given <code>OutputStream</code> with
     * indentation.
     *
     * @param output a stream into which the XML-encoded data is written
     * @param indenter an object that creates indentation strings
     */
    public abstract void encode(OutputStream output, Indenter indenter);

    public abstract void encode(OutputStream output, Indenter indenter,
            String nsURI);

}