package org.fcrepo.utilities.xml;

import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.pool.PoolableObjectFactory;
import org.xml.sax.SAXException;


public class PoolableSAXParserFactory
implements PoolableObjectFactory<SAXParser> {

    private final SAXParserFactory m_factory;
    
    private final ReentrantLock m_lock = new ReentrantLock();
    
    public PoolableSAXParserFactory(boolean namespaceAware, boolean validating) {
        m_factory =
                SAXParserFactory.newInstance();
        m_factory.setNamespaceAware(namespaceAware);
        m_factory.setValidating(validating);
    }
    
    @Override
    public void activateObject(SAXParser object) throws Exception {
        object.reset();
    }
    
    @Override
    public void destroyObject(SAXParser object) throws Exception {
        // no-op
    }

    @Override
    public SAXParser makeObject() throws SAXException, ParserConfigurationException {
        m_lock.lock();
        SAXParser result =null;
        try {
            result = m_factory.newSAXParser();
        } finally {
            m_lock.unlock();
        }
        return result;
    }

    @Override
    public void passivateObject(SAXParser object) throws Exception {
        // no-op
    }

    @Override
    public boolean validateObject(SAXParser object) {
        return true;
    }

}
