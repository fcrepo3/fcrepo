package org.fcrepo.utilities.xml;

import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.pool.PoolableObjectFactory;


public class PoolableDocumentBuilderFactory
implements PoolableObjectFactory<DocumentBuilder> {

    private final DocumentBuilderFactory m_factory;
    
    private final ReentrantLock m_lock = new ReentrantLock();
    
    public PoolableDocumentBuilderFactory(boolean namespaceAware, boolean ignoreComments) {
        m_factory =
                DocumentBuilderFactory.newInstance();
        m_factory.setNamespaceAware(namespaceAware);
        m_factory.setIgnoringComments(ignoreComments);
    }
    
    @Override
    public void activateObject(DocumentBuilder object) throws Exception {
        object.reset();
    }
    
    @Override
    public void destroyObject(DocumentBuilder object) throws Exception {
        // no-op
    }

    @Override
    public DocumentBuilder makeObject() throws ParserConfigurationException {
        m_lock.lock();
        DocumentBuilder result =  null;
        try {
            result = m_factory.newDocumentBuilder();
        } finally {
            m_lock.unlock();
        }
        return result;
    }

    @Override
    public void passivateObject(DocumentBuilder object) throws Exception {
        // no-op
    }

    @Override
    public boolean validateObject(DocumentBuilder object) {
        return true;
    }

}
