/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import java.net.URI;
import java.util.Date;
import java.util.Iterator;


/**
 * Context object for testing.
 * Currently, only getSubjectValue() and now() are implemented.
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class MockContext
        implements Context {

    /**
     * {@inheritDoc}
     */
    public Iterator<URI> actionAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<URI> environmentAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getActionValue(URI name) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getActionValues(URI name) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public MultiValueMap<URI> getEnvironmentAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getEnvironmentValue(URI name) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getEnvironmentValues(URI name) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean getNoOp() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getPassword() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getResourceValue(URI name) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getResourceValues(URI name) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getSubjectValue(String name) {
        return "fedoraAdmin";
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSubjectValues(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int nActionValues(URI name) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int nEnvironmentValues(URI name) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int nResourceValues(URI name) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int nSubjectValues(String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public Date now() {
        return new Date();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<URI> resourceAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setActionAttributes(MultiValueMap<URI> actionAttributes) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    public void setResourceAttributes(MultiValueMap<URI> resourceAttributes) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    public Iterator<String> subjectAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MultiValueMap<String> getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHeaderValue(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getHeaderValues(String name) {
        // TODO Auto-generated method stub
        return null;
    }

}
