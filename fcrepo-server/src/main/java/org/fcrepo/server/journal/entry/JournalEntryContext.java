/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.journal.entry;

import java.net.URI;
import java.util.Date;
import java.util.Iterator;

import org.fcrepo.server.Context;
import org.fcrepo.server.MultiValueMap;
import org.fcrepo.server.RecoveryContext;


/**
 * A fully writable context that can be used when recovering entries from a
 * Journal.
 * 
 * @author Jim Blake
 */
public class JournalEntryContext
        implements RecoveryContext {

    private MultiValueMap<URI> environmentAttributes = new MultiValueMap<URI>();

    private MultiValueMap<String> subjectAttributes = new MultiValueMap<String>();

    private MultiValueMap<URI> actionAttributes = new MultiValueMap<URI>();

    private MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();

    private MultiValueMap<URI> recoveryAttributes = new MultiValueMap<URI>();

    private String password = "";

    private boolean noOp = false;

    private Date now = new Date();

    /**
     * Create an empty context.
     */
    public JournalEntryContext() {
        // nothing to do
    }

    /**
     * A "copy constructor" that creates a writable context from one that might
     * be read-only.
     */
    public JournalEntryContext(Context source) {
        password = source.getPassword();
        noOp = source.getNoOp();
        now = source.now();
        for (Iterator<URI> keys = source.environmentAttributes(); keys.hasNext();) {
            URI key = keys.next();
            storeInMap(environmentAttributes, key, source
                    .getEnvironmentValues(key));
        }
        for (Iterator<String> keys = source.subjectAttributes(); keys.hasNext();) {
            String key = keys.next();
            storeInMap(subjectAttributes, key, source.getSubjectValues(key));
        }
        for (Iterator<URI> keys = source.actionAttributes(); keys.hasNext();) {
            URI key = keys.next();
            storeInMap(actionAttributes, key, source.getActionValues(key));
        }
        for (Iterator<URI> keys = source.resourceAttributes(); keys.hasNext();) {
            URI key = keys.next();
            storeInMap(resourceAttributes, key, source.getResourceValues(key));
        }
    }

    /**
     * This method covers the totally bogus Exception that is thrown by
     * MultiValueMap.set(), and wraps it in an IllegalArgumentException, which
     * is more appropriate.
     * @param <T>
     */
    private <T> void storeInMap(MultiValueMap<T> map, T key, String[] values) {
        try {
            map.set(key, values);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public MultiValueMap<URI> getEnvironmentAttributes() {
        return environmentAttributes;
    }

    public Iterator<URI> environmentAttributes() {
        return environmentAttributes.names();
    }

    public int nEnvironmentValues(URI name) {
        return environmentAttributes.length(name);
    }

    public String getEnvironmentValue(URI name) {
        return environmentAttributes.getString(name);
    }

    public String[] getEnvironmentValues(URI name) {
        return environmentAttributes.getStringArray(name);
    }

    public Iterator<String> subjectAttributes() {
        return subjectAttributes.names();
    }

    public int nSubjectValues(String name) {
        return subjectAttributes.length(name);
    }

    public String getSubjectValue(String name) {
        return subjectAttributes.getString(name);
    }

    public String[] getSubjectValues(String name) {
        return subjectAttributes.getStringArray(name);
    }

    public Iterator<URI> actionAttributes() {
        return actionAttributes.names();
    }

    public int nActionValues(URI name) {
        return actionAttributes.length(name);
    }

    public String getActionValue(URI name) {
        return actionAttributes.getString(name);
    }

    public String[] getActionValues(URI name) {
        return actionAttributes.getStringArray(name);
    }

    public Iterator<URI> resourceAttributes() {
        return resourceAttributes.names();
    }

    public int nResourceValues(URI name) {
        return resourceAttributes.length(name);
    }

    public String getResourceValue(URI name) {
        return resourceAttributes.getString(name);
    }

    public String[] getResourceValues(URI name) {
        return resourceAttributes.getStringArray(name);
    }

    public void setActionAttributes(MultiValueMap<URI> actionAttributes) {
        if (actionAttributes == null) {
            actionAttributes = new MultiValueMap<URI>();
        }
        this.actionAttributes = actionAttributes;
    }

    public void setResourceAttributes(MultiValueMap<URI> resourceAttributes) {
        if (resourceAttributes == null) {
            resourceAttributes = new MultiValueMap<URI>();
        }
        this.resourceAttributes = resourceAttributes;
    }

    public String getPassword() {
        return password;
    }

    public Date now() {
        return now;
    }

    public boolean getNoOp() {
        return noOp;
    }

    public Iterator<URI> getRecoveryNames() {
        return recoveryAttributes.names();
    }

    public String getRecoveryValue(URI attribute) {
        return recoveryAttributes.getString(attribute);
    }

    public String[] getRecoveryValues(URI attribute) {
        return recoveryAttributes.getStringArray(attribute);
    }

    // -------------------------------------------------------------------------
    // Additional methods
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return this.getClass().getName() + "[environmentAttributes="
                + environmentAttributes + ", subjectAttributes="
                + subjectAttributes + ", actionAttributes=" + actionAttributes
                + ", resourceAttributes=" + resourceAttributes
                + ", recoveryAttributes=" + recoveryAttributes + ", password="
                + password + ", noOp=" + noOp + ", date=" + now + "]\n";
    }

    // Make the class fully read/write.

    public MultiValueMap<URI> getActionAttributes() {
        return actionAttributes;
    }

    public void setEnvironmentAttributes(MultiValueMap<URI> environmentAttributes) {
        this.environmentAttributes = environmentAttributes;
    }

    public void setSubjectAttributes(MultiValueMap<String> subjectAttributes) {
        this.subjectAttributes = subjectAttributes;
    }

    public MultiValueMap<String> getSubjectAttributes() {
        return subjectAttributes;
    }

    public MultiValueMap<URI> getResourceAttributes() {
        return resourceAttributes;
    }

    public void setRecoveryAttributes(MultiValueMap<URI> recoveryAttributes) {
        this.recoveryAttributes = recoveryAttributes;
    }

    public MultiValueMap<URI> getRecoveryAttributes() {
        return recoveryAttributes;
    }

    public void setRecoveryValue(URI attribute, String value) {
        setRecoveryValues(attribute, new String[] {value});
    }

    public void setRecoveryValues(URI attribute, String[] values) {
        storeInMap(recoveryAttributes, attribute, values);
    }

    public void setNoOp(boolean noOp) {
        this.noOp = noOp;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setNow(Date now) {
        this.now = now;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!obj.getClass().equals(JournalEntryContext.class)) {
            return false;
        }
        JournalEntryContext that = (JournalEntryContext) obj;

        return environmentAttributes.equals(that.environmentAttributes)
                && subjectAttributes.equals(that.subjectAttributes)
                && actionAttributes.equals(that.actionAttributes)
                && resourceAttributes.equals(that.resourceAttributes)
                && recoveryAttributes.equals(that.recoveryAttributes)
                && password.equals(that.password) && noOp == that.noOp
                && now.equals(that.now);
    }

    @Override
    public int hashCode() {
        return environmentAttributes.hashCode() ^ subjectAttributes.hashCode()
                ^ actionAttributes.hashCode() ^ resourceAttributes.hashCode()
                ^ environmentAttributes.hashCode() ^ password.hashCode()
                ^ now.hashCode() + (noOp ? 0 : 1);
    }

    @Override
    public MultiValueMap<String> getHeaders() {
        return null;
    }

    @Override
    public String getHeaderValue(String name) {
        return null;
    }

    @Override
    public String[] getHeaderValues(String name) {
        return null;
    }
}
