package org.fcrepo.server.rest.cxf;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;


public class WadlGenerator extends
        org.apache.cxf.jaxrs.model.wadl.WadlGenerator {

    public WadlGenerator() {
        super();
    }

    public WadlGenerator(org.apache.cxf.jaxrs.model.wadl.WadlGenerator other) {
        super(other);
    }

    @Override
    public Response handleRequest(Message message, ClassResourceInfo resource) {
        if (getPath(message).endsWith("/application.wadl")) {
            String query = (String) message.get(Message.QUERY_STRING);
            if (query == null) {
                message.put(Message.QUERY_STRING, "_wadl=&_type=xml");
            } else {
                if (query.indexOf("_wadl=") < 0) message.put(Message.QUERY_STRING, query + "&_wadl=&_type=xml");
            }
        }
        return super.handleRequest(message, resource);
    }

    private static String getPath(Message message) {
        String address = HttpUtils.getEndpointAddress(message);
        if (MessageUtils.isRequestor(message)) {
            return address;
        }
        String path = HttpUtils.getPathToMatch(message, false);
        return address + path;
    }

}
