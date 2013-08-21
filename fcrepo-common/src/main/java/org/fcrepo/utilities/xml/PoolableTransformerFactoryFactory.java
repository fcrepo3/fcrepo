package org.fcrepo.utilities.xml;

import javax.xml.transform.TransformerFactory;

import org.apache.commons.pool.PoolableObjectFactory;


public class PoolableTransformerFactoryFactory
implements PoolableObjectFactory<TransformerFactory> {

    public PoolableTransformerFactoryFactory() {

    }
    
    @Override
    public void activateObject(TransformerFactory object) throws Exception {
        // no-op
    }
    
    @Override
    public void destroyObject(TransformerFactory object) throws Exception {
        // no-op
    }

    @Override
    public TransformerFactory makeObject() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        if (factory.getClass().getName().equals("net.sf.saxon.TransformerFactoryImpl")) {
            factory.setAttribute("http://saxon.sf.net/feature/version-warning", Boolean.FALSE);
        }
        return factory;
    }

    @Override
    public void passivateObject(TransformerFactory object) throws Exception {
        // no-op
    }

    @Override
    public boolean validateObject(TransformerFactory object) {
        return true;
    }

}
