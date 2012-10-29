        package org.fcrepo.server.security.xacml.pep;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Map;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;
        
/**
 * A utility class to hold some static methods used by both the REST and SOAP authZ handlers
 *
 * @author Benjamin Armintor
 */
public abstract class ResourceAttributes {
    private static final Logger logger = LoggerFactory
            .getLogger(ResourceAttributes.class);

    public static Map<URI, AttributeValue> getRepositoryResources() {
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();
        resAttr.put(Constants.OBJECT.PID.getURI(),
                    Constants.FEDORA_REPOSITORY_PID.getStringAttribute());
        resAttr.put(Constants.XACML1_RESOURCE.ID.getURI(),
                    Constants.FEDORA_REPOSITORY_PID.getURIAttribute());
        return resAttr;
    }

    public static Map<URI, AttributeValue> getResources(String pid) {
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();
        if (pid != null && !"".equals(pid)) {
            resAttr.put(Constants.OBJECT.PID.getURI(),
                        new StringAttribute(pid));
            try{
                resAttr.put(Constants.XACML1_RESOURCE.ID.getURI(),
                            new AnyURIAttribute(new URI(pid)));
            } catch (URISyntaxException e) {
                logger.warn("pid {} is not a valid uri; write policies against the StringAttribute {} instead.",
                            pid,
                            Constants.OBJECT.PID.uri);
                resAttr.put(Constants.XACML1_RESOURCE.ID.getURI(),
                            new StringAttribute(pid));
            }
        }
        return resAttr;
    }
    
}

    