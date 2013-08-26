package org.fcrepo.server.security.xacml.pdp.finder.attribute;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.fcrepo.server.security.AttributeFinderModule;
import org.fcrepo.server.security.xacml.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeDesignator;


public abstract class DesignatorAttributeFinderModule
                 extends AttributeFinderModule{

    private static final Logger logger =
            LoggerFactory.getLogger(DesignatorAttributeFinderModule.class);

    protected final Map<Integer,Map<String,Attribute>> m_attributes = new HashMap<Integer,Map<String,Attribute>>();

    public void setActionAttributes(Map<String,Attribute> attributes){
        setAttributes(AttributeDesignator.ACTION_TARGET,attributes);
    }

    public void setEnvironmentAttributes(Map<String,Attribute> attributes){
        setAttributes(AttributeDesignator.ENVIRONMENT_TARGET,attributes);
    }

    public void setResourceAttributes(Map<String,Attribute> attributes){
        setAttributes(AttributeDesignator.RESOURCE_TARGET,attributes);
    }

    public void setSubjectAttributes(Map<String,Attribute> attributes){
        setAttributes(AttributeDesignator.SUBJECT_TARGET,attributes);
    }

    protected void setAttributes(int designator, Map<String,Attribute> attributes){
        m_attributes.put(Integer.valueOf(designator),attributes);
        if (logger.isDebugEnabled()) {
            logger.debug("registering the following attributes: ");
            for (String attrName : attributes.keySet()) {
                logger.debug("{}: {}", designator, attrName);
            }
        }

    }


    protected boolean emptyAttributeMap() {
        return m_attributes.size() == 0;
    }

    /**
     * Returns true always because this module supports designators.
     *
     * @return true always
     */
    @Override
    public boolean isDesignatorSupported() {
        return true;
    }

    /**
     * Returns a <code>Set</code> with a single <code>Integer</code> specifying
     * that environment attributes are supported by this module.
     *
     * @return a <code>Set</code> with
     *         <code>AttributeDesignator.ENVIRONMENT_TARGET</code> included
     */
    @Override
    public Set<Integer> getSupportedDesignatorTypes() {
        return m_attributes.keySet();
    }

    @Override
    protected boolean canHandleAdhoc() {
                return false;
    }

    /**
     * Will not be called in this implementation, since findAttribute is overridden
     * {@inheritDoc}
     */
    @Override
    protected Object getAttributeLocally(int designatorType,
                                         URI attributeId,
                                         URI resourceCategory,
                                         EvaluationCtx context) {
                return null;

    }
}
