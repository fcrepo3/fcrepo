
package org.fcrepo.server.security.xacml.pdp.client;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.fcrepo.server.security.xacml.pdp.client package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.fcrepo.server.security.xacml.pdp.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link EvaluateBatchResponse }
     * 
     */
    public EvaluateBatchResponse createEvaluateBatchResponse() {
        return new EvaluateBatchResponse();
    }

    /**
     * Create an instance of {@link EvaluateBatch }
     * 
     */
    public EvaluateBatch createEvaluateBatch() {
        return new EvaluateBatch();
    }

    /**
     * Create an instance of {@link EvaluateResponse }
     * 
     */
    public EvaluateResponse createEvaluateResponse() {
        return new EvaluateResponse();
    }

    /**
     * Create an instance of {@link EvaluateBatchFault }
     * 
     */
    public EvaluateBatchFault createEvaluateBatchFault() {
        return new EvaluateBatchFault();
    }

    /**
     * Create an instance of {@link Evaluate }
     * 
     */
    public Evaluate createEvaluate() {
        return new Evaluate();
    }

    /**
     * Create an instance of {@link EvaluateFault }
     * 
     */
    public EvaluateFault createEvaluateFault() {
        return new EvaluateFault();
    }

}
