package org.fcrepo.utilities.xml;

import javax.xml.transform.TransformerFactory;

import org.apache.commons.pool.PoolableObjectFactory;


public class PoolableTransformerFactoryFactory implements PoolableObjectFactory {

    public PoolableTransformerFactoryFactory() {

    }
    
    @Override
    public void activateObject(Object object) throws Exception {
        // no-op
    }
    
    @Override
    public void destroyObject(Object object) throws Exception {
        // no-op
    }

    @Override
    public Object makeObject() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        if (factory.getClass().getName().equals("net.sf.saxon.TransformerFactoryImpl")) {
            factory.setAttribute("http://saxon.sf.net/feature/version-warning", Boolean.FALSE);
        }
        return factory;
    }

    @Override
    public void passivateObject(Object object) throws Exception {
        // no-op
    }

    @Override
    public boolean validateObject(Object object) {
        return true;
    }

}
